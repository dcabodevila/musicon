package es.musicalia.gestmusica.listado;

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
public class ListadoAudienciasDto {
    private Long idAgencia;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fechaDesde;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fechaHasta;

}
