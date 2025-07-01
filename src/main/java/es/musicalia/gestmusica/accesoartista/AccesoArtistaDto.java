package es.musicalia.gestmusica.accesoartista;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccesoArtistaDto {

    private Long id;
    @NotNull
    private Long idUsuario;
    private String nombreUsuario;
    @NotNull
    private Long idArtista;
    private String artista;
    @NotNull
    private Long idPermiso;
    private String permiso;


}
