package es.musicalia.gestmusica.tarifa;

import java.time.LocalDateTime;

public record TarifaDto(long id, LocalDateTime start, long idArtista, String title, boolean allDay){
}
