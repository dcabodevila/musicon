package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticatedUserTest {

    @Test
    void username_debeSerElUsernameDeLogin_yNombreCompletoElNombreVisible() {
        CustomAuthenticatedUser user = autenticado(usuario("jperez", "juan@festia.es", "Juan", "Pérez"));

        assertThat(user.getUsername()).isEqualTo("jperez");
        assertThat(user.getNombreCompleto()).isEqualTo("Juan Pérez");
    }

    @Test
    void username_sinUsername_debeUsarElEmail() {
        CustomAuthenticatedUser user = autenticado(usuario(null, "juan@festia.es", "Juan", null));

        assertThat(user.getUsername()).isEqualTo("juan@festia.es");
        assertThat(user.getNombreCompleto()).isEqualTo("Juan");
    }

    @Test
    void usuariosConElMismoNombreCompleto_noDebenSerElMismoPrincipal() {
        CustomAuthenticatedUser juan1 = autenticado(usuario("jperez", "juan@festia.es", "Juan", "Pérez"));
        CustomAuthenticatedUser juan2 = autenticado(usuario("jperez2", "otro@festia.es", "Juan", "Pérez"));

        assertThat(juan1).isNotEqualTo(juan2);
    }

    private static Usuario usuario(String username, String email, String nombre, String apellidos) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos);
        usuario.setPassword("{noop}secret");
        return usuario;
    }

    private static CustomAuthenticatedUser autenticado(Usuario usuario) {
        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(), Map.of(), Map.of());
    }
}
