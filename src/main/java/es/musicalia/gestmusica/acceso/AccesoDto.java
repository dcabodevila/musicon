package es.musicalia.gestmusica.acceso;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccesoDto {

    private Long id;
    @NotNull
    private Long idUsuario;
    private String nombreUsuario;
    @NotNull
    private Long idAgencia;
    private String agencia;
    @NotNull
    private Long idRol;
    private String rol;


}
