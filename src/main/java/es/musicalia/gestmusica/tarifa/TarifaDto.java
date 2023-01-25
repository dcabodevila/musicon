package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.artista.Artista;
import jakarta.persistence.*;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Date;

public record TarifaDto(long id, Date fecha, long idArtista, BigDecimal importe){
}
