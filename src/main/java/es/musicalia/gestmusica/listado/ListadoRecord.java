package es.musicalia.gestmusica.listado;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ListadoRecord(Long id, String solicitadoPara,  String nombreRepresentante,String localidad, String municipio, String nombre,
                            String apellidos, String tipoOcupacion, LocalDateTime fechaCreacion,
                            LocalDate fechaPropuesta1, LocalDate fechaPropuesta2, LocalDate fechaPropuesta3,
                            LocalDate fechaPropuesta4, LocalDate fechaPropuesta5, LocalDate fechaPropuesta6,
                            LocalDate fechaPropuesta7,
                            LocalDate fechaInicio, LocalDate fechaFin) {
}