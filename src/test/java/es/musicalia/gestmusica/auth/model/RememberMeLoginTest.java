package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.passkey.PasskeyOffer;
import es.musicalia.gestmusica.usuario.CodigoVerificacionService;
import es.musicalia.gestmusica.usuario.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(WebSecurityConfig.class)
class RememberMeLoginTest {

    private static final int TREINTA_DIAS_EN_SEGUNDOS = 30 * 24 * 60 * 60;

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

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());

        String passwordHash = new BCryptPasswordEncoder().encode("secret");
        // A fresh instance per call, like the real service: Spring Security erases credentials after login.
        when(userDetailsService.loadUserByUsername(eq("jperez"))).thenAnswer(invocation ->
                User.withUsername("jperez")
                        .password(passwordHash)
                        .authorities("USER")
                        .build());
    }

    @Test
    void login_conRecordarme_debeEmitirCookieDeTreintaDias() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", "secret")
                        .param("remember-me", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("jperez"))
                .andExpect(cookie().exists("remember-me"))
                .andExpect(cookie().maxAge("remember-me", TREINTA_DIAS_EN_SEGUNDOS))
                .andExpect(cookie().httpOnly("remember-me", true));
    }

    @Test
    void login_sinRecordarme_noDebeEmitirCookie() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("jperez"))
                .andExpect(cookie().doesNotExist("remember-me"));
    }

    @Test
    void login_conPassword_debeMarcarLaOfertaDePasskey() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", "secret"))
                .andExpect(redirectedUrl("/"))
                .andReturn();

        assertThat(result.getRequest().getSession(false).getAttribute(PasskeyOffer.SESSION_ATTRIBUTE)).isEqualTo(true);
    }

    @Test
    void login_conPasswordIncorrecta_noDebeEmitirCookie() throws Exception {
        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", "mala")
                        .param("remember-me", "on"))
                .andExpect(redirectedUrl("/auth/login?error"))
                .andExpect(unauthenticated())
                .andExpect(result -> {
                    Cookie cookie = result.getResponse().getCookie("remember-me");
                    assertThat(cookie == null || cookie.getMaxAge() == 0)
                            .as("no valid remember-me cookie after a failed login").isTrue();
                });
    }

    @Test
    void cookieRecordarme_sinSesion_debeAutenticarAlUsuario() throws Exception {
        Cookie rememberMe = loginConRecordarme();

        mockMvc.perform(get("/auth/login").cookie(rememberMe))
                .andExpect(authenticated().withUsername("jperez"));
    }

    @Test
    void cookieRecordarme_manipulada_noDebeAutenticar() throws Exception {
        mockMvc.perform(get("/auth/login").cookie(new Cookie("remember-me", "anBlcmV6Ojk5OTk5OTk5OTk5OTk6U0hBMjU2OmZhbHNh")))
                .andExpect(unauthenticated());
    }

    @Test
    void logout_debeBorrarLaCookieRecordarme() throws Exception {
        Cookie rememberMe = loginConRecordarme();

        mockMvc.perform(post("/logout").with(csrf()).cookie(rememberMe))
                .andExpect(cookie().maxAge("remember-me", 0));
    }

    @Test
    void paginaLogin_debeOfrecerRecordarmeYCamposParaElGestorDeContrasenas() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"remember-me\"")))
                .andExpect(content().string(containsString("autocomplete=\"username")))
                .andExpect(content().string(containsString("autocomplete=\"current-password\"")));
    }

    private Cookie loginConRecordarme() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "jperez")
                        .param("password", "secret")
                        .param("remember-me", "on"))
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("remember-me");
        assertThat(cookie).isNotNull();
        return new Cookie("remember-me", cookie.getValue());
    }
}
