package es.musicalia.gestmusica.acceso;

import es.musicalia.gestmusica.permiso.PermisoRecord;
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
