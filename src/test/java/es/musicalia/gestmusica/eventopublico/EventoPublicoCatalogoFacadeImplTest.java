package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventoPublicoCatalogoFacadeImplTest {

    @Mock
    private EventoPublicoService eventoPublicoService;

    @Mock
    private LocalizacionService localizacionService;

    @InjectMocks
    private EventoPublicoCatalogoFacadeImpl facade;

    @Test
    void prepararCatalogoPublico_debeLimitarQuickLinksDinamicosANoMasDeSieteYNormalizarCoruna() {
        LocalDate hoy = LocalDate.now();
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
            crearEvento(91L, 19L, "Genérico", "Provisional", "Orquesta 19", hoy.plusDays(2).atTime(22, 0)),
            crearEvento(92L, 20L, "Sin asignar", "Otras", "Orquesta 20", hoy.plusDays(2).atTime(22, 0))
        );

        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), any(LocalDate.class), isNull()))
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
                null, null, null, hoy, hoy.plusDays(45), PageRequest.of(0, 20), 1));

        long dinamicos = view.quickLinks().stream().filter(EventoPublicoCatalogoFacade.QuickLinkView::dynamic).count();
        assertTrue(dinamicos <= 7);
        assertTrue(view.quickLinks().stream().anyMatch(link -> link.href().contains("/eventos/provincia/Coru%C3%B1a")));
        assertTrue(view.quickLinks().stream().noneMatch(link -> link.href().contains("A%20Coru%C3%B1a")));
        assertTrue(view.quickLinks().stream().noneMatch(link -> link.href().contains("Provisional") || link.href().contains("Otras")));
        assertEquals(List.of("Coruña", "Lugo"), view.provincias());

        facade.prepararCatalogoPublico(new EventoPublicoCatalogoFacade.EventoPublicoCatalogoRequest(
            "Coruña", null, null, hoy, hoy.plusDays(45), PageRequest.of(0, 20), 1));
        verify(eventoPublicoService).obtenerEventosPublicosFiltradosPaginados(
            eq("A Coruña"), isNull(), isNull(), eq(hoy), eq(hoy.plusDays(45)), any());
        verify(localizacionService).findMunicipiosByProvinciaNombre("A Coruña");
    }

    @Test
    void prepararCatalogoPublico_debeOrdenarArtistasPorNombreSinDuplicados() {
        LocalDate hoy = LocalDate.now();
        List<EventoPublicoDto> catalogo = List.of(
            crearEvento(1L, 2L, "Lugo", "Lugo", "Zeta Show", hoy.plusDays(2).atTime(20, 0)),
            crearEvento(2L, 1L, "Vigo", "Pontevedra", "Alfa Band", hoy.plusDays(3).atTime(20, 0)),
            crearEvento(3L, 2L, "Ourense", "Ourense", "Zeta Show", hoy.plusDays(4).atTime(20, 0))
        );

        when(eventoPublicoService.obtenerEventosPublicosFiltrados(isNull(), isNull(), isNull(), any(LocalDate.class), isNull()))
            .thenReturn(catalogo);
        when(eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(catalogo, PageRequest.of(0, 20), catalogo.size()));
        when(localizacionService.findAllProvincias()).thenReturn(List.of());

        EventoPublicoCatalogoFacade.EventoPublicoCatalogoView view = facade.prepararCatalogoPublico(
            new EventoPublicoCatalogoFacade.EventoPublicoCatalogoRequest(
                null, null, null, hoy, hoy.plusDays(45), PageRequest.of(0, 20), 1));

        assertEquals(List.of("Alfa Band", "Zeta Show"),
            view.artistasDisponibles().stream().map(EventoPublicoDto::getNombreArtista).toList());
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
}
