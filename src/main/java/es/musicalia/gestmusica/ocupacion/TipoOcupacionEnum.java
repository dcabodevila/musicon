package es.musicalia.gestmusica.ocupacion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoOcupacionEnum {
    OCUPADO(1L, "Ocupado"),
    RESERVADO(2L, "Reservado"),
    OTRO(3L, "Otro");

    private final Long id;
    private final String descripcion;


    public static TipoOcupacionEnum findByDescripcion(String descripcion) {
        for (TipoOcupacionEnum tipo : values()) {
            if (tipo.getDescripcion().equalsIgnoreCase(descripcion)) {
                return tipo;
            }
        }
        return OCUPADO;
    }

}
