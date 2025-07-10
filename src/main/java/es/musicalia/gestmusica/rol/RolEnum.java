package es.musicalia.gestmusica.rol;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum RolEnum {

    ROL_ADMINISTRADOR("ADMIN", "Administrador"),
    ROL_REPRESENTANTE( "REPRE", "Representante"),
    ROL_AGENTE("AGENTE", "Agente"),
    ROL_AGENCIA( "AGENCIA", "Agencia"),
    ROL_ARTISTA( "ARTISTA", "Artista");

    private String codigo;
    private String descripcion;

    RolEnum (String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }


    public static Map<String, String> getRoleEnumMap() {
        Map<String, String> roleMap = new HashMap<>();
        for (RolEnum rol : RolEnum.values()) {
            roleMap.put(rol.getCodigo(), rol.getDescripcion());
        }
        return roleMap;
    }
}
