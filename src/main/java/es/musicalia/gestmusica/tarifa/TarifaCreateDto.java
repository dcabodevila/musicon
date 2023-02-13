package es.musicalia.gestmusica.tarifa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TarifaCreateDto {

    private Long idTarifa;
    private Long idArtista;
    private LocalDateTime fechaDesde;
    private LocalDateTime fechaHasta;
    private BigDecimal importe;



}
