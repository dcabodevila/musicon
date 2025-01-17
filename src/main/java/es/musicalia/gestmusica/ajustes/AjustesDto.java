package es.musicalia.gestmusica.ajustes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjustesDto {

    private Long id;
    private List<Long> idsTipoArtista = new ArrayList<>();
    private List<Long> idsAgencias = new ArrayList<>();
    private List<Long> idsComunidades = new ArrayList<>();

}
