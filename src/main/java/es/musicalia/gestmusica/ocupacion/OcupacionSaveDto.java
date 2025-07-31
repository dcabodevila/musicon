package es.musicalia.gestmusica.ocupacion;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcupacionSaveDto {
    private Long id;
    private Long idAgencia;
    @NotNull
    private Long idArtista;
    private String estado;
    private LocalDateTime fecha;
    @NotNull
    private Long idTipoOcupacion;
    @NotNull
    private Long idCcaa;
    @NotNull
    private Long idProvincia;
    private Long idMunicipio;
    @NotNull
    private String localidad;
    private String lugar;
    @NotNull
    private BigDecimal importe;
    @NotNull
    private BigDecimal porcentajeRepre;
    @NotNull
    private BigDecimal iva;

    private String observaciones;

    private Boolean matinal= Boolean.FALSE;

    private Boolean soloMatinal= Boolean.FALSE;

    private Boolean activo = Boolean.TRUE;

    private Boolean provisional;

    private String textoOrquestasDeGalicia;

    private Integer idOcupacionLegacy;

}
