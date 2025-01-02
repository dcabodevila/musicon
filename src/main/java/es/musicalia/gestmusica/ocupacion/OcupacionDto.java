package es.musicalia.gestmusica.ocupacion;

import java.time.LocalDateTime;

public record OcupacionDto(long id, LocalDateTime start, long idArtista, String importe, boolean allDay, String tipoOcupacion, String provincia, String municipio,String localidad, boolean matinal, boolean soloMatinal, String estado){
}
