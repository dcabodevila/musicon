package es.musicalia.gestmusica.actividad;

import java.time.LocalDateTime;

public record ActividadRecord(Long id, String nombreAgencia, String nombreArtista,LocalDateTime fecha, Long numeroActualizacionesMes) {
}