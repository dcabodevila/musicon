package es.musicalia.gestmusica.permiso;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TipoPermisoEnum {
    GENERAL(0, "GENERAL"),
    AGENCIA(1, "AGENCIA"),
    ARTISTA(2, "ARTISTA");

    private final Integer id;
    private final String descripcion;

    public static String getDescripcionById(Long id) {
        return Arrays.stream(TipoPermisoEnum.values())
                .filter(tipo -> tipo.getId().equals(id))
                .map(TipoPermisoEnum::getDescripcion)
                .findFirst()
                .orElse(null);
    }
}
