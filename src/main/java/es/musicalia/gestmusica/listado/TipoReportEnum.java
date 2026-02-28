package es.musicalia.gestmusica.listado;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoReportEnum {
    LISTADO_SIN_OCUPACION_HORIZONTAL( "listado_sin_ocupacion2.jrxml", "Presupuesto sin ocupación"),
    LISTADO_CON_OCUPACION_HORIZONTAL( "listado_con_ocupacion.jrxml", "Presupuesto con ocupación"),
    LISTADO_SIN_OCUPACION_VERTICAL( "listado_sin_ocupacion_vertical2.jrxml", "Presupuesto sin ocupación"),
    LISTADO_CON_OCUPACION_VERTICAL("listado_con_ocupacion_vertical.jrxml", "Presupuesto con ocupación"),
    TARIFA_CON_OCUPACION_HORIZONTAL( "tarifa_anual_horizontal_ocupacion.jrxml", "Tarifa anual"),
    TARIFA_CON_OCUPACION_HORIZONTAL_8MESES( "tarifa_anual_horizontal_ocupacion_8meses.jrxml", "Tarifa anual");

    private final String nombreFicheroReport;
    private final String titulo;

}
