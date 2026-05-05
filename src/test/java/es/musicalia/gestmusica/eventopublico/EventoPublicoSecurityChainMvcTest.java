package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventoPublicoController.class)
@Import({DefaultEventoPublicStructuredDataBuilder.class, WebSecurityConfig.class})
class EventoPublicoSecurityChainMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventoPublicoService eventoPublicoService;

    @MockBean
    private LocalizacionService localizacionService;

    @MockBean
    private MensajeService mensajeService;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private FunctionalEventTracker functionalEventTracker;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CustomPermissionEvaluator customPermissionEvaluator;

    @Test
    void detallePublico_debeResponder200ConSecurityChainRealSinSesionNiCsrf() throws Exception {
        EventoPublicoDto evento = EventoPublicoDto.builder()
            .id(10L)
            .idArtista(20L)
            .nombreArtista("Los Satélites")
            .municipio("Lugo")
            .provincia("Lugo")
            .lugar("Praza Maior")
            .fecha(LocalDateTime.of(2026, 8, 15, 0, 0))
            .fechaActualizacion(LocalDateTime.of(2026, 8, 1, 12, 0))
            .build();

        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));
        when(eventoPublicoService.obtenerEventosPublicosPorArtista(20L)).thenReturn(List.of(evento));

        mockMvc.perform(get("/eventos/evento/10-los-satelites-lugo-2026-08-15"))
            .andExpect(status().isOk());
    }
}
