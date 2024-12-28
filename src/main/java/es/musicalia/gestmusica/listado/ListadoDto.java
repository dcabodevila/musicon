package es.musicalia.gestmusica.listado;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListadoDto {
    @NotNull
    private String solicitadoPara;
    @NotNull
    private Long idCcaa;
    @NotNull
    private Long idProvincia;
    @NotNull
    private Long idMunicipio;
    @NotNull
    private String localidad;
    private String comentario;
    private Long idTipoOcupacion;
    @NotNull
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fechaDesde;
    @NotNull
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fechaHasta;

}
