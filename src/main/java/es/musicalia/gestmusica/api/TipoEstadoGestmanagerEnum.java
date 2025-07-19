package es.musicalia.gestmusica.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoEstadoGestmanagerEnum {
    OCUPADO("O"),
    RESERVADO("R");

    private final String codigo;
}
