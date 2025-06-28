package es.musicalia.gestmusica.rol;

import lombok.Getter;

@Getter
public enum RolEnum {

    ROL_ADMINISTRADOR("ADMIN"),
    ROL_REPRESENTANTE( "REPRE"),
    ROL_AGENTE("AGENTE"),
    ROL_AGENCIA( "AGENCIA"),
    ROL_ARTISTA( "ARTISTA");

    private String codigo;

    RolEnum (String codigo) {
        this.codigo = codigo;
    }


}
