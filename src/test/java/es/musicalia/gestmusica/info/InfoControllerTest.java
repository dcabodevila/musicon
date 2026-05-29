package es.musicalia.gestmusica.info;

import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.ThymeleafConfig;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.listado.ListadoRepository;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InfoController.class)
@Import({WebSecurityConfig.class, ThymeleafConfig.class})
class InfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgenciaRepository agenciaRepository;
    @MockBean
    private ArtistaRepository artistaRepository;
    @MockBean
    private UsuarioRepository usuarioRepository;
    @MockBean
    private ListadoRepository listadoRepository;
    @MockBean
    private OcupacionRepository ocupacionRepository;

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
    void metricasCcaa_debeExponerCampoPresupuestosUltimos100DiasSinLegacy() throws Exception {
        when(usuarioRepository.countUsuariosActivosValidosPorCcaa())
            .thenReturn(List.of(new InfoCcaaMetricRecord("Galicia", 3L, 0L)));
        when(listadoRepository.countPresupuestosActivosPorCcaaDesde(any(LocalDateTime.class)))
            .thenReturn(List.of(new InfoCcaaMetricRecord("Galicia", 0L, 7L)));

        mockMvc.perform(get("/info/metricas-ccaa"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ccaaNombre").value("Galicia"))
            .andExpect(jsonPath("$[0].usuariosActivos").value(3))
            .andExpect(jsonPath("$[0].presupuestosUltimos100Dias").value(7))
            .andExpect(jsonPath("$[0].presupuestosUltimos30Dias").doesNotExist());
    }

    @Test
    void metricasCcaa_debeUsarCorteTemporalDe100Dias() throws Exception {
        when(usuarioRepository.countUsuariosActivosValidosPorCcaa()).thenReturn(List.of());
        when(listadoRepository.countPresupuestosActivosPorCcaaDesde(any(LocalDateTime.class))).thenReturn(List.of());

        LocalDateTime antes = LocalDateTime.now();

        mockMvc.perform(get("/info/metricas-ccaa"))
            .andExpect(status().isOk());

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(listadoRepository).countPresupuestosActivosPorCcaaDesde(captor.capture());
        LocalDateTime esperado = antes.minusDays(100);
        long deltaSegundos = Math.abs(Duration.between(esperado, captor.getValue()).getSeconds());
        org.junit.jupiter.api.Assertions.assertTrue(deltaSegundos <= 2,
            "El corte temporal debe aproximar hoy-100d. Delta(s): " + deltaSegundos);
    }

    @Test
    void info_debeRenderizarCopyDe100DiasYNoElAntiguo() throws Exception {
        when(agenciaRepository.countByActivoTrue()).thenReturn(1L);
        when(artistaRepository.countByActivoTrue()).thenReturn(2L);
        when(usuarioRepository.countByRolGeneralCodigoInAndActivoTrue(any())).thenReturn(3L);
        when(listadoRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(any(LocalDateTime.class))).thenReturn(4L);
        when(ocupacionRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(any(LocalDateTime.class))).thenReturn(5L);

        mockMvc.perform(get("/info"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Presupuestos (últimos 100 días)")))
            .andExpect(content().string(not(containsString("Presupuestos (últimos 30 días)"))))
            .andExpect(content().string(containsString("Ocupaciones en el último mes")));
    }

    @Test
    void info_debeUsarCortesTemporalesCorrectosParaPresupuestosYOcupaciones() throws Exception {
        when(agenciaRepository.countByActivoTrue()).thenReturn(1L);
        when(artistaRepository.countByActivoTrue()).thenReturn(2L);
        when(usuarioRepository.countByRolGeneralCodigoInAndActivoTrue(any())).thenReturn(3L);
        when(listadoRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(any(LocalDateTime.class))).thenReturn(4L);
        when(ocupacionRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(any(LocalDateTime.class))).thenReturn(5L);

        LocalDateTime antes = LocalDateTime.now();

        mockMvc.perform(get("/info"))
            .andExpect(status().isOk());

        ArgumentCaptor<LocalDateTime> cortePresupuestos = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(listadoRepository).countByActivoTrueAndFechaCreacionGreaterThanEqual(cortePresupuestos.capture());
        long deltaPresupuestos = Math.abs(Duration.between(antes.minusDays(100), cortePresupuestos.getValue()).getSeconds());
        org.junit.jupiter.api.Assertions.assertTrue(deltaPresupuestos <= 2,
            "El corte de presupuestos debe aproximar hoy-100d. Delta(s): " + deltaPresupuestos);

        ArgumentCaptor<LocalDateTime> corteOcupaciones = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(ocupacionRepository).countByActivoTrueAndFechaCreacionGreaterThanEqual(corteOcupaciones.capture());
        long deltaOcupaciones = Math.abs(Duration.between(antes.minusDays(30), corteOcupaciones.getValue()).getSeconds());
        org.junit.jupiter.api.Assertions.assertTrue(deltaOcupaciones <= 2,
            "El corte de ocupaciones debe aproximar hoy-30d. Delta(s): " + deltaOcupaciones);
    }

    @Test
    void info_debeExponerDatasetGlobalConsumiblePorMapaCcaa() throws Exception {
        when(agenciaRepository.countByActivoTrue()).thenReturn(1L);
        when(artistaRepository.countByActivoTrue()).thenReturn(2L);
        when(usuarioRepository.countByRolGeneralCodigoInAndActivoTrue(any())).thenReturn(3L);
        when(listadoRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(any(LocalDateTime.class))).thenReturn(1234L);
        when(ocupacionRepository.countByActivoTrueAndFechaCreacionGreaterThanEqual(any(LocalDateTime.class))).thenReturn(5L);

        mockMvc.perform(get("/info"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-total-presupuestos-global=\"1234\"")))
            .andExpect(content().string(not(containsString("presupuestosUltimos30Dias"))));
    }
}
