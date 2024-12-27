package es.musicalia.gestmusica.ocupacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcupacionSaveDto {
    private Long id;
    @NotNull
    private Long idArtista;

    private LocalDateTime fecha;
    @NotNull
    private Long idTipoOcupacion;
    @NotNull
    private Long idCcaa;
    @NotNull
    private Long idProvincia;
    @NotNull
    private Long idMunicipio;
    @NotNull
    private String localidad;
    private String lugar;
    @NotNull
    private BigDecimal importe;
    private String observaciones;

    private Boolean matinal= Boolean.FALSE;

    private Boolean activo = Boolean.TRUE;

}
