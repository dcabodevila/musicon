package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.auth.model.AuthController;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.CodigoVerificacionService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(WebSecurityConfig.class)
class PasskeyLoginTest {

    /** Same shape the browser sends after navigator.credentials.get(). */
    private static final String ASSERTION = """
            {"id":"AQID","rawId":"AQID",
             "response":{"authenticatorData":"AQID","clientDataJSON":"AQID","signature":"AQID","userHandle":"BAUG"},
             "credType":"public-key","clientExtensionResults":{},"authenticatorAttachment":"platform"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SessionRegistry sessionRegistry;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private CodigoVerificacionService codigoVerificacionService;
    @MockitoBean
    private LocalizacionService localizacionService;
    @MockitoBean
    private SecurityService securityService;
    @MockitoBean
    private FunctionalEventTracker functionalEventTracker;
    @MockitoBean
    private MensajeService mensajeService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;
    @MockitoBean
    private CustomPermissionEvaluator customPermissionEvaluator;
    @MockitoBean
    private PublicKeyCredentialUserEntityRepository userEntityRepository;
    @MockitoBean
    private UserCredentialRepository userCredentialRepository;
    @MockitoBean
    private WebAuthnRelyingPartyOperations rpOperations;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());

        // The session registry is a singleton shared by every test of this cached context.
        sessionRegistry.getAllPrincipals().forEach(principal -> sessionRegistry.getAllSessions(principal, true)
                .forEach(info -> sessionRegistry.removeSessionInformation(info.getSessionId())));

        when(rpOperations.createCredentialRequestOptions(any())).thenAnswer(invocation ->
                PublicKeyCredentialRequestOptions.builder().challenge(Bytes.random()).rpId("localhost").build());
        when(userDetailsService.loadUserByUsername("jperez")).thenAnswer(invocation -> usuarioAutenticado());
    }

    @Test
    void loginConPasskey_debeAutenticarConElUsuarioDeLaAplicacion() throws Exception {
        passkeyValida();
        MockHttpSession session = sesionConReto();

        mockMvc.perform(loginConPasskey(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.redirectUrl").value("/"))
                .andExpect(authenticated().withAuthentication(authentication -> {
                    assertThat(authentication.getPrincipal()).isInstanceOf(CustomAuthenticatedUser.class);
                    assertThat(((CustomAuthenticatedUser) authentication.getPrincipal()).getUserId()).isEqualTo(7L);
                    assertThat(authentication.getName()).isEqualTo("jperez");
                }));
    }

    @Test
    void loginConPasskey_debeCambiarLaSesionYRegistrarla() throws Exception {
        passkeyValida();
        MockHttpSession session = sesionConReto();
        String sesionAnterior = session.getId();

        MvcResult result = mockMvc.perform(loginConPasskey(session)).andReturn();

        String sesionNueva = result.getRequest().getSession(false).getId();
        assertThat(sesionNueva).isNotEqualTo(sesionAnterior);
        assertThat(sesionesActivasDe("jperez")).extracting(SessionInformation::getSessionId).containsExactly(sesionNueva);
    }

    @Test
    void segundoLoginConPasskey_debeCaducarLaSesionAnterior() throws Exception {
        passkeyValida();
        MvcResult primero = mockMvc.perform(loginConPasskey(sesionConReto())).andReturn();
        String primeraSesion = primero.getRequest().getSession(false).getId();

        mockMvc.perform(loginConPasskey(sesionConReto())).andExpect(status().isOk());

        assertThat(sessionRegistry.getSessionInformation(primeraSesion).isExpired()).isTrue();
    }

    @Test
    void loginConPasskey_debeRegistrarElEventoFuncional() throws Exception {
        passkeyValida();

        mockMvc.perform(loginConPasskey(sesionConReto())).andExpect(status().isOk());

        verify(functionalEventTracker).track(FunctionalEventNames.AUTH_LOGIN, FunctionalEventOutcome.SUCCESS,
                7L, "jperez", Map.of("source", "passkey"));
    }

    @Test
    void passkeyInvalida_debeResponder401SinAutenticarNiRegistrarSesion() throws Exception {
        when(rpOperations.authenticate(any())).thenThrow(new IllegalArgumentException("invalid signature"));

        mockMvc.perform(loginConPasskey(sesionConReto()))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());

        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(functionalEventTracker).track(eq(FunctionalEventNames.AUTH_LOGIN), eq(FunctionalEventOutcome.FAILURE),
                isNull(), isNull(), eq(Map.of("source", "passkey", "reason", "BadCredentialsException")));
        assertThat(sesionesActivasDe("jperez")).isEmpty();
    }

    @Test
    void loginConPasskey_sinCsrf_debeRechazarse() throws Exception {
        passkeyValida();

        mockMvc.perform(post("/login/webauthn").session(sesionConReto())
                        .contentType(MediaType.APPLICATION_JSON).content(ASSERTION))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());

        verify(functionalEventTracker, never()).track(any(), any(), any(), any(), anyMap());
    }

    private void passkeyValida() {
        when(rpOperations.authenticate(any())).thenReturn(ImmutablePublicKeyCredentialUserEntity.builder()
                .id(Bytes.fromBase64("BAUG")).name("jperez").displayName("Juan Pérez").build());
    }

    private MockHttpSession sesionConReto() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/webauthn/authenticate/options").session(session).with(csrf()))
                .andExpect(status().isOk());
        return session;
    }

    private org.springframework.test.web.servlet.RequestBuilder loginConPasskey(MockHttpSession session) {
        return post("/login/webauthn").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(ASSERTION);
    }

    private List<SessionInformation> sesionesActivasDe(String username) {
        return sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p instanceof CustomAuthenticatedUser u && u.getUsername().equals(username))
                .flatMap(p -> sessionRegistry.getAllSessions(p, false).stream())
                .toList();
    }

    private static CustomAuthenticatedUser usuarioAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("jperez");
        usuario.setNombre("Juan");
        usuario.setApellidos("Pérez");
        usuario.setPassword("{noop}x");
        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(), Map.of(), Map.of());
    }
}
