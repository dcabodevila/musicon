package es.musicalia.gestmusica.permiso;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PermisoGeneralEnum {
    ACCESO_PANEL_ADMIN,
    GESTION_AGENCIAS,
    GESTION_AGRUPACION,
    AGENCIA_CREAR,
    USUARIOS;

}
