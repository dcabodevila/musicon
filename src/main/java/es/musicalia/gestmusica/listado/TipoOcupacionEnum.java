package es.musicalia.gestmusica.listado;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TipoOcupacionEnum {
    CON_OCUPACION(1L, "Con ocupación"),
    SIN_OCUPACION(2L, "Sin ocupación");

    private final Long id;
    private final String descripcion;

    public static String getDescripcionById(Long id) {
        return Arrays.stream(TipoOcupacionEnum.values())
                .filter(tipo -> tipo.getId().equals(id))
                .map(TipoOcupacionEnum::getDescripcion)
                .findFirst()
                .orElse(null);
    }

    public static TipoOcupacionEnum getById(Long id) {
        return Arrays.stream(TipoOcupacionEnum.values())
                .filter(tipo -> tipo.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
