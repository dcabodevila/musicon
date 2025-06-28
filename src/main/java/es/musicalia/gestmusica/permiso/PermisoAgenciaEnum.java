package es.musicalia.gestmusica.permiso;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PermisoAgenciaEnum {
    GESTION_ACCESOS,
    CONFIRMAR_OCUPACION,
    MODIFICAR_OCUPACION_OTROS,
    AGENCIA_EDITAR,
    ARTISTA_CREAR,
    VER_DATOS_ECONOMICOS,
    VER_DATOS_ACTUACION;

}
