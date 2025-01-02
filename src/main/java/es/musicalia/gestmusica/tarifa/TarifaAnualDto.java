package es.musicalia.gestmusica.tarifa;

import lombok.*;

import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaAnualDto implements Serializable {


    private Long idArtista;
    private Long idProvincia;
    private Integer ano;



}
