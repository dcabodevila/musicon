package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.Provincia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventoPublicoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DefaultEventoPublicStructuredDataBuilder.class)
class EventoPublicoControllerMvcTest {

    private static final Pattern EVENT_TYPE_PATTERN = Pattern.compile("\\\"@type\\\":\\\"Event\\\"");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 1);
    private static final LocalDate HORIZON = LocalDate.of(2026, 8, 15);

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

    @MockBean
    private EventoPublicoDateWindow eventoPublicoDateWindow;

    @BeforeEach
    void setUpFacadeDefaults() {
        when(eventoPublicoDateWindow.today()).thenReturn(TODAY);
        when(eventoPublicoDateWindow.publicHorizon()).thenReturn(HORIZON);
        when(eventoPublicoDateWindow.clampHasta(nullable(LocalDate.class))).thenAnswer(invocation -> {
            LocalDate requestedHasta = invocation.getArgument(0);
            if (requestedHasta == null || requestedHasta.isAfter(HORIZON)) {
                return HORIZON;
            }
            return requestedHasta;
        });
        when(eventoPublicoDateWindow.effectiveUpcomingWindow(nullable(LocalDate.class), nullable(LocalDate.class))).thenAnswer(invocation -> {
            LocalDate requestedDesde = invocation.getArgument(0);
            LocalDate requestedHasta = invocation.getArgument(1);
            LocalDate effectiveDesde = requestedDesde == null || requestedDesde.isBefore(TODAY) ? TODAY : requestedDesde;
            LocalDate effectiveHasta = requestedHasta == null || requestedHasta.isAfter(HORIZON) ? HORIZON : requestedHasta;
            return new EventoPublicoDateWindow.DateRange(effectiveDesde, effectiveHasta);
        });
        when(eventoPublicoCatalogoFacade.normalizarProvinciaCanonica(nullable(String.class)))
            .thenAnswer(invocation -> {
                String provincia = invocation.getArgument(0);
                if (provincia == null) {
                    return "";
                }
                String provinciaTrim = provincia.trim();
                return "A Coruña".equalsIgnoreCase(provinciaTrim) ? "Coruña" : provinciaTrim;
            });
        when(eventoPublicoCatalogoFacade.normalizarProvinciaParaConsulta(nullable(String.class)))
            .thenAnswer(invocation -> {
                String provincia = invocation.getArgument(0);
                if (provincia == null) {
                    return "";
                }
                String provinciaTrim = provincia.trim();
                return "Coruña".equalsIgnoreCase(provinciaTrim) ? "A Coruña" : provinciaTrim;
            });
        when(eventoPublicoCatalogoFacade.esProvinciaExcluidaPublica(nullable(String.class)))
            .thenAnswer(invocation -> {
                String provincia = invocation.getArgument(0);
                if (provincia == null) {
                    return false;
                }
                String provinciaNormalizada = "A Coruña".equalsIgnoreCase(provincia.trim()) ? "Coruña" : provincia.trim();
                String provinciaLower = provinciaNormalizada.toLowerCase();
                return provinciaLower.equals("provisional") || provinciaLower.equals("otras");
            });
        when(eventoPublicoCatalogoFacade.obtenerProvinciasPublicasOrdenadas()).thenReturn(List.of());
        when(eventoPublicoCatalogoFacade.obtenerMunicipiosPublicosPorProvincia(anyString())).thenReturn(List.of());
        when(eventoPublicoCatalogoFacade.obtenerArtistasOrdenados(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void detallePublico_debeRenderizarJsonLdEventSSRConCanonicalYMetadatos() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), eq(20L), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(evento));

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
    void detallePublicoLegacy_debeRedirigirAUrlCanonicaAbsolutaCon301() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));

        mockMvc.perform(get("/eventos/evento/10"))
            .andExpect(status().isMovedPermanently())
            .andExpect(redirectedUrl("http://localhost/eventos/evento/10-los-satelites-lugo-2026-08-15"));
    }

    @Test
    void detallePublico_debeRedirigirSlugIncorrectoALaCanonicaSinRomperContratoPublico() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));

        mockMvc.perform(get("/eventos/evento/10-slug-incorrecto"))
            .andExpect(status().isMovedPermanently())
            .andExpect(redirectedUrl("http://localhost/eventos/evento/10-los-satelites-lugo-2026-08-15"));
    }

    @Test
    void detallePublico_debeEmitirUnUnicoObjetoPrincipalEvent() throws Exception {
        EventoPublicoDto evento = crearEvento();
        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), eq(20L), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(evento));

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
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), eq(20L), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(evento));

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
        when(eventoPublicoCatalogoFacade.obtenerProvinciasPublicasOrdenadas()).thenReturn(List.of("Coruña"));
        when(eventoPublicoCatalogoFacade.obtenerQuickLinksPublicos()).thenReturn(List.of(
            new EventoPublicoCatalogoFacade.QuickLinkView("Fiestas en Coruña", "/eventos/provincia/Coruña", "province", true)
        ));

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
    void provinciaPublica_debeRedirigirCasingNoCanonicoCon301Absoluto() throws Exception {
        Provincia provincia = new Provincia();
        provincia.setNombre("Lugo");

        when(localizacionService.findProvinciaByNombreUpperCase("lugo")).thenReturn(Optional.of(provincia));

        mockMvc.perform(get("/eventos/provincia/lugo"))
            .andExpect(status().isMovedPermanently())
            .andExpect(redirectedUrl("http://localhost/eventos/provincia/Lugo"));
    }

    @Test
    void provinciaPublica_debeResolverCorunaConNombreRealDeBbddSinRomperCanonical() throws Exception {
        EventoPublicoDto evento = crearEventoConDatos(10L, 20L, "Los Satélites", "Santiago", "A Coruña", LocalDateTime.now().plusDays(5));
        Provincia provincia = new Provincia();
        provincia.setNombre("A Coruña");

        when(localizacionService.findProvinciaByNombreUpperCase("A Coruña")).thenReturn(Optional.of(provincia));
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(eq("A Coruña"), isNull(), isNull(), any(LocalDate.class), any(LocalDate.class), any()))
            .thenReturn(new PageImpl<>(List.of(evento), PageRequest.of(0, 20), 1));
        when(eventoPublicoCatalogoFacade.obtenerProvinciasPublicasOrdenadas()).thenReturn(List.of("Coruña"));
        when(eventoPublicoCatalogoFacade.obtenerMunicipiosPublicosPorProvincia("A Coruña")).thenReturn(List.of());
        when(eventoPublicoCatalogoFacade.obtenerQuickLinksPublicos("Coruña", null)).thenReturn(List.of(
            new EventoPublicoCatalogoFacade.QuickLinkView("Fiestas hoy", "/eventos/hoy", "today", false)
        ));

        mockMvc.perform(get("/eventos/provincia/Coruña"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("rel=\"canonical\"")))
            .andExpect(content().string(containsString("http://localhost/eventos/provincia/Coru%C3%B1a")))
            .andExpect(content().string(not(containsString("http://localhost/eventos/provincia/A%20Coru%C3%B1a"))))
            .andExpect(content().string(containsString("href=\"/eventos/hoy\"")))
            .andExpect(content().string(containsString("<option value=\"Coruña\"")))
            .andExpect(content().string(not(containsString("<option value=\"A Coruña\""))))
            .andExpect(content().string(not(containsString("<option value=\"Otras\""))))
            .andExpect(content().string(not(containsString("<option value=\"Provisional\""))));

        verify(localizacionService).findProvinciaByNombreUpperCase("A Coruña");
        verify(eventoPublicoService).obtenerEventosPublicosFiltradosPaginados(eq("A Coruña"), isNull(), isNull(), any(LocalDate.class), any(LocalDate.class), any());
        verify(eventoPublicoCatalogoFacade).obtenerMunicipiosPublicosPorProvincia("A Coruña");
        verify(eventoPublicoCatalogoFacade).obtenerQuickLinksPublicos("Coruña", null);
    }

    @Test
    void apiMunicipios_debeResolverCorunaPublicaConNombreRealDeBbdd() throws Exception {
        Provincia provincia = new Provincia();
        provincia.setNombre("A Coruña");

        when(localizacionService.findProvinciaByNombreUpperCase("A Coruña")).thenReturn(Optional.of(provincia));
        when(localizacionService.findMunicipiosByProvinciaNombre("A Coruña"))
            .thenReturn(List.of(new CodigoNombreRecord(1L, "Coruña, A")));

        mockMvc.perform(get("/eventos/api/municipios").param("provincia", "Coruña"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Coruña, A"))
            .andExpect(jsonPath("$[0].provincia").value("A Coruña"));

        verify(localizacionService).findProvinciaByNombreUpperCase("A Coruña");
        verify(localizacionService).findMunicipiosByProvinciaNombre("A Coruña");
    }

    @Test
    void sitemap_noDebeGenerarUrlsDeMunicipioParaProvinciasPublicasExcluidas() throws Exception {
        EventoPublicoDto eventoLugo = crearEventoConDatos(10L, 20L, "Los Satélites", "Lugo", "Lugo", LocalDateTime.now().plusDays(5));
        EventoPublicoDto eventoOtras = crearEventoConDatos(11L, 21L, "Olympus", "Sin asignar", "Otras", LocalDateTime.now().plusDays(6));
        EventoPublicoDto eventoProvisional = crearEventoConDatos(12L, 22L, "Panorama", "Genérico", "Provisional", LocalDateTime.now().plusDays(7));

        when(eventoPublicoService.obtenerTodosEventosPublicos())
            .thenReturn(List.of(eventoLugo, eventoOtras, eventoProvisional));

        mockMvc.perform(get("/eventos/sitemap.xml"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(content().string(containsString("/eventos/municipio/Lugo")))
            .andExpect(content().string(not(containsString("/eventos/municipio/Sin%20asignar"))))
            .andExpect(content().string(not(containsString("/eventos/municipio/Gen%C3%A9rico"))));
    }

    @Test
    void sitemap_debeUsarProvinciaCanonicaYExcluirLandingsDeProvinciasPublicasExcluidas() throws Exception {
        EventoPublicoDto eventoCoruna = crearEventoConDatos(10L, 20L, "Los Satélites", "Santiago", "A Coruña", LocalDateTime.now().plusDays(5));
        EventoPublicoDto eventoOtras = crearEventoConDatos(11L, 21L, "Olympus", "Sin asignar", "Otras", LocalDateTime.now().plusDays(6));
        EventoPublicoDto eventoProvisional = crearEventoConDatos(12L, 22L, "Panorama", "Genérico", "Provisional", LocalDateTime.now().plusDays(7));

        when(eventoPublicoService.obtenerTodosEventosPublicos())
            .thenReturn(List.of(eventoCoruna, eventoOtras, eventoProvisional));

        mockMvc.perform(get("/eventos/sitemap.xml"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
            .andExpect(content().string(containsString("/eventos/provincia/Coru%C3%B1a")))
            .andExpect(content().string(not(containsString("/eventos/provincia/A%20Coru%C3%B1a"))))
            .andExpect(content().string(not(containsString("/eventos/provincia/Otras"))))
            .andExpect(content().string(not(containsString("/eventos/provincia/Provisional"))));
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
    void municipioPublico_debeEnlazarProvinciaCorunaConRutaPublicaCanonica() throws Exception {
        EventoPublicoDto evento = crearEventoConDatos(10L, 20L, "Los Satélites", "Coruña, A", "A Coruña", LocalDateTime.now().plusDays(5));
        Provincia provincia = new Provincia();
        provincia.setNombre("A Coruña");

        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(isNull(), eq("Coruña, A"), isNull(), any(LocalDate.class), any(LocalDate.class), any()))
            .thenReturn(new PageImpl<>(List.of(evento), PageRequest.of(0, 10), 1));
        when(localizacionService.findProvinciaByNombreUpperCase("A Coruña")).thenReturn(Optional.of(provincia));
        when(eventoPublicoCatalogoFacade.obtenerProvinciasPublicasOrdenadas()).thenReturn(List.of("Coruña"));
        when(eventoPublicoCatalogoFacade.obtenerMunicipiosPublicosPorProvincia("A Coruña"))
            .thenReturn(List.of(new CodigoNombreRecord(1404L, "Coruña, A")));
        when(eventoPublicoCatalogoFacade.obtenerQuickLinksPublicos("Coruña", "Coruña, A")).thenReturn(List.of(
            new EventoPublicoCatalogoFacade.QuickLinkView("Fiestas hoy", "/eventos/hoy", "today", false)
        ));

        mockMvc.perform(get("/eventos/municipio/{municipio}", "Coruña, A"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/eventos/hoy\"")))
            .andExpect(content().string(containsString("href=\"/eventos/provincia/Coru%C3%B1a\"")))
            .andExpect(content().string(containsString("http://localhost/eventos/provincia/Coru%C3%B1a")))
            .andExpect(content().string(not(containsString("/eventos/provincia/A%20Coru%C3%B1a"))))
            .andExpect(content().string(containsString("<option value=\"Coruña\"")));

        verify(localizacionService).findProvinciaByNombreUpperCase("A Coruña");
        verify(eventoPublicoCatalogoFacade).obtenerMunicipiosPublicosPorProvincia("A Coruña");
        verify(eventoPublicoCatalogoFacade).obtenerQuickLinksPublicos("Coruña", "Coruña, A");
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
    void artistaPublico_debeMostrarSoloEventosDeProximos45DiasYOcultarCtaCalendarioPublica() throws Exception {
        EventoPublicoDto eventoDentroDeVentana = crearEventoConDatos(10L, 20L, "Los Satélites", "Lugo", "Lugo", TODAY.plusDays(5).atTime(0, 0));
        EventoPublicoDto eventoFueraDeVentana = crearEventoConDatos(11L, 20L, "Los Satélites", "Ponferrada", "León", HORIZON.plusDays(15).atTime(0, 0));
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), eq(20L), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(eventoDentroDeVentana));
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), eq(TODAY), eq(HORIZON)))
            .thenReturn(List.of(eventoDentroDeVentana));
        when(eventoPublicoCatalogoFacade.obtenerQuickLinksPublicos()).thenReturn(List.of());

        mockMvc.perform(get("/eventos/artista/20"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Los Satélites")))
            .andExpect(content().string(not(containsString("Suscribirse al calendario"))))
            .andExpect(content().string(not(containsString("Apple Calendar"))))
            .andExpect(content().string(not(containsString("Google Calendar"))))
            .andExpect(content().string(not(containsString("Ponferrada"))));

        verify(eventoPublicoService).obtenerEventosPublicosFiltrados(
            isNull(),
            isNull(),
            eq(20L),
            eq(TODAY),
            eq(HORIZON)
        );
        verify(eventoPublicoService, never()).obtenerUrlSuscripcionCalendarioArtista(20L);
    }

    @Test
    void feedCalendarioArtista_debeResponder200ConTextCalendarInline() throws Exception {
        when(eventoPublicoService.obtenerFeedCalendarioArtista(20L, "token-ok"))
            .thenReturn("BEGIN:VCALENDAR\r\nEND:VCALENDAR");

        mockMvc.perform(get("/eventos/artista/20/calendar/token-ok.ics"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/calendar;charset=UTF-8"))
            .andExpect(header().string("Content-Disposition", containsString("inline; filename=\"festia-artista-20.ics\"")))
            .andExpect(content().string(containsString("BEGIN:VCALENDAR")));
    }

    @Test
    void feedCalendarioArtista_debeResponder404UniformeCuandoNoEsElegible() throws Exception {
        when(eventoPublicoService.obtenerFeedCalendarioArtista(20L, "token-ko"))
            .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/eventos/artista/20/calendar/token-ko.ics"))
            .andExpect(status().isNotFound());
    }

    @Test
    void listadoCatalogo_debeRenderizarQuickLinksSSRsinBloquesSecundarios() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate viernes = siguienteViernes();
        LocalDate domingo = domingoObjetivo();

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
            .andExpect(content().string(containsString("href=\"/eventos?desde=" + viernes + "&amp;hasta=" + domingo + "\"")))
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
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), eq(20L), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(evento));

        mockMvc.perform(get("/eventos/evento/10-los-satelites-lugo-2026-08-15"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Por confirmar"))))
            .andExpect(content().string(not(containsString("event-dt__label\">Hora</div>"))));
    }

    @Test
    void detallePublico_debeMostrarSoloRelacionadosDentroDe45DiasOrdenadosYSinEventoActual() throws Exception {
        EventoPublicoDto eventoActual = crearEventoConDatos(10L, 20L, "Los Satélites", "Lugo", "Lugo", TODAY.plusDays(5).atTime(0, 0));
        EventoPublicoDto relacionadoValidoTemprano = crearEventoConDatos(11L, 20L, "Los Satélites", "A Coruña", "Coruña", TODAY.plusDays(7).atTime(0, 0));
        EventoPublicoDto relacionadoValidoTardio = crearEventoConDatos(12L, 20L, "Los Satélites", "Ourense", "Ourense", TODAY.plusDays(20).atTime(0, 0));
        EventoPublicoDto relacionadoFueraDeVentana = crearEventoConDatos(13L, 20L, "Los Satélites", "Ponferrada", "León", HORIZON.plusDays(5).atTime(0, 0));
        List<EventoPublicoDto> relacionadosMismoArtista = List.of(
            relacionadoValidoTardio,
            relacionadoFueraDeVentana,
            eventoActual,
            relacionadoValidoTemprano
        );

        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(eventoActual));
        when(eventoPublicoService.obtenerEventosRelacionadosPublicos(eq(10L), eq(20L), any(LocalDate.class), any(LocalDate.class), eq(10)))
            .thenReturn(List.of(relacionadoValidoTemprano, relacionadoValidoTardio));

        String html = mockMvc.perform(get("/eventos/evento/10-" + eventoActual.getSlug()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("rel=\"canonical\"")))
            .andExpect(content().string(containsString("http://localhost/eventos/evento/10-" + eventoActual.getSlug())))
            .andExpect(content().string(containsString("meta name=\"robots\" content=\"index,follow\"")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String bloqueRelacionados = html.substring(
            html.indexOf("event-related-list"),
            html.indexOf("Informaci", html.indexOf("event-related-list"))
        );

        org.junit.jupiter.api.Assertions.assertTrue(bloqueRelacionados.contains("/eventos/evento/11-"));
        org.junit.jupiter.api.Assertions.assertTrue(bloqueRelacionados.contains("/eventos/evento/12-"));
        org.junit.jupiter.api.Assertions.assertFalse(bloqueRelacionados.contains("/eventos/evento/13-"));
        org.junit.jupiter.api.Assertions.assertFalse(bloqueRelacionados.contains("/eventos/evento/10-"));
        org.junit.jupiter.api.Assertions.assertTrue(
            bloqueRelacionados.indexOf("/eventos/evento/11-") < bloqueRelacionados.indexOf("/eventos/evento/12-"),
            "Los relacionados deben mantenerse ordenados por fecha ascendente"
        );

        verify(eventoPublicoService).obtenerEventosRelacionadosPublicos(
            eq(10L),
            eq(20L),
            eq(TODAY),
            eq(HORIZON),
            eq(10)
        );
        verify(eventoPublicoService, never()).obtenerEventosPublicosPorArtista(20L);
    }

    @Test
    void apiArtista_debeDevolverSoloEventosDentroDelHorizontePublico() throws Exception {
        EventoPublicoDto eventoProximo = crearEventoConDatos(21L, 20L, "Los Satélites", "Lugo", "Lugo", TODAY.plusDays(7).atTime(0, 0));
        List<EventoPublicoDto> eventosApi = List.of(eventoProximo);

        when(eventoPublicoService.obtenerEventosPublicosPorArtista(20L)).thenReturn(eventosApi);

        mockMvc.perform(get("/eventos/api/artista/20"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(21))
            .andExpect(jsonPath("$[0].pathPublico").value("/eventos/evento/21-los-satelites-lugo-" + eventoProximo.getFecha().toLocalDate()));

        verify(eventoPublicoService).obtenerEventosPublicosPorArtista(20L);
    }

    @Test
    void listadoCatalogo_debeRecortarHastaYUsarFechaMaxFiltroCompartida() throws Exception {
        stubCatalogoFacade(List.of(), 0);

        mockMvc.perform(get("/eventos").param("hasta", "2026-09-30"))
            .andExpect(status().isOk());

        verify(eventoPublicoCatalogoFacade).prepararCatalogoPublico(argThat(request ->
            TODAY.equals(request.fechaDesde()) && HORIZON.equals(request.fechaHasta())
        ));
    }

    @Test
    void listadoCatalogo_debeRecortarDesdeAnteriorAHoy() throws Exception {
        stubCatalogoFacade(List.of(), 0);

        mockMvc.perform(get("/eventos").param("desde", "2026-06-15"))
            .andExpect(status().isOk());

        verify(eventoPublicoCatalogoFacade).prepararCatalogoPublico(argThat(request ->
            TODAY.equals(request.fechaDesde()) && HORIZON.equals(request.fechaHasta())
        ));
    }

    @Test
    void listadoCatalogo_debeOcultarEnHtmlEventosMasAllaDelDia45AunqueLleguenEnLaPagina() throws Exception {
        EventoPublicoDto eventoDentroDeVentana = crearEventoConDatos(31L, 20L, "Los Satélites", "Sarria", "Lugo", TODAY.plusDays(5).atTime(22, 0));
        EventoPublicoDto eventoFueraDeVentana = crearEventoConDatos(32L, 21L, "Olympus", "Ponferrada", "León", HORIZON.plusDays(1).atTime(22, 0));

        when(eventoPublicoCatalogoFacade.prepararCatalogoPublico(any()))
            .thenReturn(new EventoPublicoCatalogoFacade.EventoPublicoCatalogoView(
                new PageImpl<>(List.of(eventoDentroDeVentana, eventoFueraDeVentana), PageRequest.of(0, 20), 2),
                List.of(eventoDentroDeVentana, eventoFueraDeVentana),
                List.of(),
                List.of(),
                List.of(eventoDentroDeVentana, eventoFueraDeVentana),
                List.of(),
                "Orquestas, verbenas y actuaciones musicales en España | Festia",
                "Consulta fiestas, verbenas, orquestas y actuaciones musicales de próximos eventos en España"
            ));

        mockMvc.perform(get("/eventos"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Sarria")))
            .andExpect(content().string(not(containsString("Ponferrada"))));
    }

    @Test
    void provinciaPublica_debeSolicitarSoloEventosDentroDeHoyAHorizonte() throws Exception {
        Provincia provincia = new Provincia();
        provincia.setNombre("Lugo");
        when(localizacionService.findProvinciaByNombreUpperCase("Lugo")).thenReturn(Optional.of(provincia));
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(eq("Lugo"), isNull(), isNull(), eq(TODAY), eq(HORIZON), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/eventos/provincia/Lugo"))
            .andExpect(status().isOk());
    }

    @Test
    void provinciaPublica_debeOcultarEnHtmlEventosMasAllaDelDia45AunqueLleguenEnLaPagina() throws Exception {
        Provincia provincia = new Provincia();
        provincia.setNombre("Lugo");
        EventoPublicoDto eventoDentroDeVentana = crearEventoConDatos(41L, 20L, "Los Satélites", "Sarria", "Lugo", TODAY.plusDays(3).atTime(22, 0));
        EventoPublicoDto eventoFueraDeVentana = crearEventoConDatos(42L, 21L, "Olympus", "Ponferrada", "Lugo", HORIZON.plusDays(1).atTime(22, 0));

        when(localizacionService.findProvinciaByNombreUpperCase("Lugo")).thenReturn(Optional.of(provincia));
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(eq("Lugo"), isNull(), isNull(), eq(TODAY), eq(HORIZON), any()))
            .thenReturn(new PageImpl<>(List.of(eventoDentroDeVentana, eventoFueraDeVentana), PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/eventos/provincia/Lugo"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Sarria")))
            .andExpect(content().string(not(containsString("Ponferrada"))));
    }

    @Test
    void provinciaPublica_debePreservarMetadatosDePaginacionAunqueFiltreEventosFueraDeVentana() throws Exception {
        Provincia provincia = new Provincia();
        provincia.setNombre("Lugo");
        EventoPublicoDto eventoFueraDeVentana = crearEventoConDatos(43L, 21L, "Olympus", "Ponferrada", "Lugo", HORIZON.plusDays(1).atTime(22, 0));

        when(localizacionService.findProvinciaByNombreUpperCase("Lugo")).thenReturn(Optional.of(provincia));
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(eq("Lugo"), isNull(), isNull(), eq(TODAY), eq(HORIZON), any()))
            .thenReturn(new PageImpl<>(List.of(eventoFueraDeVentana), PageRequest.of(1, 20), 21));

        mockMvc.perform(get("/eventos/provincia/Lugo").param("page", "2"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("totalPaginas", 2))
            .andExpect(model().attribute("totalEventos", 21L))
            .andExpect(model().attribute("paginaActual", 2))
            .andExpect(content().string(not(containsString("Ponferrada"))));
    }

    @Test
    void municipioApi_debeRecortarHastaSobredimensionada() throws Exception {
        when(eventoPublicoService.obtenerEventosPublicosPorMunicipio(eq("Sarria"), eq(TODAY), eq(HORIZON)))
            .thenReturn(List.of());

        mockMvc.perform(get("/eventos/api/municipio/Sarria").param("hasta", "2026-09-30"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(eventoPublicoService).obtenerEventosPublicosPorMunicipio("Sarria", TODAY, HORIZON);
    }

    @Test
    void provinciaApi_debeUsarVentanaPublicaPorDefectoCuandoHastaNoSeInforma() throws Exception {
        when(eventoPublicoService.obtenerEventosPublicosPorProvincia(eq("Lugo"), eq(TODAY), eq(HORIZON)))
            .thenReturn(List.of());

        mockMvc.perform(get("/eventos/api/provincia/Lugo"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(eventoPublicoService).obtenerEventosPublicosPorProvincia("Lugo", TODAY, HORIZON);
    }

    @Test
    void descargaIcal_debeMantenerAdjuntoContentTypeYCamposEscapados() throws Exception {
        EventoPublicoDto evento = crearEventoConDatos(10L, 20L, "Los Satélites", "Lugo", "Lugo", LocalDateTime.of(2026, 8, 15, 22, 0));
        evento.setLugar("Recinto\\nFeiral");

        when(eventoPublicoService.obtenerEventoPublico(10L)).thenReturn(Optional.of(evento));

        mockMvc.perform(get("/eventos/evento/10-los-satelites-lugo-2026-08-15/ical"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/calendar;charset=UTF-8"))
            .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"festia-evento-10.ics\"")))
            .andExpect(content().string(containsString("BEGIN:VCALENDAR\r\n")))
            .andExpect(content().string(containsString("DTSTART;TZID=Europe/Madrid:20260815T220000")))
            .andExpect(content().string(containsString("DTEND;TZID=Europe/Madrid:20260816T010000")))
            .andExpect(content().string(containsString("LOCATION:Recinto")))
            .andExpect(content().string(containsString("Feiral")))
            .andExpect(content().string(containsString("Lugo")));
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

    private LocalDate siguienteViernes() {
        LocalDate fecha = LocalDate.now();
        while (fecha.getDayOfWeek() != java.time.DayOfWeek.FRIDAY) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }

    private LocalDate domingoObjetivo() {
        LocalDate fecha = LocalDate.now();
        while (fecha.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
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
        List<EventoPublicoCatalogoFacade.QuickLinkView> quickLinks = List.of(
            new EventoPublicoCatalogoFacade.QuickLinkView("Fiestas hoy", "/eventos/hoy", "today", false),
            new EventoPublicoCatalogoFacade.QuickLinkView(
                "Fiestas este fin de semana",
                "/eventos?desde=" + siguienteViernes() + "&hasta=" + domingoObjetivo(),
                "weekend",
                false
            )
        );

        when(eventoPublicoCatalogoFacade.prepararCatalogoPublico(any()))
            .thenReturn(new EventoPublicoCatalogoFacade.EventoPublicoCatalogoView(
                new PageImpl<>(catalogo, PageRequest.of(0, 20), total),
                catalogo,
                List.of(),
                List.of(),
                catalogo,
                quickLinks,
                "Orquestas, verbenas y actuaciones musicales en España | Festia",
                "Consulta fiestas, verbenas, orquestas y actuaciones musicales de próximos eventos en España"
            ));
        when(eventoPublicoCatalogoFacade.obtenerQuickLinksPublicos()).thenReturn(quickLinks);
    }
}
