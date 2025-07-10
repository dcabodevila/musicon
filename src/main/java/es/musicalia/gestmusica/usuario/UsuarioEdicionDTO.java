package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.acceso.Acceso;
import es.musicalia.gestmusica.acceso.AccesoDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEdicionDTO {

    @NotNull
    private Long id;
    @NotNull
    private String username;
    @NotNull
    private String nombre;
    private String apellidos;
    @NotNull
    @Email
    private String email;
    private String imagen;

}
