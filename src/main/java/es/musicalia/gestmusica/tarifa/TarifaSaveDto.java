package es.musicalia.gestmusica.tarifa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaSaveDto {
    private Long id;
    @NotNull
    private long idArtista;
    @NotNull
    private LocalDateTime fechaDesde;
    @NotNull
    private LocalDateTime fechaHasta;
    @NotNull
    private BigDecimal importe;

    private Boolean activo;

    public TarifaSaveDto(long idArtista, LocalDateTime fechaDesde, LocalDateTime fechaHasta, BigDecimal importe) {
        this.idArtista = idArtista;
        this.fechaDesde = fechaDesde;
        this.fechaHasta = fechaHasta;
        this.importe = importe;
        this.activo = true;

    }

    public TarifaSaveDto(long idArtista) {
        this.idArtista = idArtista;
    }

}
