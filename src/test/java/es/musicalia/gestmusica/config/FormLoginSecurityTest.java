package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.AuthController;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.SecurityService;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Characterization of the form login, logout and session rules of {@link WebSecurityConfig}.
 * Remember-me and passkey login live in RememberMeLoginTest and PasskeyLoginTest.
 */
@WebMvcTest(AuthController.class)
@Import(WebSecurityConfig.class)
class FormLoginSecurityTest {

    private static final String PASSWORD = "secret";
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);

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

        // A fresh instance per call, like the real service: Spring Security erases credentials after login.
        when(userDetailsService.loadUserByUsername("jperez"))
                .thenAnswer(invocation -> usuario(true, true));
    }

    // ==================== Protected pages ====================

    @Test
    void paginaProtegida_sinSesion_debeRedirigirAlLogin() throws Exception {
        mockMvc.perform(get("/listado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"))
                .andExpect(unauthenticated());
    }

    @Test
    void paginaLogin_anonimo_debeMostrarseSinAutenticar() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(unauthenticated());
    }

    // ==================== Successful login ====================

    @Test
    void login_correcto_debeAutenticarCambiarLaSesionYRedirigirALaRaiz() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String sesionAnterior = session.getId();

        MvcResult result = mockMvc.perform(post("/auth/login").session(session).with(csrf())
                        .param("username", "jperez")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withAuthentication(authentication -> {
                    assertThat(authentication.getPrincipal()).isInstanceOf(CustomAuthenticatedUser.class);
                    assertThat(((CustomAuthenticatedUser) authentication.getPrincipal()).getUserId()).isEqualTo(7L);
                    assertThat(authentication.getName()).isEqualTo("jperez");
                }))
                .andReturn();

        String sesionNueva = result.getRequest().getSession(false).getId();
        assertThat(sesionNueva).as("session fixation protection").isNotEqualTo(sesionAnterior);
        assertThat(sesionesActivasDe("jperez")).extracting(SessionInformation::getSessionId)
                .containsExactly(sesionNueva);
    }

    @Test
    void login_correcto_trasPedirUnaPaginaProtegida_debeIrSiempreALaRaiz() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/listado").session(session))
                .andExpect(redirectedUrl("http://localhost/auth/login"));

        mockMvc.perform(post("/auth/login").session(session).with(csrf())
                        .param("username", "jperez")
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("jperez"));
    }

    @Test
    void login_correcto_debeRegistrarElEventoFuncional() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/"));

        verify(functionalEventTracker).track(FunctionalEventNames.AUTH_LOGIN, FunctionalEventOutcome.SUCCESS,
                7L, "jperez", Map.of("source", "form_login"));
    }

    // ==================== Failed login ====================

    @Test
    void login_passwordIncorrecta_debeVolverAlLoginConErrorYRegistrarElFallo() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", "mala"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?error"))
                .andExpect(unauthenticated());

        verify(functionalEventTracker).track(FunctionalEventNames.AUTH_LOGIN, FunctionalEventOutcome.FAILURE,
                null, "jperez", Map.of("source", "form_login", "reason", "BadCredentialsException"));
        assertThat(sessionRegistry.getAllPrincipals()).isEmpty();
    }

    @Test
    void login_usuarioInexistenteOInactivo_debeDarElMismoErrorQueUnaPasswordIncorrecta() throws Exception {
        // The repository only returns active users, so an inactive user is "not found" as well.
        when(userDetailsService.loadUserByUsername("baja")).thenThrow(new UsernameNotFoundException("baja"));

        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "baja")
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/auth/login?error"))
                .andExpect(unauthenticated());

        verify(functionalEventTracker).track(FunctionalEventNames.AUTH_LOGIN, FunctionalEventOutcome.FAILURE,
                null, "baja", Map.of("source", "form_login", "reason", "BadCredentialsException"));
    }

    @Test
    void login_usuarioDeshabilitado_debeVolverAlLoginConError() throws Exception {
        when(userDetailsService.loadUserByUsername("jperez")).thenAnswer(invocation -> usuario(false, true));

        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/auth/login?error"))
                .andExpect(unauthenticated());

        verify(functionalEventTracker).track(FunctionalEventNames.AUTH_LOGIN, FunctionalEventOutcome.FAILURE,
                null, "jperez", Map.of("source", "form_login", "reason", "DisabledException"));
    }

    @Test
    void login_usuarioBloqueado_debeVolverAlLoginConError() throws Exception {
        when(userDetailsService.loadUserByUsername("jperez")).thenAnswer(invocation -> usuario(true, false));

        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/auth/login?error"))
                .andExpect(unauthenticated());

        verify(functionalEventTracker).track(FunctionalEventNames.AUTH_LOGIN, FunctionalEventOutcome.FAILURE,
                null, "jperez", Map.of("source", "form_login", "reason", "LockedException"));
    }

    @Test
    void login_sinCsrf_debeRechazarseSinIntentarAutenticar() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .param("username", "jperez")
                        .param("password", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());

        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(functionalEventTracker, never()).track(any(), any(), any(), any(), anyMap());
    }

    // ==================== Concurrent sessions ====================

    @Test
    void segundoLogin_debeCaducarLaSesionAnteriorYEnviarlaAlLoginConExpired() throws Exception {
        MockHttpSession primeraSesion = (MockHttpSession) loginCorrecto().getRequest().getSession(false);

        loginCorrecto();

        assertThat(sessionRegistry.getSessionInformation(primeraSesion.getId()).isExpired()).isTrue();
        mockMvc.perform(get("/listado").session(primeraSesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?expired"))
                .andExpect(unauthenticated());
    }

    // ==================== Logout ====================

    @Test
    void logout_debeInvalidarLaSesionBorrarCookiesRegistrarElEventoYVolverAlLogin() throws Exception {
        MockHttpSession session = (MockHttpSession) loginCorrecto().getRequest().getSession(false);

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?logout"))
                .andExpect(cookie().maxAge("JSESSIONID", 0))
                .andExpect(cookie().maxAge("remember-me", 0))
                .andExpect(unauthenticated());

        assertThat(session.isInvalid()).isTrue();
        verify(functionalEventTracker).track(FunctionalEventNames.AUTH_LOGOUT, FunctionalEventOutcome.SUCCESS,
                7L, "jperez", Map.of("source", "logout"));
    }

    @Test
    void logout_sinCsrf_debeRechazarseYMantenerLaSesion() throws Exception {
        MockHttpSession session = (MockHttpSession) loginCorrecto().getRequest().getSession(false);

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().isForbidden());

        assertThat(session.isInvalid()).isFalse();
        mockMvc.perform(get("/auth/login").session(session))
                .andExpect(authenticated().withUsername("jperez"));
        verify(functionalEventTracker, never()).track(
                eq(FunctionalEventNames.AUTH_LOGOUT), any(), any(), any(), anyMap());
    }

    // ==================== Public SEO routes ====================

    @Test
    void robotsTxt_anonimo_debeServirseSinSesion() throws Exception {
        MvcResult result = mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).as("stateless public chain").isNull();
    }

    @Test
    void eventosPublicos_anonimo_noDebenPedirLoginNiCrearSesion() throws Exception {
        // No controller is loaded in this slice: 404 means security let the request through.
        MvcResult result = mockMvc.perform(get("/eventos/verbena-de-prueba"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).as("stateless public chain").isNull();
    }

    @Test
    void bajaPublica_postSinCsrf_noDebeRechazarse() throws Exception {
        MvcResult result = mockMvc.perform(post("/baja/token-de-prueba"))
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).as("stateless public chain").isNull();
    }

    @Test
    void paginaPrivada_postSinCsrf_debeRechazarse() throws Exception {
        MockHttpSession session = (MockHttpSession) loginCorrecto().getRequest().getSession(false);

        mockMvc.perform(post("/listado").session(session))
                .andExpect(status().isForbidden());
    }

    private MvcResult loginCorrecto() throws Exception {
        return mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andReturn();
    }

    private List<SessionInformation> sesionesActivasDe(String username) {
        return sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p instanceof CustomAuthenticatedUser u && u.getUsername().equals(username))
                .flatMap(p -> sessionRegistry.getAllSessions(p, false).stream())
                .toList();
    }

    private static CustomAuthenticatedUser usuario(boolean enabled, boolean accountNonLocked) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("jperez");
        usuario.setNombre("Juan");
        usuario.setApellidos("Pérez");
        usuario.setPassword(PASSWORD_HASH);
        return new CustomAuthenticatedUser(usuario, enabled, true, true, accountNonLocked, List.of(), Map.of(), Map.of());
    }
}
