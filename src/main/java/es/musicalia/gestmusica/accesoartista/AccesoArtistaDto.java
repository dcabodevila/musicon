package es.musicalia.gestmusica.accesoartista;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

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
