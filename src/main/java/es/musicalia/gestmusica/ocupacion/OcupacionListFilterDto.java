package es.musicalia.gestmusica.ocupacion;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcupacionListFilterDto {

    private Long idAgencia;
    private Long idArtista;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    @NotNull
    private LocalDate fechaDesde;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fechaHasta;

}
