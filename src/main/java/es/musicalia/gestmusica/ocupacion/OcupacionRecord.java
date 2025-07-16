package es.musicalia.gestmusica.ocupacion;

import java.time.LocalDateTime;

public record OcupacionRecord(long id, LocalDateTime start, long idArtista, String artista, String importe, boolean allDay, String tipoOcupacion, String provincia, String municipio, String localidad, boolean matinal, boolean soloMatinal, String estado, long idUsuario, String nombreUsuario){
}
