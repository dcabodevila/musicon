package es.musicalia.gestmusica.permiso;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermisoArtistaEnum {
    LISTADOS_CON_OCUPACION_PERMITIR("LISTADOS_CON_OCUPACION_PERMITIR"),
    LISTADOS_CON_OCUPACION_PROHIBIR( "LISTADOS_CON_OCUPACION_PROHIBIR"),
    LISTADOS_SIN_OCUPACION_PERMITIR( "LISTADOS_SIN_OCUPACION_PERMITIR"),
    LISTADOS_SIN_OCUPACION_PROHIBIR( "LISTADOS_SIN_OCUPACION_PROHIBIR");

    private final String descripcion;


}
