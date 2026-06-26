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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EventoPublicoServiceImplTest {

    @Mock
    private OcupacionRepository ocupacionRepository;

    @Mock
    private ArtistaRepository artistaRepository;

    private EventoPublicoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventoPublicoServiceImpl(ocupacionRepository, artistaRepository);
    }

    @Test
    void obtenerEventosRelacionadosPublicos_debeExcluirActualRespetarVentanaYOrdenarPorFecha() {
        LocalDate fechaDesde = LocalDate.of(2026, 8, 15);
        LocalDate fechaHasta = LocalDate.of(2026, 8, 20);

        when(ocupacionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(
            crearOcupacion(10L, 20L, LocalDateTime.of(2026, 8, 16, 22, 0), "Los Satélites", "Lugo", "Lugo"),
            crearOcupacion(11L, 20L, LocalDateTime.of(2026, 8, 18, 22, 0), "Los Satélites", "Sarria", "Lugo"),
            crearOcupacion(12L, 20L, LocalDateTime.of(2026, 8, 14, 22, 0), "Los Satélites", "Monforte", "Lugo"),
            crearOcupacion(13L, 20L, LocalDateTime.of(2026, 8, 17, 22, 0), "Los Satélites", "Viveiro", "Lugo")
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
