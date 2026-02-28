package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.listado.TipoTarifaEnum;
import lombok.*;

import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaAnualDto implements Serializable {


    private Long idArtista;
    private Long idProvincia;
    private Integer ano;
    private Boolean conOcupacion;
    private TipoTarifaEnum tipoTarifa;



}
