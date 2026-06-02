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
import java.util.stream.Stream;
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
    private EventoPublicoCatalogoFacade eventoPublicoCatalogoFacade;

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
        stubCatalogoFacade(List.of(), 0);

        mockMvc.perform(get("/eventos"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("meta name=\"robots\" content=\"noindex,follow\"")));
    }

    @Test
    void listadosPublicos_debenEnlazarACorunaConRutaCanonicaDeProvincia() throws Exception {
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(localizacionService.findAllProvincias()).thenReturn(List.of());

        mockMvc.perform(get("/eventos/hoy"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/eventos/provincia/Coruña\"")))
            .andExpect(content().string(not(containsString("href=\"/eventos/provincia/A Coruña\""))));
    }

    @Test
    void provinciaPublica_debeRedirigirAliasACorunaCanonica() throws Exception {
        mockMvc.perform(get("/eventos/provincia/A%20Coru%C3%B1a"))
            .andExpect(status().isMovedPermanently())
            .andExpect(redirectedUrl("http://localhost/eventos/provincia/Coru%C3%B1a"));
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

    @Test
    void listadoCatalogo_debeRenderizarQuickLinksSSRsinBloquesSecundarios() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate sabado = siguienteSabado();
        LocalDate domingo = sabado.plusDays(1);

        List<EventoPublicoDto> catalogo = Stream.of(
                crearEventoConDatos(10L, 20L, "Los Satélites", "Lugo", "Lugo", hoy.plusDays(2).atTime(22, 0)),
                crearEventoConDatos(11L, 30L, "Olympus", "Vigo", "Pontevedra", hoy.plusDays(3).atTime(23, 0)),
                crearEventoConDatos(12L, 40L, "Panorama", "A Coruña", "Coruña", hoy.plusDays(4).atTime(21, 30)),
                crearEventoConDatos(13L, 50L, "Paris de Noia", "Ourense", "Ourense", hoy.plusDays(5).atTime(21, 30)),
                crearEventoConDatos(14L, 60L, "Kubo", "Ponferrada", "León", hoy.plusDays(6).atTime(20, 30)),
                crearEventoConDatos(15L, 70L, "La Misión", "Burgos", "Burgos", hoy.plusDays(7).atTime(20, 30)),
                crearEventoConDatos(16L, 80L, "Passarela", "Avilés", "Asturias", hoy.plusDays(8).atTime(20, 30)),
                crearEventoConDatos(17L, 90L, "Fania", "Bilbao", "Bizkaia", hoy.plusDays(9).atTime(20, 30)),
                crearEventoConDatos(18L, 100L, "Gran Parada", "Zamora", "Zamora", hoy.plusDays(10).atTime(20, 30))
            )
            .toList();

        stubCatalogoFacade(catalogo, catalogo.size());

        String html = mockMvc.perform(get("/eventos"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Provincias con más actuaciones"))))
            .andExpect(content().string(not(containsString("Municipios con más actuaciones"))))
            .andExpect(content().string(not(containsString("Próximas actuaciones confirmadas"))))
            .andExpect(content().string(not(containsString("Orquestas y artistas con más fechas próximas"))))
            .andExpect(content().string(containsString("href=\"/eventos/hoy\"")))
            .andExpect(content().string(containsString("href=\"/eventos?desde=" + sabado + "&amp;hasta=" + domingo + "\"")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(contarOcurrencias(html, "class=\"eventos-quick-link") >= 2,
            "La zona de filtros debe renderizar quick links SSR visibles");
        int enlacesDinamicos = contarOcurrencias(html, "eventos-quick-link--dynamic\">");
        org.junit.jupiter.api.Assertions.assertTrue(enlacesDinamicos <= 7,
            "Los quick links dinámicos no deben superar 7");
    }

    @Test
    void listadoCatalogo_debeUsarCopySeoPrincipalAlineado() throws Exception {
        EventoPublicoDto evento = crearEvento();
        stubCatalogoFacade(List.of(evento), 1);

        mockMvc.perform(get("/eventos"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Orquestas, verbenas y actuaciones musicales en España | Festia")))
            .andExpect(content().string(containsString("Consulta fiestas, verbenas, orquestas y actuaciones musicales de próximos eventos en España")))
            .andExpect(content().string(containsString("Próximas fiestas, verbenas y orquestas en")))
            .andExpect(content().string(containsString(">España<")))
            .andExpect(content().string(not(containsString("Horario por confirmar"))));
    }

    @Test
    void detallePublico_conHoraSinConfirmar_noDebeMostrarPlaceholderDeHora() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));
        when(eventoPublicoService.obtenerEventosPublicosPorArtista(20L)).thenReturn(List.of(evento));

        mockMvc.perform(get("/eventos/evento/10-los-satelites-lugo-2026-08-15"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Por confirmar"))))
            .andExpect(content().string(not(containsString("event-dt__label\">Hora</div>"))));
    }

    private EventoPublicoDto crearEvento() {
        return crearEventoConDatos(10L, 20L, "Los Satélites", "Lugo", "Lugo", LocalDateTime.of(2026, 8, 15, 0, 0));
    }

    private EventoPublicoDto crearEventoConDatos(Long id, Long idArtista, String nombreArtista, String municipio,
                                                  String provincia, LocalDateTime fecha) {
        return EventoPublicoDto.builder()
            .id(id)
            .idArtista(idArtista)
            .nombreArtista(nombreArtista)
            .municipio(municipio)
            .provincia(provincia)
            .lugar("Praza Maior")
            .fecha(fecha)
            .fechaActualizacion(fecha.minusDays(1))
            .build();
    }

    private LocalDate siguienteSabado() {
        LocalDate fecha = LocalDate.now();
        while (fecha.getDayOfWeek().getValue() != 6) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }

    private int contarOcurrencias(String texto, String objetivo) {
        int contador = 0;
        int indice = 0;
        while ((indice = texto.indexOf(objetivo, indice)) != -1) {
            contador++;
            indice += objetivo.length();
        }
        return contador;
    }

    private void stubCatalogoFacade(List<EventoPublicoDto> catalogo, long total) {
        when(eventoPublicoCatalogoFacade.prepararCatalogoPublico(any()))
            .thenReturn(new EventoPublicoCatalogoFacade.EventoPublicoCatalogoView(
                new PageImpl<>(catalogo, PageRequest.of(0, 20), total),
                catalogo,
                List.of(),
                List.of(),
                catalogo,
                List.of(
                    new EventoPublicoCatalogoFacade.QuickLinkView("Fiestas hoy", "/eventos/hoy", "today", false),
                    new EventoPublicoCatalogoFacade.QuickLinkView(
                        "Fiestas este fin de semana",
                        "/eventos?desde=" + siguienteSabado() + "&hasta=" + siguienteSabado().plusDays(1),
                        "weekend",
                        false
                    )
                ),
                "Orquestas, verbenas y actuaciones musicales en España | Festia",
                "Consulta fiestas, verbenas, orquestas y actuaciones musicales de próximos eventos en España"
            ));
    }
}
