package es.musicalia.gestmusica.sincronizacion;

import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.ThymeleafConfig;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SincronizacionController.class)
@Import({WebSecurityConfig.class, ThymeleafConfig.class})
@TestPropertySource(properties = "mariadb.datasource.enabled=true")
class SincronizacionControllerMvcTest {

    private static final LocalDate INICIO_2025 = LocalDate.of(2025, 1, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SincronizacionService sincronizacionService;
    @MockitoBean private SessionRegistry sessionRegistry;
    @MockitoBean private UserService userService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private RateLimitingFilter rateLimitingFilter;
    @MockitoBean private CustomPermissionEvaluator customPermissionEvaluator;
    @MockitoBean private MensajeService mensajeService;
    @MockitoBean private FunctionalEventTracker functionalEventTracker;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
        when(sessionRegistry.getAllPrincipals()).thenReturn(Collections.emptyList());
    }

    @Test
    void sincronizarSyncsFromTodayWithLastHourModificationsAndReturnsCounters() throws Exception {
        when(sincronizacionService.sincronizarOcupacionesDesde(any(), any())).thenReturn(result(3, 2, 1));
        LocalDate diaAntes = LocalDate.now();
        LocalDateTime antes = LocalDateTime.now();

        mockMvc.perform(post("/api/sincronizacion/sincronizar").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupacionesCreadas").value(3))
                .andExpect(jsonPath("$.ocupacionesActualizadas").value(2))
                .andExpect(jsonPath("$.ocupacionesEliminadas").value(1))
                .andExpect(jsonPath("$.ocupacionesErrores").isEmpty())
                .andExpect(jsonPath("$.mensaje").value("Sincronización completada"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error").doesNotExist());

        LocalDateTime despues = LocalDateTime.now();
        ArgumentCaptor<LocalDate> fecha = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDateTime> modificadoDesde = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sincronizacionService).sincronizarOcupacionesDesde(fecha.capture(), modificadoDesde.capture());
        assertThat(fecha.getValue()).isBetween(diaAntes, despues.toLocalDate());
        assertThat(modificadoDesde.getValue()).isBetween(antes.minusHours(1), despues.minusHours(1));
    }

    @Test
    void sincronizarAllSyncsEverythingSince2025() throws Exception {
        when(sincronizacionService.sincronizarOcupacionesDesde(INICIO_2025, INICIO_2025.atStartOfDay())).thenReturn(result(10, 0, 4));

        mockMvc.perform(post("/api/sincronizacion/sincronizar-all").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupacionesCreadas").value(10))
                .andExpect(jsonPath("$.ocupacionesActualizadas").value(0))
                .andExpect(jsonPath("$.ocupacionesEliminadas").value(4))
                .andExpect(jsonPath("$.mensaje").value("Sincronización completada"));

        verify(sincronizacionService).sincronizarOcupacionesDesde(INICIO_2025, INICIO_2025.atStartOfDay());
    }

    @Test
    void sincronizarFechaSyncsFromGivenDateAtStartOfDay() throws Exception {
        LocalDate fechaDesde = LocalDate.of(2025, 6, 15);
        when(sincronizacionService.sincronizarOcupacionesDesde(fechaDesde, fechaDesde.atStartOfDay())).thenReturn(result(1, 1, 0));

        mockMvc.perform(post("/api/sincronizacion/sincronizar-fecha").param("fechaDesde", "2025-06-15").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupacionesCreadas").value(1))
                .andExpect(jsonPath("$.ocupacionesActualizadas").value(1))
                .andExpect(jsonPath("$.mensaje").value("Sincronización completada"));

        verify(sincronizacionService).sincronizarOcupacionesDesde(fechaDesde, fechaDesde.atStartOfDay());
    }

    @Test
    void sincronizarFechaWithoutDateIsRejectedWithoutSyncing() throws Exception {
        mockMvc.perform(post("/api/sincronizacion/sincronizar-fecha").with(user("admin")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sincronizacionService);
    }

    @Test
    void sincronizarArtistaSyncsThatArtistaSince2025() throws Exception {
        when(sincronizacionService.sincronizarOcupacionesArtista(INICIO_2025, INICIO_2025.atStartOfDay(), 42)).thenReturn(result(2, 3, 0));

        mockMvc.perform(post("/api/sincronizacion/sincronizar/42").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupacionesCreadas").value(2))
                .andExpect(jsonPath("$.ocupacionesActualizadas").value(3))
                .andExpect(jsonPath("$.mensaje").value("Sincronización completada"));

        verify(sincronizacionService).sincronizarOcupacionesArtista(INICIO_2025, INICIO_2025.atStartOfDay(), 42);
    }

    @Test
    void generalErrorInsideResultStillAnswers200WithoutErrorField() throws Exception {
        SincronizacionResult conError = result(0, 0, 0);
        conError.setErrorGeneral("MariaDB caída");
        when(sincronizacionService.sincronizarOcupacionesDesde(any(), any())).thenReturn(conError);

        mockMvc.perform(post("/api/sincronizacion/sincronizar").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sincronización completada"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void serviceExceptionAnswers500WithErrorMessage() throws Exception {
        when(sincronizacionService.sincronizarOcupacionesArtista(any(), any(), anyInt())).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/api/sincronizacion/sincronizar/7").with(user("admin")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error en la sincronización: boom"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.mensaje").doesNotExist());
    }

    @Test
    void anonymousRequestIsRedirectedToLoginWithoutSyncing() throws Exception {
        mockMvc.perform(post("/api/sincronizacion/sincronizar-all"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        verifyNoInteractions(sincronizacionService);
    }

    @Test
    void getIsNotAllowedOnSyncEndpoints() throws Exception {
        mockMvc.perform(get("/api/sincronizacion/sincronizar").with(user("admin")))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(sincronizacionService);
    }

    private static SincronizacionResult result(int creadas, int actualizadas, int eliminadas) {
        SincronizacionResult result = new SincronizacionResult();
        result.setCreadas(creadas);
        result.setActualizadas(actualizadas);
        result.setEliminadas(eliminadas);
        return result;
    }
}
