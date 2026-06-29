package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.ocupacion.Ocupacion;
import es.musicalia.gestmusica.ocupacion.OcupacionEstado;
import es.musicalia.gestmusica.ocupacion.OcupacionEstadoEnum;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class EventoPublicoServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 1);
    private static final LocalDate HORIZON = LocalDate.of(2026, 8, 15);

    @Mock
    private OcupacionRepository ocupacionRepository;

    @Mock
    private ArtistaRepository artistaRepository;

    private EventoPublicoServiceImpl service;
    private EventoPublicoDateWindow dateWindow;

    @BeforeEach
    void setUp() {
        dateWindow = spy(new EventoPublicoDateWindow(Clock.fixed(
            Instant.parse("2026-07-01T10:15:30Z"),
            ZoneId.of("Europe/Madrid")
        )));
        service = new EventoPublicoServiceImpl(ocupacionRepository, artistaRepository, dateWindow);
    }

    @Test
    void obtenerEventosPublicosFiltrados_debeAplicarHorizontePublicoPorDefecto() {
        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
            crearOcupacion(1L, 20L, TODAY.minusDays(1).atTime(22, 0), "Los Satélites", "Lugo", "Lugo"),
            crearOcupacion(2L, 20L, TODAY.atTime(22, 0), "Los Satélites", "Lugo", "Lugo"),
            crearOcupacion(3L, 20L, HORIZON.atTime(22, 0), "Los Satélites", "Sarria", "Lugo"),
            crearOcupacion(4L, 20L, HORIZON.plusDays(1).atTime(22, 0), "Los Satélites", "Monforte", "Lugo")
        ));

        List<EventoPublicoDto> eventos = service.obtenerEventosPublicosFiltrados("Lugo", null, null, null, null);

        assertThat(eventos).extracting(EventoPublicoDto::getId).containsExactly(2L, 3L);
    }

    @Test
    void obtenerEventosPublicosFiltrados_debeRecortarHastaSobredimensionadaManteniendoDia45Inclusivo() {
        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
            crearOcupacion(5L, 20L, HORIZON.minusDays(1).atTime(22, 0), "Los Satélites", "Lugo", "Lugo"),
            crearOcupacion(6L, 20L, HORIZON.atTime(22, 0), "Los Satélites", "Sarria", "Lugo"),
            crearOcupacion(7L, 20L, HORIZON.plusDays(10).atTime(22, 0), "Los Satélites", "Monforte", "Lugo")
        ));

        List<EventoPublicoDto> eventos = service.obtenerEventosPublicosFiltrados(
            "Lugo", null, null, TODAY, LocalDate.of(2026, 9, 30));

        assertThat(eventos).extracting(EventoPublicoDto::getId).containsExactly(5L, 6L);
    }

    @Test
    void obtenerEventosPublicosFiltrados_debeRecortarDesdeAnteriorAHoyYExcluirEventosPasados() {
        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
            crearOcupacion(15L, 20L, TODAY.minusDays(2).atTime(22, 0), "Los Satélites", "Lugo", "Lugo"),
            crearOcupacion(16L, 20L, TODAY.atTime(22, 0), "Los Satélites", "Sarria", "Lugo"),
            crearOcupacion(17L, 20L, TODAY.plusDays(3).atTime(22, 0), "Los Satélites", "Monforte", "Lugo")
        ));

        List<EventoPublicoDto> eventos = service.obtenerEventosPublicosFiltrados(
            "Lugo", null, null, LocalDate.of(2026, 6, 28), LocalDate.of(2026, 8, 10));

        assertThat(eventos).extracting(EventoPublicoDto::getId).containsExactly(16L, 17L);
    }

    @Test
    void obtenerEventosPublicosFiltradosPaginados_debeAplicarHorizontePublicoPorDefectoIncluyendoDia45YExcluyendoDia46() {
        when(ocupacionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(
                crearOcupacion(21L, 20L, TODAY.atTime(22, 0), "Los Satélites", "Lugo", "Lugo"),
                crearOcupacion(22L, 20L, HORIZON.atTime(22, 0), "Los Satélites", "Sarria", "Lugo"),
                crearOcupacion(23L, 20L, HORIZON.plusDays(1).atTime(22, 0), "Los Satélites", "Ponferrada", "León")
            ), PageRequest.of(0, 20), 3));

        Page<EventoPublicoDto> pagina = service.obtenerEventosPublicosFiltradosPaginados(
            null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(EventoPublicoDto::getId).containsExactly(21L, 22L);
        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getTotalPages()).isEqualTo(1);
    }

    @Test
    void obtenerEventosPublicosFiltradosPaginados_debeRecortarHastaSobredimensionadaEnRutaPaginada() {
        when(ocupacionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(
                crearOcupacion(24L, 20L, HORIZON.atTime(22, 0), "Los Satélites", "Lugo", "Lugo"),
                crearOcupacion(25L, 20L, HORIZON.plusDays(10).atTime(22, 0), "Los Satélites", "Monforte", "Lugo")
            ), PageRequest.of(0, 20), 2));

        Page<EventoPublicoDto> pagina = service.obtenerEventosPublicosFiltradosPaginados(
            null, null, null, TODAY, LocalDate.of(2026, 9, 30), PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(EventoPublicoDto::getId).containsExactly(24L);
        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getTotalPages()).isEqualTo(1);
    }

    @Test
    void obtenerEventosPublicosPorArtista_debeLimitarAProximos45Dias() {
        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
            crearOcupacion(8L, 20L, TODAY.minusDays(2).atTime(22, 0), "Los Satélites", "Lugo", "Lugo"),
            crearOcupacion(9L, 20L, TODAY.plusDays(7).atTime(22, 0), "Los Satélites", "Sarria", "Lugo"),
            crearOcupacion(10L, 20L, HORIZON.plusDays(20).atTime(22, 0), "Los Satélites", "Ponferrada", "León")
        ));

        List<EventoPublicoDto> eventos = service.obtenerEventosPublicosPorArtista(20L);

        assertThat(eventos).extracting(EventoPublicoDto::getId).containsExactly(9L);
    }

    @Test
    void obtenerEventosRelacionadosPublicos_debeExcluirActualRespetarVentanaYOrdenarPorFecha() {
        LocalDate fechaDesde = TODAY;
        LocalDate fechaHasta = HORIZON;

        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
            crearOcupacion(10L, 20L, TODAY.plusDays(5).atTime(22, 0), "Los Satélites", "Lugo", "Lugo"),
            crearOcupacion(11L, 20L, TODAY.plusDays(7).atTime(22, 0), "Los Satélites", "Sarria", "Lugo"),
            crearOcupacion(12L, 20L, TODAY.minusDays(1).atTime(22, 0), "Los Satélites", "Monforte", "Lugo"),
            crearOcupacion(13L, 20L, TODAY.plusDays(6).atTime(22, 0), "Los Satélites", "Viveiro", "Lugo")
        ));

        List<EventoPublicoDto> relacionados = service.obtenerEventosRelacionadosPublicos(10L, 20L, fechaDesde, fechaHasta, 2);

        assertEquals(List.of(13L, 11L), relacionados.stream().map(EventoPublicoDto::getId).toList());
        verify(ocupacionRepository).findAll(any(Specification.class), eq(Sort.by(Sort.Direction.ASC, "fecha", "artista.nombre")));
    }

    @Test
    void obtenerFeedCalendarioArtista_debeAplicarVentanaPropiaEIncluirLimites() {
        Artista artista = crearArtistaConSuscripcion(20L, "token-ok");
        LocalDate hoy = LocalDate.now();
        Ocupacion eventoFueraDeLimiteInferior = crearOcupacion(9L, 20L, hoy.minusMonths(12).minusDays(1).atTime(22, 0), "Los Satélites", "Ourense", "Ourense");
        Ocupacion eventoEnLimiteInferior = crearOcupacion(10L, 20L, hoy.minusMonths(12).atTime(22, 0), "Los Satélites", "Lugo", "Lugo");
        eventoEnLimiteInferior.setHoraActuacionDesde(java.time.LocalTime.of(22, 0));
        eventoEnLimiteInferior.setHoraActuacionHasta(java.time.LocalTime.of(1, 0));
        eventoEnLimiteInferior.setLugar("Praza Maior");
        Ocupacion eventoEnLimite45 = crearOcupacion(45L, 20L, hoy.plusDays(45).atTime(21, 30), "Los Satélites", "Sarria", "Lugo");
        Ocupacion eventoFueraDeLimite46 = crearOcupacion(46L, 20L, hoy.plusDays(46).atTime(21, 30), "Los Satélites", "Monforte", "Lugo");

        when(artistaRepository.findByIdAndCalendarSubscriptionToken(20L, "token-ok")).thenReturn(Optional.of(artista));
        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of(eventoFueraDeLimiteInferior, eventoEnLimiteInferior, eventoEnLimite45, eventoFueraDeLimite46));

        String feed = service.obtenerFeedCalendarioArtista(20L, "token-ok");

        assertThat(feed)
            .contains("BEGIN:VCALENDAR")
            .doesNotContain("UID:9@festia.es")
            .contains("UID:10@festia.es")
            .contains("UID:45@festia.es")
            .doesNotContain("UID:46@festia.es")
            .doesNotContain("SUMMARY:Actuación de ");
    }

    @Test
    void obtenerFeedCalendarioArtista_noDebeAplicarVentanaPublicaDe45Dias() {
        Artista artista = crearArtistaConSuscripcion(20L, "token-ok");

        when(artistaRepository.findByIdAndCalendarSubscriptionToken(20L, "token-ok")).thenReturn(Optional.of(artista));
        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        service.obtenerFeedCalendarioArtista(20L, "token-ok");

        verify(dateWindow, never()).effectiveUpcomingWindow(any(), any());
    }

    @Test
    void obtenerFeedCalendarioArtista_debeConstruirFeedAllDayDentroDeVentana() {
        Artista artista = crearArtistaConSuscripcion(20L, "token-ok");
        Ocupacion eventoAllDay = crearOcupacion(11L, 20L, LocalDate.now().plusDays(1).atStartOfDay(), "Los Satélites", "Sarria", "Lugo");

        when(artistaRepository.findByIdAndCalendarSubscriptionToken(20L, "token-ok")).thenReturn(Optional.of(artista));
        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(eventoAllDay));

        String feed = service.obtenerFeedCalendarioArtista(20L, "token-ok");

        assertThat(feed)
            .contains("UID:11@festia.es")
            .contains("DTSTART;VALUE=DATE:" + LocalDate.now().plusDays(1).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
    }

    @Test
    void obtenerFeedCalendarioArtista_debeGenerarFeedVacioSiNoHayEventosElegibles() {
        Artista artista = crearArtistaConSuscripcion(20L, "token-ok");

        when(artistaRepository.findByIdAndCalendarSubscriptionToken(20L, "token-ok")).thenReturn(Optional.of(artista));
        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        String feed = service.obtenerFeedCalendarioArtista(20L, "token-ok");

        assertThat(feed)
            .contains("BEGIN:VCALENDAR")
            .contains("END:VCALENDAR")
            .doesNotContain("BEGIN:VEVENT");
    }

    @Test
    void obtenerFeedCalendarioArtista_debeResponder404UniformeSiTokenInvalidoRevocadoODeshabilitado() {
        when(artistaRepository.findByIdAndCalendarSubscriptionToken(20L, "token-invalido")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.obtenerFeedCalendarioArtista(20L, "token-invalido"));

        assertEquals(NOT_FOUND, exception.getStatusCode());
    }

    private Artista crearArtistaConSuscripcion(Long idArtista, String token) {
        Artista artista = new Artista();
        artista.setId(idArtista);
        artista.setNombre("Los Satélites");
        artista.setActivo(true);
        artista.setPublicarEventos(true);
        artista.setPermitirSuscripcionCalendario(true);
        artista.setCalendarSubscriptionToken(token);
        return artista;
    }

    private Ocupacion crearOcupacion(Long id, Long idArtista, LocalDateTime fecha, String artistaNombre, String municipioNombre, String provinciaNombre) {
        Artista artista = new Artista();
        artista.setId(idArtista);
        artista.setNombre(artistaNombre);
        artista.setActivo(true);
        artista.setPublicarEventos(true);

        Provincia provincia = new Provincia();
        provincia.setNombre(provinciaNombre);

        Municipio municipio = new Municipio();
        municipio.setNombre(municipioNombre);
        municipio.setProvincia(provincia);

        OcupacionEstado estado = new OcupacionEstado();
        estado.setId(OcupacionEstadoEnum.OCUPADO.getId());

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setId(id);
        ocupacion.setArtista(artista);
        ocupacion.setProvincia(provincia);
        ocupacion.setMunicipio(municipio);
        ocupacion.setOcupacionEstado(estado);
        ocupacion.setActivo(true);
        ocupacion.setEventoVisible(true);
        ocupacion.setFecha(fecha);
        ocupacion.setFechaCreacion(fecha.minusDays(1));
        return ocupacion;
    }
}
