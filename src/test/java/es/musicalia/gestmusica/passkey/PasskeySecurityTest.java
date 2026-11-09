package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.auth.model.AuthController;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.CodigoVerificacionService;
import es.musicalia.gestmusica.usuario.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(WebSecurityConfig.class)
class PasskeySecurityTest {

    @Autowired
    private MockMvc mockMvc;

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

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
        when(userCredentialRepository.findByUserId(any())).thenReturn(List.of());
    }

    @Test
    void opcionesDeLogin_anonimo_debenDevolverRetoParaElDominioConfigurado() throws Exception {
        mockMvc.perform(post("/webauthn/authenticate/options").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").isNotEmpty())
                .andExpect(jsonPath("$.rpId").value("localhost"));
    }

    @Test
    void opcionesDeLogin_sinCsrf_debenRechazarse() throws Exception {
        mockMvc.perform(post("/webauthn/authenticate/options"))
                .andExpect(status().isForbidden());
    }

    @Test
    void opcionesDeRegistro_anonimo_debenRechazarseSinTocarLasPasskeys() throws Exception {
        // Spring Security answers 400 (not a login redirect) when the registration filter is not authorized.
        mockMvc.perform(post("/webauthn/register/options").with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userEntityRepository, userCredentialRepository);
    }

    @Test
    void opcionesDeRegistro_autenticado_debenUsarElUsernameDeLogin() throws Exception {
        mockMvc.perform(post("/webauthn/register/options").with(csrf()).with(user("jperez")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").isNotEmpty())
                .andExpect(jsonPath("$.rp.id").value("localhost"))
                .andExpect(jsonPath("$.user.name").value("jperez"));
    }

    @Test
    void paginaLogin_debeOfrecerPasskeyYAutorrelleno() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("autocomplete=\"username webauthn\"")))
                .andExpect(content().string(containsString("id=\"passkey-login\"")))
                .andExpect(content().string(containsString("Entrar con huella o Face ID")))
                .andExpect(content().string(matchesPattern("(?s).*src=\"/js/passkeys(-[0-9a-f]+)?[.]js\" data-context-path=\"/\".*")))
                .andExpect(content().string(matchesPattern("(?s).*src=\"/js/login-passkey(-[0-9a-f]+)?[.]js\".*")));
    }

    @Test
    void paginaDeRegistroPorDefecto_debeEstarDesactivada() throws Exception {
        mockMvc.perform(get("/webauthn/register").with(user("jperez")).accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());
    }
}
