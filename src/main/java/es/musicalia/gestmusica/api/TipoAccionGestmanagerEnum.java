package es.musicalia.gestmusica.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoAccionGestmanagerEnum {
    ALTA("ALTA"),
    MODIFICACION("MODIFICAR"),
    BAJA("DELETE"),
    MOD_AGR("MOD_AGR"),
    MOD_FECHA_AGR("MOD_FECHA_AGR"),
    MOD_FECHA("MOD_FECHA");

    private final String codigo;

    public static TipoAccionGestmanagerEnum getTipoAccionByCodigo(String codigo) {
        return codigo == null ? null :
                java.util.stream.Stream.of(TipoAccionGestmanagerEnum.values())
                        .filter(tipo -> tipo.getCodigo().equals(codigo))
                        .findFirst()
                        .orElse(null);
    }

}
