package es.musicalia.gestmusica.rol;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TipoRolEnum {
    GENERAL(0, "GENERAL"),
    AGENCIA(1, "AGENCIA"),
    ARTISTA(2, "ARTISTA"),
    ADMIN(2, "ADMIN");


    private final Integer id;
    private final String descripcion;

    public static String getDescripcionById(Long id) {
        return Arrays.stream(TipoRolEnum.values())
                .filter(tipo -> tipo.getId().equals(id))
                .map(TipoRolEnum::getDescripcion)
                .findFirst()
                .orElse(null);
    }
}
