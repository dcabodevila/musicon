// AccesoPorDiaDTO.java
package es.musicalia.gestmusica.registrologin;

import java.io.Serializable;

public record AccesoPorDiaDTO(
    String fecha,
    String dia,
    Long cantidad
) implements Serializable {}
