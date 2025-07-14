package es.musicalia.gestmusica.ocupacion;

import java.time.LocalDateTime;

public record OcupacionListRecord(long id, LocalDateTime start, long idArtista, String artista, String importe, boolean allDay, String tipoOcupacion, String provincia, String municipio, String localidad, boolean matinal, boolean soloMatinal, String estado, long idUsuario, String nombreUsuario, Long idUsuarioConfirmacion, String usuarioConfirmacion, LocalDateTime fechaCreacion){
}
