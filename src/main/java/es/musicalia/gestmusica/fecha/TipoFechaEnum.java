package es.musicalia.gestmusica.fecha;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoFechaEnum {
    TARIFA(1L, "Tarifa"),
    OCUPACION(2L, "Ocupacion");

    private final Long id;
    private final String descripcion;
}
