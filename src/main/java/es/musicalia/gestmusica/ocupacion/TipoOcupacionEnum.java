package es.musicalia.gestmusica.ocupacion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoOcupacionEnum {
    OCUPADO(1L, "Ocupado"),
    RESERVADO(2L, "Reservado");

    private final Long id;
    private final String descripcion;
}
