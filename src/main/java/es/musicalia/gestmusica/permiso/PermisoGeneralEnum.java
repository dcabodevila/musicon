package es.musicalia.gestmusica.permiso;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PermisoGeneralEnum {
    ACCESO_PANEL_ADMIN(1, "ACCESO_PANEL_ADMIN"),
    GESTION_AGENCIAS(2, "GESTION_AGENCIAS"),
    GESTION_AGRUPACION(3, "GESTION_AGRUPACION");

    private final Integer id;
    private final String descripcion;

}
