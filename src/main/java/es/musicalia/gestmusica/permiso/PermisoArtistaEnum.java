package es.musicalia.gestmusica.permiso;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermisoArtistaEnum {

    VER_TARIFAS,
    ARTISTAS_EDITAR,
    ANULAR_OCUPACION,
    OCUPACIONES,
    TARIFA_ANUAL,
    TARIFA_ANUAL_CON_OCUPACION,
    LISTADOS_CON_OCUPACION_PERMITIR,
    LISTADOS_SIN_OCUPACION_PERMITIR,
    CREAR_TARIFAS,
    RESERVAR_OCUPACION;



}
