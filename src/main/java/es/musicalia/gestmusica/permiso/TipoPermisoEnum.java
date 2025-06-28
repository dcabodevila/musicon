package es.musicalia.gestmusica.permiso;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoPermisoEnum {
    GENERAL(0, "GENERAL"),
    AGENCIA(1, "AGENCIA"),
    ARTISTA(2, "ARTISTA");

    private final Integer id;
    private final String descripcion;

}
