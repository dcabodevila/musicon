package es.musicalia.gestmusica.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
