package es.musicalia.gestmusica.actividad;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class ActividadServiceImpl implements ActividadService {

    private static final List<Integer> HEATMAP_DAYS = IntStream.rangeClosed(1, 31).boxed().toList();
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Locale SPANISH_LOCALE = Locale.forLanguageTag("es-ES");

    private final TarifaRepository tarifaRepository;
    private final OcupacionRepository ocupacionRepository;
    private final ArtistaRepository artistaRepository;
    private final Clock clock;

	public ActividadServiceImpl(TarifaRepository tarifaRepository,
                              OcupacionRepository ocupacionRepository,
                              ArtistaRepository artistaRepository,
                              Clock clock){

        this.tarifaRepository = tarifaRepository;
        this.ocupacionRepository = ocupacionRepository;
        this.artistaRepository = artistaRepository;
        this.clock = clock;
    }


    @Override
    public List<ActividadRecord> findActividadTarifas() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusMonths(1);

        return this.tarifaRepository.findActividadTarifasConConteo(fechaLimite);
    }

    @Override
    public List<ActividadRecord> findActividadOcupaciones() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusMonths(1);

        return this.ocupacionRepository.findActividadOcupacionesConConteo(fechaLimite);
    }

    @Override
    public List<ActividadArtistaOptionRecord> findActiveArtistOptions() {
        return artistaRepository.findAllActiveOptionsOrderByName();
    }

    @Override
    public ActividadOcupacionesHeatmapResponse findOcupacionesHeatmap(Long artistId) {
        if (!artistaRepository.existsByIdAndActivoTrue(artistId)) {
            throw new ResponseStatusException(NOT_FOUND);
        }

        LocalDate today = LocalDate.now(clock);
        YearMonth currentMonth = YearMonth.from(today);
        List<YearMonth> months = IntStream.rangeClosed(0, 11)
            .mapToObj(index -> currentMonth.minusMonths(11L - index))
            .toList();

        LocalDate from = months.get(0).atDay(1);
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toExclusive = today.plusDays(1).atStartOfDay();

        Map<YearMonth, Map<Integer, Long>> countsByMonthAndDay = ocupacionRepository
            .findOccupationCreationHeatmapBuckets(artistId, fromDateTime, toExclusive)
            .stream()
            .collect(Collectors.groupingBy(
                bucket -> YearMonth.of(bucket.year(), bucket.month()),
                Collectors.toMap(OcupacionHeatmapBucketRecord::day, OcupacionHeatmapBucketRecord::count)
            ));

        List<ActividadHeatmapMonthRowRecord> series = months.stream()
            .map(month -> buildMonthRow(month, countsByMonthAndDay.getOrDefault(month, Map.of())))
            .toList();

        return new ActividadOcupacionesHeatmapResponse(artistId, from, today, HEATMAP_DAYS, series);
    }

    private ActividadHeatmapMonthRowRecord buildMonthRow(YearMonth month, Map<Integer, Long> monthCounts) {
        List<ActividadHeatmapCellRecord> cells = HEATMAP_DAYS.stream()
            .map(day -> new ActividadHeatmapCellRecord(day, resolveCellCount(month, day, monthCounts)))
            .toList();

        return new ActividadHeatmapMonthRowRecord(
            month.format(YEAR_MONTH_FORMATTER),
            capitalize(month.getMonth().getDisplayName(java.time.format.TextStyle.FULL, SPANISH_LOCALE)),
            cells
        );
    }

    private long resolveCellCount(YearMonth month, Integer day, Map<Integer, Long> monthCounts) {
        if (day > month.lengthOfMonth()) {
            return 0L;
        }
        return monthCounts.getOrDefault(day, 0L);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(SPANISH_LOCALE) + value.substring(1);
    }
}
