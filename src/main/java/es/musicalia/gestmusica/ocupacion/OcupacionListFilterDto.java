package es.musicalia.gestmusica.ocupacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcupacionListFilterDto {

    private Long idAgencia;

    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate fechaDesde;
}
