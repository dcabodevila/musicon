package es.musicalia.gestmusica.actividad;

import java.time.LocalDate;
import java.util.List;

public record ActividadOcupacionesHeatmapResponse(
    Long artistId,
    LocalDate from,
    LocalDate to,
    List<Integer> days,
    List<ActividadHeatmapMonthRowRecord> series
) {
}
