package es.musicalia.gestmusica.actividad;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ActividadServiceImplTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Madrid");
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 30);

    @Mock
    private TarifaRepository tarifaRepository;

    @Mock
    private OcupacionRepository ocupacionRepository;

    @Mock
    private ArtistaRepository artistaRepository;

    private ActividadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ActividadServiceImpl(
            tarifaRepository,
            ocupacionRepository,
            artistaRepository,
            Clock.fixed(Instant.parse("2026-06-30T10:15:30Z"), ZONE_ID)
        );
    }

    @Test
    void findOcupacionesHeatmap_debeConstruirVentanaRodante12x31ConEtiquetasEnEspanol() {
        when(artistaRepository.existsByIdAndActivoTrue(7L)).thenReturn(true);
        when(ocupacionRepository.findOccupationCreationHeatmapBuckets(
            7L,
            LocalDateTime.of(2025, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 1, 0, 0)
        )).thenReturn(List.of(
            new OcupacionHeatmapBucketRecord(2025, 7, 1, 2),
            new OcupacionHeatmapBucketRecord(2026, 2, 28, 4),
            new OcupacionHeatmapBucketRecord(2026, 6, 30, 1)
        ));

        ActividadOcupacionesHeatmapResponse response = service.findOcupacionesHeatmap(7L);

        assertThat(response.artistId()).isEqualTo(7L);
        assertThat(response.from()).isEqualTo(LocalDate.of(2025, 7, 1));
        assertThat(response.to()).isEqualTo(TODAY);
        assertThat(response.days()).containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 31).boxed().toList());
        assertThat(response.series()).hasSize(12);
        assertThat(response.series().get(0).month()).isEqualTo("2025-07");
        assertThat(response.series().get(0).label()).isEqualTo("Julio");
        assertThat(response.series().get(7).label()).isEqualTo("Febrero");
        assertThat(response.series().get(11).label()).isEqualTo("Junio");
        assertThat(response.series()).allSatisfy(row -> assertThat(row.data()).hasSize(31));
        assertThat(response.series().get(0).data().get(0).count()).isEqualTo(2L);
        assertThat(response.series().get(7).data().get(27).count()).isEqualTo(4L);
        assertThat(response.series().get(11).data().get(29).count()).isEqualTo(1L);
    }

    @Test
    void findOcupacionesHeatmap_debeRellenarACeroDiasInexistentesYHuecosSinActividad() {
        when(artistaRepository.existsByIdAndActivoTrue(8L)).thenReturn(true);
        when(ocupacionRepository.findOccupationCreationHeatmapBuckets(
            8L,
            LocalDateTime.of(2025, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 1, 0, 0)
        )).thenReturn(List.of(
            new OcupacionHeatmapBucketRecord(2026, 4, 30, 3),
            new OcupacionHeatmapBucketRecord(2026, 6, 15, 5)
        ));

        ActividadOcupacionesHeatmapResponse response = service.findOcupacionesHeatmap(8L);

        ActividadHeatmapMonthRowRecord february = response.series().get(7);
        ActividadHeatmapMonthRowRecord april = response.series().get(9);
        ActividadHeatmapMonthRowRecord june = response.series().get(11);

        assertThat(february.data().get(28).count()).isZero();
        assertThat(february.data().get(29).count()).isZero();
        assertThat(february.data().get(30).count()).isZero();
        assertThat(april.data().get(29).count()).isEqualTo(3L);
        assertThat(april.data().get(30).count()).isZero();
        assertThat(june.data().get(14).count()).isEqualTo(5L);
        assertThat(june.data().get(0).count()).isZero();
    }

    @Test
    void findOcupacionesHeatmap_debeMantenerMatrizBlancaCuandoNoHayHistorial() {
        when(artistaRepository.existsByIdAndActivoTrue(9L)).thenReturn(true);
        when(ocupacionRepository.findOccupationCreationHeatmapBuckets(
            9L,
            LocalDateTime.of(2025, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 1, 0, 0)
        )).thenReturn(List.of());

        ActividadOcupacionesHeatmapResponse response = service.findOcupacionesHeatmap(9L);

        assertThat(response.series())
            .allSatisfy(row -> assertThat(row.data()).allSatisfy(cell -> assertThat(cell.count()).isZero()));
    }

    @Test
    void findOcupacionesHeatmap_debeUsarSoloBucketsPorFechaCreacion() {
        when(artistaRepository.existsByIdAndActivoTrue(10L)).thenReturn(true);
        when(ocupacionRepository.findOccupationCreationHeatmapBuckets(
            10L,
            LocalDateTime.of(2025, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 1, 0, 0)
        )).thenReturn(List.of(
            new OcupacionHeatmapBucketRecord(2026, 3, 10, 1)
        ));

        ActividadOcupacionesHeatmapResponse response = service.findOcupacionesHeatmap(10L);

        ActividadHeatmapMonthRowRecord march = response.series().get(8);
        assertThat(march.data().get(9).count()).isEqualTo(1L);
        assertThat(march.data().get(10).count()).isZero();
    }

    @Test
    void findOcupacionesHeatmap_debeResponder404SiArtistaNoEstaActivo() {
        when(artistaRepository.existsByIdAndActivoTrue(99L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.findOcupacionesHeatmap(99L));

        assertThat(exception.getStatusCode()).isEqualTo(NOT_FOUND);
        verifyNoInteractions(ocupacionRepository);
    }
}
