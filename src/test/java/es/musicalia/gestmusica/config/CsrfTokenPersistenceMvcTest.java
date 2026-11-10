package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The CSRF token must outlive the HTTP session, like the remember-me cookie does: a form loaded before the
 * session expired (timeout, restart, deploy) has to keep working once remember-me re-authenticates the user.
 */
@WebMvcTest(controllers = TestCsrfProbeController.class)
@Import(WebSecurityConfig.class)
class CsrfTokenPersistenceMvcTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;
    @MockitoBean
    private CustomPermissionEvaluator customPermissionEvaluator;
    @MockitoBean
    private MensajeService mensajeService;
    @MockitoBean
    private FunctionalEventTracker functionalEventTracker;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());

        when(userDetailsService.loadUserByUsername(any())).thenReturn(
            User.withUsername("test").password("{noop}password").authorities("USER").build()
        );
    }

    @Test
    void formTokenIsAcceptedAfterTheSessionIsLost() throws Exception {
        MvcResult formPage = mockMvc.perform(get("/test-csrf/token").with(user("auth").roles("USER")))
            .andExpect(status().isOk())
            .andReturn();
        String formToken = formPage.getResponse().getContentAsString();
        String cookieValue = csrfCookieValue(formPage);

        // New request without the original session: only the browser cookies and the form token remain.
        mockMvc.perform(post("/test-csrf/save")
                .with(user("auth").roles("USER"))
                .cookie(new Cookie(CSRF_COOKIE, cookieValue))
                .param("_csrf", formToken))
            .andExpect(status().isOk())
            .andExpect(content().string("saved"));
    }

    @Test
    void csrfCookieLastsAsLongAsRememberMeAndStaysHiddenFromScripts() throws Exception {
        MvcResult formPage = mockMvc.perform(get("/test-csrf/token").with(user("auth").roles("USER")))
            .andExpect(status().isOk())
            .andReturn();

        Cookie cookie = csrfCookie(formPage);
        assertThat(cookie.getMaxAge()).isEqualTo(WebSecurityConfig.REMEMBER_ME_VALIDITY.toSeconds());
        assertThat(cookie.isHttpOnly()).isTrue();
        // Servlet cookie attribute, written by the container in the Set-Cookie header.
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void postWithoutCsrfTokenIsStillRejected() throws Exception {
        mockMvc.perform(post("/test-csrf/save").with(user("auth").roles("USER")))
            .andExpect(status().isForbidden())
            .andExpect(forwardedUrl("/403"));
    }

    @Test
    void postWithForeignCsrfTokenIsStillRejected() throws Exception {
        mockMvc.perform(post("/test-csrf/save")
                .with(user("auth").roles("USER"))
                .cookie(new Cookie(CSRF_COOKIE, "attacker-chosen"))
                .param("_csrf", "attacker-chosen"))
            .andExpect(status().isForbidden())
            .andExpect(forwardedUrl("/403"));
    }

    private static Cookie csrfCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(CSRF_COOKIE);
        if (cookie == null) {
            throw new AssertionError("No " + CSRF_COOKIE + " cookie issued; headers: "
                + result.getResponse().getHeaders(HttpHeaders.SET_COOKIE));
        }
        return cookie;
    }

    private static String csrfCookieValue(MvcResult result) {
        return csrfCookie(result).getValue();
    }
}
