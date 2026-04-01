package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.ajustes.AjustesService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.permiso.PermisoService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ListadoController.class)
@Import(WebSecurityConfig.class)
class ListadoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocalizacionService localizacionService;
    @MockBean
    private ListadoService listadoService;
    @MockBean
    private ArtistaService artistaService;
    @MockBean
    private AgenciaService agenciaService;
    @MockBean
    private AjustesService ajustesService;
    @MockBean
    private PermisoService permisoService;
    @MockBean
    private ListadoChartDataFactory listadoChartDataFactory;

    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private RateLimitingFilter rateLimitingFilter;
    @MockBean
    private CustomPermissionEvaluator customPermissionEvaluator;
    @MockBean
    private MensajeService mensajeService;

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
    }

    @Test
    void chartData_sinListados_devuelveFallbackSinDatosCero() throws Exception {
        when(listadoService.obtenerListadoEntreFechas(any())).thenReturn(List.of());
        when(listadoService.obtenerListadosPorPeriodo(any(), anyBoolean())).thenReturn(List.of());
        when(listadoChartDataFactory.from(any())).thenReturn(List.of(
                java.util.Map.of("mes", "Sin datos", "cantidad", 0L)
        ));

        mockMvc.perform(post("/listado/audiencias/chart-data")
                        .with(csrf())
                        .with(user("auth").roles("USER"))
                        .param("porDia", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.chartData[0].mes").value("Sin datos"))
                .andExpect(jsonPath("$.chartData[0].cantidad").value(0));
    }
}
