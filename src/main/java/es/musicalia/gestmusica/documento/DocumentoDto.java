package es.musicalia.gestmusica.documento;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentoDto {
    private Long id;

    private String nombre;

    private String url;

    @NotNull(message = "El ID del artista es requerido")
    private Long idArtista;


}
