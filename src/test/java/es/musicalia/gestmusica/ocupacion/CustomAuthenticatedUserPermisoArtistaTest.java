package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticatedUserPermisoArtistaTest {

    @Test
    void hasPermisoArtista_artistaConPermiso_devuelveTrue() {
        CustomAuthenticatedUser user = authenticatedUser(Map.of(1L, Set.of(PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name())));

        assertThat(user.hasPermisoArtista(1L, PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name())).isTrue();
    }

    @Test
    void hasPermisoArtista_artistaSinPermiso_devuelveFalse() {
        CustomAuthenticatedUser user = authenticatedUser(Map.of(1L, Set.of("OCUPACIONES")));

        assertThat(user.hasPermisoArtista(1L, PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name())).isFalse();
    }

    @Test
    void hasPermisoArtista_artistaFueraDelMapa_devuelveFalse() {
        CustomAuthenticatedUser user = authenticatedUser(Map.of(1L, Set.of(PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name())));

        assertThat(user.hasPermisoArtista(2L, PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name())).isFalse();
    }

    @Test
    void hasPermisoEnAlgunArtista_algunArtistaConPermiso_devuelveTrue() {
        CustomAuthenticatedUser user = authenticatedUser(Map.of(
                1L, Set.of("OCUPACIONES"),
                2L, Set.of(PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name())));

        assertThat(user.hasPermisoEnAlgunArtista(PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name())).isTrue();
    }

    @Test
    void hasPermisoEnAlgunArtista_ningunArtistaConPermiso_devuelveFalse() {
        CustomAuthenticatedUser user = authenticatedUser(Map.of(1L, Set.of("OCUPACIONES")));

        assertThat(user.hasPermisoEnAlgunArtista(PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name())).isFalse();
    }

    private CustomAuthenticatedUser authenticatedUser(Map<Long, Set<String>> mapPermisosArtista) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Usuario");
        usuario.setApellidos("Prueba");
        usuario.setPassword("secret");

        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(), mapPermisosArtista, Map.of());
    }
}
