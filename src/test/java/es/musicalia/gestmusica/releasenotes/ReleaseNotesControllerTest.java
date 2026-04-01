package es.musicalia.gestmusica.releasenotes;

import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReleaseNotesController.class)
@Import(WebSecurityConfig.class)
class ReleaseNotesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReleaseNotesService releaseNotesService;
    @MockBean
    private UserService userService;

    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private RateLimitingFilter rateLimitingFilter;
    @MockBean
    private CustomPermissionEvaluator customPermissionEvaluator;
    @MockBean
    private MensajeService mensajeService;

    private Usuario usuario;

    @BeforeEach
    void setUp() throws Exception {
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
        when(mensajeService.obtenerMensajesRecibidos(any())).thenReturn(Collections.emptyList());

        usuario = new Usuario();
        usuario.setId(7L);
        when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario));
    }

    @Test
    void check_noMuestraCuandoVersionActualEsInvalida_failClosed() throws Exception {
        when(releaseNotesService.getCurrentEffectiveVersion()).thenReturn(Optional.empty());
        when(releaseNotesService.getCurrentVersion()).thenReturn("v1.2.3");

        mockMvc.perform(get("/release-notes/api/check").with(user("auth").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldShow").value(false));
    }

    @Test
    void check_121ConLeido120_noDebeMostrarModal() throws Exception {
        when(releaseNotesService.getCurrentEffectiveVersion()).thenReturn(Optional.of("1.2.0"));
        when(releaseNotesService.hasReadReleaseNotes(7L, "1.2.0")).thenReturn(true);

        mockMvc.perform(get("/release-notes/api/check").with(user("auth").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldShow").value(false));
    }

    @Test
    void check_121ConLeidoHistorico121_noDebeMostrarModal() throws Exception {
        when(releaseNotesService.getCurrentEffectiveVersion()).thenReturn(Optional.of("1.2.0"));
        when(releaseNotesService.hasReadReleaseNotes(7L, "1.2.0")).thenReturn(true);

        mockMvc.perform(get("/release-notes/api/check").with(user("auth").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldShow").value(false));
    }

    @Test
    void check_130ConContenidoAplicable_siDebeMostrarModal() throws Exception {
        when(releaseNotesService.getCurrentEffectiveVersion()).thenReturn(Optional.of("1.3.0"));
        when(releaseNotesService.hasReadReleaseNotes(7L, "1.3.0")).thenReturn(false);
        when(releaseNotesService.shouldShowReleaseNotes(usuario, "1.3.0")).thenReturn(true);
        when(releaseNotesService.hasApplicableContent(usuario, "1.3.0")).thenReturn(true);
        when(releaseNotesService.getReleaseNotesContent("1.3.0", usuario, true)).thenReturn("<p>contenido</p>");

        mockMvc.perform(get("/release-notes/api/check").with(user("auth").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldShow").value(true))
                .andExpect(jsonPath("$.version").value("1.3.0"))
                .andExpect(jsonPath("$.content").value("<p>contenido</p>"));
    }

    @Test
    void check_noDebeMostrarModal_siContenidoNoDisponible() throws Exception {
        when(releaseNotesService.getCurrentEffectiveVersion()).thenReturn(Optional.of("1.3.0"));
        when(releaseNotesService.hasReadReleaseNotes(7L, "1.3.0")).thenReturn(false);
        when(releaseNotesService.shouldShowReleaseNotes(usuario, "1.3.0")).thenReturn(true);
        when(releaseNotesService.hasApplicableContent(usuario, "1.3.0")).thenReturn(true);
        when(releaseNotesService.getReleaseNotesContent("1.3.0", usuario, true))
                .thenReturn("<p>No hay release notes disponibles para esta versión.</p>");

        mockMvc.perform(get("/release-notes/api/check").with(user("auth").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldShow").value(false));
    }

    @Test
    void markRead_debeMantenerEndpointYResponderVersionEfectiva() throws Exception {
        when(releaseNotesService.getEffectiveVersion("1.2.9")).thenReturn(Optional.of("1.2.0"));

        mockMvc.perform(post("/release-notes/api/mark-read")
                        .with(csrf())
                        .with(user("auth").roles("USER"))
                        .param("version", "1.2.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.version").value("1.2.0"));

        verify(releaseNotesService).markAsRead(7L, "1.2.9");
    }
}
