package es.musicalia.gestmusica.agencia.publicacioneventos;

import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AgenciaPublicacionEventosController.class)
@Import(WebSecurityConfig.class)
class AgenciaPublicacionEventosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgenciaPublicacionEventosService agenciaPublicacionEventosService;
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
    @MockBean
    private FunctionalEventTracker functionalEventTracker;

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

        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario));
    }

    @Test
    void check_devuelveShouldShow() throws Exception {
        when(agenciaPublicacionEventosService.debeMostrarModal(7L)).thenReturn(true);
        when(agenciaPublicacionEventosService.findAgenciasPendientesModal(7L))
                .thenReturn(List.of(new AgenciaPublicacionEventosModalItem(3L, "Agencia Uno")));

        mockMvc.perform(get("/api/agencia/publicacion-eventos/check").with(user("auth").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shouldShow").value(true))
                .andExpect(jsonPath("$.agencias[0].idAgencia").value(3L))
                .andExpect(jsonPath("$.agencias[0].nombreAgencia").value("Agencia Uno"));
    }

    @Test
    void activar_publicaYMarcaDecision() throws Exception {
        mockMvc.perform(post("/api/agencia/publicacion-eventos/activar").with(user("auth").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(agenciaPublicacionEventosService).activarPublicacionEventos(7L);
    }

    @Test
    void desactivar_despublicaYMarcaDecision() throws Exception {
        mockMvc.perform(post("/api/agencia/publicacion-eventos/desactivar").with(user("auth").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(agenciaPublicacionEventosService).desactivarPublicacionEventos(7L);
    }
}
