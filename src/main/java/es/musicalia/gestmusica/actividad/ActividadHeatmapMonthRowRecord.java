package es.musicalia.gestmusica.actividad;

import java.util.List;

public record ActividadHeatmapMonthRowRecord(String month, String label, List<ActividadHeatmapCellRecord> data) {
}
