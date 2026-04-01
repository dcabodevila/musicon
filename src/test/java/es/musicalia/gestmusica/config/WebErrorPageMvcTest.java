package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.home.HomeController;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = {
    HomeController.class,
    TestErrorThrowingController.class
})
@Import({WebSecurityConfig.class, ErrorPageViewResolver.class})
@ImportAutoConfiguration(ErrorMvcAutoConfiguration.class)
public class WebErrorPageMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OcupacionService ocupacionService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private CustomPermissionEvaluator customPermissionEvaluator;

    @MockBean
    private MensajeService mensajeService;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());

        when(userDetailsService.loadUserByUsername(any())).thenReturn(
            User.withUsername("test")
                .password("{noop}password")
                .authorities("USER")
                .build()
        );
    }

    @Test
    void shouldRender500TemplateForHtmlRequests() throws Exception {
        mockMvc.perform(get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/test-errors/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(view().name("error/500"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Ups... tuvimos un problema inesperado")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))));
    }

    @Test
    void shouldRender404TemplateWithPublicCtaForEventosPaths() throws Exception {
        mockMvc.perform(get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/eventos/inexistente"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error/404"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/eventos\"")));
    }

    @Test
    void shouldRender403WithoutRedirectForAnonymousAndAuthenticated() throws Exception {
        mockMvc.perform(get("/403").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(view().name("/error/403"));

        mockMvc.perform(get("/403")
                .with(user("auth").roles("USER"))
                .accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(view().name("/error/403"));
    }

    @Test
    void shouldRender500TemplateWhenControllerThrowsUncheckedException() throws Exception {
        ServletException servletException = assertThrows(ServletException.class, () ->
            mockMvc.perform(get("/eventos/test-errors/runtime-boom")
                    .accept(MediaType.TEXT_HTML))
                .andReturn()
        );

        mockMvc.perform(get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/eventos/test-errors/runtime-boom")
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, servletException.getCause()))
            .andExpect(status().isInternalServerError())
            .andExpect(view().name("error/500"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Ups... tuvimos un problema inesperado")));
    }

    @Test
    void shouldUseSecurityAccessDeniedFlowToBranded403() throws Exception {
        mockMvc.perform(get("/user")
                .with(user("auth").roles("USER"))
                .accept(MediaType.TEXT_HTML))
            .andExpect(status().isForbidden())
            .andExpect(forwardedUrl("/403"));
    }

    @Test
    void shouldRenderFallbackTemplateForUnknownStatus() throws Exception {
        mockMvc.perform(get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 418)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/test-errors/teapot"))
            .andExpect(status().isIAmATeapot())
            .andExpect(view().name("error/error"));
    }

    @Test
    void shouldKeepJsonErrorPayloadForApiClients() throws Exception {
        mockMvc.perform(get("/error")
                .accept(MediaType.APPLICATION_JSON)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/eventos/api/artista/1"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.path").value("/eventos/api/artista/1"));
    }

    @Test
    void shouldAllowAnonymousAccessToErrorEndpointInsteadOfLoginRedirect() throws Exception {
        mockMvc.perform(get("/error")
                .accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/privado/no-existe"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error/404"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/\"")));
    }

}
