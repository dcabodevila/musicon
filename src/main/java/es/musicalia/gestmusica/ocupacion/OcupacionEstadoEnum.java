package es.musicalia.gestmusica.ocupacion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OcupacionEstadoEnum {
    OCUPADO(1L, "Ocupado"),
    RESERVADO(2L, "Reservado"),
    PENDIENTE(3L, "Pendiente"),
    ANULADO(4L, "Anulado");

    private final Long id;
    private final String descripcion;
}
