package es.musicalia.gestmusica.eventopublico;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventoPublicoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DefaultEventoPublicStructuredDataBuilder.class)
class EventoPublicoControllerMvcTest {

    private static final Pattern EVENT_TYPE_PATTERN = Pattern.compile("\\\"@type\\\":\\\"Event\\\"");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventoPublicoService eventoPublicoService;

    @MockBean
    private es.musicalia.gestmusica.localizacion.LocalizacionService localizacionService;

    @MockBean
    private es.musicalia.gestmusica.mensaje.MensajeService mensajeService;

    @Test
    void detallePublico_debeRenderizarJsonLdEventSSRConCanonicalYMetadatos() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));
        when(eventoPublicoService.obtenerEventosPublicosPorArtista(20L)).thenReturn(List.of(evento));

        mockMvc.perform(get("/eventos/evento/10-los-satelites-lugo-2026-08-15"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"@type\":\"Event\"")))
            .andExpect(content().string(not(containsString("\"@type\":\"MusicEvent\""))))
            .andExpect(content().string(containsString("rel=\"canonical\"")))
            .andExpect(content().string(containsString("http://localhost/eventos/evento/10-los-satelites-lugo-2026-08-15")))
            .andExpect(content().string(containsString("meta name=\"robots\" content=\"index,follow\"")))
            .andExpect(content().string(containsString("/adminkit/css/light.css")))
            .andExpect(content().string(not(containsString("_csrf"))));
    }

    @Test
    void detallePublico_debeEmitirUnUnicoObjetoPrincipalEvent() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));
        when(eventoPublicoService.obtenerEventosPublicosPorArtista(20L)).thenReturn(List.of(evento));

        String html = mockMvc.perform(get("/eventos/evento/10-los-satelites-lugo-2026-08-15"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Matcher matcher = EVENT_TYPE_PATTERN.matcher(html);
        int eventCount = 0;
        while (matcher.find()) {
            eventCount++;
        }

        org.junit.jupiter.api.Assertions.assertEquals(1, eventCount,
            "La ficha pública debe renderizar exactamente un Event principal");
    }

    @Test
    void detallePublico_debeSerAccesibleSinSesionNiCsrf() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));
        when(eventoPublicoService.obtenerEventosPublicosPorArtista(20L)).thenReturn(List.of(evento));

        mockMvc.perform(get("/eventos/evento/10-los-satelites-lugo-2026-08-15"))
            .andExpect(status().isOk());
    }

    @Test
    void listadosPublicos_noDebenCambiarMetaRobotsNoIndexFollow() throws Exception {
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(eventoPublicoService.obtenerTodosEventosPublicos()).thenReturn(List.of());
        when(localizacionService.findAllProvincias()).thenReturn(List.of());

        mockMvc.perform(get("/eventos"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("meta name=\"robots\" content=\"noindex,follow\"")));
    }

    @Test
    void provinciaPublica_debeMantenerContratoNoObjetivoSinEventPrincipal() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(evento), PageRequest.of(0, 20), 1));
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(any(), any(), any(), any(), any()))
            .thenReturn(List.of(evento));
        when(localizacionService.findProvinciaByNombreUpperCase(anyString())).thenReturn(Optional.empty());
        when(localizacionService.findAllProvincias()).thenReturn(List.of());
        when(localizacionService.findMunicipiosByProvinciaNombre(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/eventos/provincia/Lugo"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("meta name=\"robots\" content=\"index,follow\"")))
            .andExpect(content().string(not(containsString("\"@type\":\"Event\""))))
            .andExpect(content().string(containsString("\"@type\":\"ItemList\"")));
    }

    @Test
    void municipioPublico_debeMantenerContratoNoObjetivoConNoindexEnPaginacion() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(evento), PageRequest.of(1, 10), 21));
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(any(), any(), any(), any(), any()))
            .thenReturn(List.of(evento));
        when(localizacionService.findProvinciaByNombreUpperCase(anyString())).thenReturn(Optional.empty());
        when(localizacionService.findAllProvincias()).thenReturn(List.of());
        when(localizacionService.findMunicipiosByProvinciaNombre(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/eventos/municipio/Lugo").param("page", "2"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("meta name=\"robots\" content=\"noindex,follow\"")))
            .andExpect(content().string(not(containsString("\"@type\":\"Event\""))))
            .andExpect(content().string(containsString("\"@type\":\"ItemList\"")));
    }

    @Test
    void artistaPublico_debeMantenerContratoNoObjetivoYNoindexSiVacio() throws Exception {
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(eventoPublicoService.obtenerTodosEventosPublicos()).thenReturn(List.of());
        when(localizacionService.findAllProvincias()).thenReturn(List.of());

        mockMvc.perform(get("/eventos/artista/20"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("meta name=\"robots\" content=\"noindex,follow\"")))
            .andExpect(content().string(not(containsString("\"@type\":\"Event\""))));
    }

    private EventoPublicoDto crearEvento() {
        return EventoPublicoDto.builder()
            .id(10L)
            .idArtista(20L)
            .nombreArtista("Los Satélites")
            .municipio("Lugo")
            .provincia("Lugo")
            .lugar("Praza Maior")
            .fecha(LocalDateTime.of(2026, 8, 15, 0, 0))
            .fechaActualizacion(LocalDateTime.of(2026, 8, 1, 12, 0))
            .build();
    }
}
