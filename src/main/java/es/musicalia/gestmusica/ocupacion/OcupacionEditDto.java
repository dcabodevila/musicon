package es.musicalia.gestmusica.ocupacion;

import java.time.LocalDateTime;

public record OcupacionEditDto(long id, LocalDateTime start, long idArtista, String importe, Long idTipoOcupacion, String tipoOcupacion, Long idCcaa, Long idProvincia, String provincia, Long idMunicipio, String municipio, String localidad, String lugar, boolean matinal, String estado, String observaciones){
}