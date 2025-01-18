package es.musicalia.gestmusica.permiso;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PermisoGeneralEnum {
    ACCESO_PANEL_ADMIN(1, "ACCESO_PANEL_ADMIN"),
    AGENCIA(1, "AGENCIA"),
    ARTISTA(2, "ARTISTA");

    private final Integer id;
    private final String descripcion;

    public static String getDescripcionById(Long id) {
        return Arrays.stream(PermisoGeneralEnum.values())
                .filter(tipo -> tipo.getId().equals(id))
                .map(PermisoGeneralEnum::getDescripcion)
                .findFirst()
                .orElse(null);
    }
}
