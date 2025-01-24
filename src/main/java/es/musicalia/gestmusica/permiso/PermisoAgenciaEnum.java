package es.musicalia.gestmusica.permiso;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PermisoAgenciaEnum {
    AGENCIA_EDITAR("AGENCIA_EDITAR"),
    ARTISTAS_EDITAR( "ARTISTAS_EDITAR"),
    CONFIRMAR_OCUPACION( "CONFIRMAR_OCUPACION"),
    ANULAR_OCUPACION( "ANULAR_OCUPACION"),
    TARIFA_ANUAL( "TARIFA_ANUAL"),
    GESTION_ACCESOS( "GESTION_ACCESOS"),
    GESTION_TARIFAS( "GESTION_TARIFAS"),
    OCUPACIONES( "OCUPACIONES");

    private final String descripcion;


}
