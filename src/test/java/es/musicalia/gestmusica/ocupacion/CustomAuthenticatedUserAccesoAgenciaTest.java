package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticatedUserAccesoAgenciaTest {

    @Test
    void hasAccesoAgencia_idAgenciaNulo_devuelveTrue() {
        CustomAuthenticatedUser user = authenticatedUser(Map.of(10L, Set.of("VER_OCUPACIONES")));

        assertThat(user.hasAccesoAgencia(null)).isTrue();
    }

    @Test
    void hasAccesoAgencia_agenciaEnMapaPermisos_devuelveTrue() {
        CustomAuthenticatedUser user = authenticatedUser(Map.of(10L, Set.of("VER_OCUPACIONES")));

        assertThat(user.hasAccesoAgencia(10L)).isTrue();
    }

    @Test
    void hasAccesoAgencia_agenciaFueraDelMapaPermisos_devuelveFalse() {
        CustomAuthenticatedUser user = authenticatedUser(Map.of(10L, Set.of("VER_OCUPACIONES")));

        assertThat(user.hasAccesoAgencia(20L)).isFalse();
    }

    @Test
    void hasAccesoAgencia_sinAgencias_devuelveFalseParaIdConcreto() {
        CustomAuthenticatedUser user = authenticatedUser(Map.of());

        assertThat(user.hasAccesoAgencia(10L)).isFalse();
    }

    private CustomAuthenticatedUser authenticatedUser(Map<Long, Set<String>> mapPermisosAgencia) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Usuario");
        usuario.setApellidos("Prueba");
        usuario.setPassword("secret");

        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(), Map.of(), mapPermisosAgencia);
    }
}
