package es.musicalia.gestmusica.listado;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListadosPorMesDto {
    private String mes;
    private Long cantidad;

    public ListadosPorMesDto(String mes, Long cantidad) {
        this.mes = mes;
        this.cantidad = cantidad;
    }

}