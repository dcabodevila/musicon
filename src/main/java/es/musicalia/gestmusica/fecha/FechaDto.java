package es.musicalia.gestmusica.fecha;

import java.time.LocalDateTime;

public record FechaDto(long id, LocalDateTime start, long idArtista, String title, boolean allDay, String tipoFecha, String tipoOcupacion, String tooltip, boolean matinal, String estado){
}
