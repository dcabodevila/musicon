package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventoPublicoCatalogoFacadeImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 1);
    private static final LocalDate HORIZON = LocalDate.of(2026, 8, 15);

    @Mock
    private EventoPublicoService eventoPublicoService;

    @Mock
    private LocalizacionService localizacionService;

    private EventoPublicoCatalogoFacadeImpl facade;

    @BeforeEach
    void setUp() {
        EventoPublicoDateWindow dateWindow = new EventoPublicoDateWindow(Clock.fixed(
            Instant.parse("2026-07-01T10:15:30Z"),
            ZoneId.of("Europe/Madrid")
        ));
        facade = new EventoPublicoCatalogoFacadeImpl(eventoPublicoService, localizacionService, dateWindow);
    }

    @Test
    void prepararCatalogoPublico_debeLimitarQuickLinksDinamicosANoMasDeSieteYNormalizarCoruna() {
        LocalDate hoy = TODAY;
        List<EventoPublicoDto> catalogo = List.of(
            crearEvento(1L, 10L, "Lugo", "Lugo", "Orquesta 10", hoy.plusDays(1).atTime(22, 0)),
            crearEvento(2L, 11L, "Vigo", "Pontevedra", "Orquesta 11", hoy.plusDays(2).atTime(22, 0)),
            crearEvento(3L, 12L, "Santiago", "A Coruña", "Orquesta 12", hoy.plusDays(3).atTime(22, 0)),
            crearEvento(31L, 12L, "Arteixo", "A Coruña", "Orquesta 12", hoy.plusDays(4).atTime(22, 0)),
            crearEvento(32L, 12L, "Betanzos", "A Coruña", "Orquesta 12", hoy.plusDays(5).atTime(22, 0)),
            crearEvento(4L, 13L, "Ourense", "Ourense", "Orquesta 13", hoy.plusDays(4).atTime(22, 0)),
            crearEvento(5L, 14L, "Ponferrada", "León", "Orquesta 14", hoy.plusDays(5).atTime(22, 0)),
            crearEvento(6L, 15L, "Burgos", "Burgos", "Orquesta 15", hoy.plusDays(6).atTime(22, 0)),
            crearEvento(7L, 16L, "Avilés", "Asturias", "Orquesta 16", hoy.plusDays(7).atTime(22, 0)),
            crearEvento(8L, 17L, "Bilbao", "Bizkaia", "Orquesta 17", hoy.plusDays(8).atTime(22, 0)),
            crearEvento(9L, 18L, "Zamora", "Zamora", "Orquesta 18", hoy.plusDays(9).atTime(22, 0)),
            crearEvento(90L, 21L, "Provisional", "Lugo", "Orquesta 21", hoy.plusDays(2).atTime(22, 0)),
            crearEvento(91L, 19L, "Genérico", "Provisional", "Orquesta 19", hoy.plusDays(2).atTime(22, 0)),
            crearEvento(92L, 20L, "Sin asignar", "Otras", "Orquesta 20", hoy.plusDays(2).atTime(22, 0))
        );

        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(catalogo);
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(catalogo, PageRequest.of(0, 20), catalogo.size()));
        when(localizacionService.findAllProvincias()).thenReturn(List.of(
            new CodigoNombreRecord(1L, "A Coruña"),
            new CodigoNombreRecord(2L, "Otras"),
            new CodigoNombreRecord(3L, "Provisional"),
            new CodigoNombreRecord(4L, "Lugo")
        ));
        when(localizacionService.findMunicipiosByProvinciaNombre("A Coruña")).thenReturn(List.of());

        EventoPublicoCatalogoFacade.EventoPublicoCatalogoView view = facade.prepararCatalogoPublico(
            new EventoPublicoCatalogoFacade.EventoPublicoCatalogoRequest(
                null, null, null, hoy, HORIZON, PageRequest.of(0, 20), 1));

        long dinamicos = view.quickLinks().stream().filter(EventoPublicoCatalogoFacade.QuickLinkView::dynamic).count();
        assertTrue(dinamicos <= 7);
        assertTrue(view.quickLinks().stream().anyMatch(link -> link.href().contains("/eventos/provincia/Coru%C3%B1a")));
        assertTrue(view.quickLinks().stream().noneMatch(link -> link.href().contains("A%20Coru%C3%B1a")));
        assertTrue(view.quickLinks().stream().noneMatch(link -> link.href().contains("Provisional") || link.href().contains("Otras")));
        assertEquals(List.of("Coruña", "Lugo"), view.provincias());

        facade.prepararCatalogoPublico(new EventoPublicoCatalogoFacade.EventoPublicoCatalogoRequest(
            "Coruña", null, null, hoy, HORIZON, PageRequest.of(0, 20), 1));
        verify(eventoPublicoService).obtenerEventosPublicosFiltradosPaginados(
            eq("A Coruña"), isNull(), isNull(), eq(hoy), eq(HORIZON), any());
        verify(eventoPublicoService, times(2)).obtenerEventosPublicosFiltrados(
            isNull(), isNull(), isNull(), eq(hoy), eq(HORIZON));
        verify(localizacionService).findMunicipiosByProvinciaNombre("A Coruña");
    }

    @Test
    void prepararCatalogoPublico_debeRecortarHastaSolicitadaMasAllaDelHorizonte() {
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of());
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(localizacionService.findAllProvincias()).thenReturn(List.of());

        facade.prepararCatalogoPublico(new EventoPublicoCatalogoFacade.EventoPublicoCatalogoRequest(
            null, null, null, TODAY, LocalDate.of(2026, 9, 30), PageRequest.of(0, 20), 1));

        verify(eventoPublicoService).obtenerEventosPublicosFiltradosPaginados(
            eq(""), isNull(), isNull(), eq(TODAY), eq(HORIZON), any());
    }

    @Test
    void prepararCatalogoPublico_debeOrdenarArtistasPorNombreSinDuplicados() {
        LocalDate hoy = TODAY;
        List<EventoPublicoDto> catalogo = List.of(
            crearEvento(1L, 2L, "Lugo", "Lugo", "Zeta Show", hoy.plusDays(2).atTime(20, 0)),
            crearEvento(2L, 1L, "Vigo", "Pontevedra", "Alfa Band", hoy.plusDays(3).atTime(20, 0)),
            crearEvento(3L, 2L, "Ourense", "Ourense", "Zeta Show", hoy.plusDays(4).atTime(20, 0))
        );

        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(catalogo);
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(catalogo, PageRequest.of(0, 20), catalogo.size()));
        when(localizacionService.findAllProvincias()).thenReturn(List.of());

        EventoPublicoCatalogoFacade.EventoPublicoCatalogoView view = facade.prepararCatalogoPublico(
            new EventoPublicoCatalogoFacade.EventoPublicoCatalogoRequest(
                null, null, null, hoy, HORIZON, PageRequest.of(0, 20), 1));

        assertEquals(List.of("Alfa Band", "Zeta Show"),
            view.artistasDisponibles().stream().map(EventoPublicoDto::getNombreArtista).toList());
    }

    @Test
    void metodosPublicosDeCatalogo_debenNormalizarCorunaYExcluirValoresPublicos() {
        when(localizacionService.findAllProvincias()).thenReturn(List.of(
            new CodigoNombreRecord(1L, "A Coruña"),
            new CodigoNombreRecord(2L, "Otras"),
            new CodigoNombreRecord(3L, "Provisional"),
            new CodigoNombreRecord(4L, "Lugo")
        ));

        assertEquals("Coruña", facade.normalizarProvinciaCanonica("A Coruña"));
        assertEquals("A Coruña", facade.normalizarProvinciaParaConsulta("Coruña"));
        assertTrue(facade.esProvinciaExcluidaPublica("Otras"));
        assertTrue(facade.esProvinciaExcluidaPublica("Provisional"));
        assertEquals(List.of("Coruña", "Lugo"), facade.obtenerProvinciasPublicasOrdenadas());
    }

    @Test
    void metodosPublicosDeCatalogo_debenResolverMunicipiosContextualesConNombreRealDeProvincia() {
        when(localizacionService.findMunicipiosByProvinciaNombre("A Coruña")).thenReturn(List.of(
            new CodigoNombreRecord(1L, "Santiago de Compostela"),
            new CodigoNombreRecord(2L, "Sin asignar"),
            new CodigoNombreRecord(3L, "Provisional")
        ));

        List<CodigoNombreRecord> municipios = facade.obtenerMunicipiosPublicosPorProvincia("Coruña");

        assertEquals(List.of("Santiago de Compostela"), municipios.stream().map(CodigoNombreRecord::nombre).toList());
        verify(localizacionService).findMunicipiosByProvinciaNombre("A Coruña");
    }

    @Test
    void metodosPublicosDeCatalogo_debenOrdenarArtistasSinDuplicados() {
        List<EventoPublicoDto> eventos = List.of(
            crearEvento(1L, 2L, "Lugo", "Lugo", "Zeta Show", LocalDate.now().plusDays(1).atTime(20, 0)),
            crearEvento(2L, 1L, "Vigo", "Pontevedra", "Alfa Band", LocalDate.now().plusDays(2).atTime(20, 0)),
            crearEvento(3L, 2L, "Ourense", "Ourense", "Zeta Show", LocalDate.now().plusDays(3).atTime(20, 0))
        );

        assertEquals(List.of("Alfa Band", "Zeta Show"),
            facade.obtenerArtistasOrdenados(eventos).stream().map(EventoPublicoDto::getNombreArtista).toList());
    }

    @Test
    void obtenerQuickLinksPublicos_debeContextualizarMunicipiosParaProvincia() {
        LocalDate hoy = TODAY;
        List<EventoPublicoDto> catalogo = List.of(
            crearEvento(1L, 10L, "Lugo", "Lugo", "Orquesta 10", hoy.plusDays(1).atTime(22, 0)),
            crearEvento(2L, 11L, "Monforte de Lemos", "Lugo", "Orquesta 11", hoy.plusDays(2).atTime(22, 0)),
            crearEvento(3L, 12L, "Sarria", "Lugo", "Orquesta 12", hoy.plusDays(3).atTime(22, 0)),
            crearEvento(4L, 13L, "Vigo", "Pontevedra", "Orquesta 13", hoy.plusDays(4).atTime(22, 0)),
            crearEvento(5L, 14L, "Provisional", "Lugo", "Orquesta 14", hoy.plusDays(5).atTime(22, 0)),
            crearEvento(6L, 15L, "Betanzos", "A Coruña", "Orquesta 15", HORIZON.plusDays(1).atTime(22, 0))
        );

        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(catalogo);

        List<EventoPublicoCatalogoFacade.QuickLinkView> quickLinks = facade.obtenerQuickLinksPublicos("Lugo", null);

        assertTrue(quickLinks.stream().anyMatch(link -> link.label().equals("Verbenas en Lugo")));
        assertTrue(quickLinks.stream().anyMatch(link -> link.label().equals("Verbenas en Monforte de Lemos")));
        assertTrue(quickLinks.stream().anyMatch(link -> link.label().equals("Verbenas en Sarria")));
        assertTrue(quickLinks.stream().noneMatch(link -> link.label().equals("Verbenas en Vigo")));
        assertTrue(quickLinks.stream().noneMatch(link -> link.label().contains("Provisional")));
        assertTrue(quickLinks.stream().noneMatch(link -> link.label().contains("Betanzos")));
        assertTrue(quickLinks.stream().noneMatch(link -> link.label().equals("Fiestas en Lugo")));
    }

    @Test
    void obtenerQuickLinksPublicos_debeContextualizarOtrosMunicipiosDeLaProvinciaParaMunicipio() {
        LocalDate hoy = TODAY;
        List<EventoPublicoDto> catalogo = List.of(
            crearEvento(1L, 10L, "Lugo", "Lugo", "Orquesta 10", hoy.plusDays(1).atTime(22, 0)),
            crearEvento(2L, 11L, "Monforte de Lemos", "Lugo", "Orquesta 11", hoy.plusDays(2).atTime(22, 0)),
            crearEvento(3L, 12L, "Sarria", "Lugo", "Orquesta 12", hoy.plusDays(3).atTime(22, 0)),
            crearEvento(4L, 13L, "Vigo", "Pontevedra", "Orquesta 13", hoy.plusDays(4).atTime(22, 0)),
            crearEvento(5L, 14L, "Sin asignar", "Lugo", "Orquesta 14", hoy.plusDays(5).atTime(22, 0))
        );

        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(catalogo);

        List<EventoPublicoCatalogoFacade.QuickLinkView> quickLinks = facade.obtenerQuickLinksPublicos("Lugo", "Lugo");

        assertTrue(quickLinks.stream().anyMatch(link -> link.label().equals("Verbenas en Monforte de Lemos")));
        assertTrue(quickLinks.stream().anyMatch(link -> link.label().equals("Verbenas en Sarria")));
        assertTrue(quickLinks.stream().noneMatch(link -> link.label().equals("Verbenas en Lugo")));
        assertTrue(quickLinks.stream().noneMatch(link -> link.label().equals("Verbenas en Vigo")));
        assertTrue(quickLinks.stream().noneMatch(link -> link.label().contains("Sin asignar")));
        assertTrue(quickLinks.stream().noneMatch(link -> link.label().equals("Fiestas en Lugo")));
    }

    @Test
    void obtenerQuickLinksPublicos_debeUsarRangoDeViernesADomingoEnElEnlaceDeFinDeSemana() {
        LocalDate hoy = LocalDate.now();
        List<EventoPublicoDto> catalogo = List.of(
            crearEvento(1L, 10L, "Lugo", "Lugo", "Orquesta 10", hoy.plusDays(1).atTime(22, 0))
        );

        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(catalogo);

        List<EventoPublicoCatalogoFacade.QuickLinkView> quickLinks = facade.obtenerQuickLinksPublicos();

        EventoPublicoCatalogoFacade.QuickLinkView finDeSemana = quickLinks.stream()
            .filter(link -> link.label().equals("Fiestas este fin de semana"))
            .findFirst()
            .orElseThrow();

        assertEquals(
            "/eventos?desde=" + calcularViernesEsperado(hoy) + "&hasta=" + calcularDomingoEsperado(hoy),
            finDeSemana.href()
        );
    }

    @Test
    void obtenerQuickLinksPublicos_debeSolicitarCatalogoDentroDelHorizonteCompartido() {
        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of());

        facade.obtenerQuickLinksPublicos();

        verify(eventoPublicoService).obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), eq(TODAY), eq(HORIZON));
    }

    @Test
    void calcularViernesObjetivo_debeApuntarAlProximoViernesSiHoyEsLaborable() {
        LocalDate fechaBase = LocalDate.of(2026, 6, 15);

        assertEquals(LocalDate.of(2026, 6, 19), EventoPublicoCatalogoFacadeImpl.calcularViernesObjetivo(fechaBase));
        assertEquals(LocalDate.of(2026, 6, 21), EventoPublicoCatalogoFacadeImpl.calcularDomingoObjetivo(fechaBase));
    }

    @Test
    void calcularViernesObjetivo_debeMantenerElFinDeSemanaActualSiHoyEsDomingo() {
        LocalDate fechaBase = LocalDate.of(2026, 6, 21);

        assertEquals(LocalDate.of(2026, 6, 19), EventoPublicoCatalogoFacadeImpl.calcularViernesObjetivo(fechaBase));
        assertEquals(LocalDate.of(2026, 6, 21), EventoPublicoCatalogoFacadeImpl.calcularDomingoObjetivo(fechaBase));
    }

    private EventoPublicoDto crearEvento(Long id, Long idArtista, String municipio, String provincia, String nombreArtista, LocalDateTime fecha) {
        return EventoPublicoDto.builder()
            .id(id)
            .idArtista(idArtista)
            .nombreArtista(nombreArtista)
            .municipio(municipio)
            .provincia(provincia)
            .fecha(fecha)
            .build();
    }

    private LocalDate calcularViernesEsperado(LocalDate fechaBase) {
        return switch (fechaBase.getDayOfWeek()) {
            case FRIDAY, SATURDAY -> fechaBase.with(java.time.DayOfWeek.FRIDAY);
            case SUNDAY -> fechaBase.minusDays(2);
            default -> fechaBase.with(java.time.DayOfWeek.FRIDAY);
        };
    }

    private LocalDate calcularDomingoEsperado(LocalDate fechaBase) {
        return switch (fechaBase.getDayOfWeek()) {
            case FRIDAY, SATURDAY -> fechaBase.with(java.time.DayOfWeek.SUNDAY);
            case SUNDAY -> fechaBase;
            default -> fechaBase.with(java.time.DayOfWeek.SUNDAY);
        };
    }
}
