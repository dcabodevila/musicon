package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OcupacionMostrarImportesTest {

    private final OcupacionController controller = new OcupacionController(
            null, null, null, null, null, null);

    @Test
    void toDataTableRow_mostrarImportesTrueYConPermiso_incluyeImporte() {
        OcupacionListRecord ocupacion = crearOcupacion("1500");
        CustomAuthenticatedUser user = usuarioConPermiso(1L);

        Map<String, Object> row = controller.toDataTableRow(ocupacion, true, user);

        assertThat(row.get("importe")).isEqualTo("1500");
    }

    @Test
    void toDataTableRow_mostrarImportesFalse_noIncluyeImporteAunqueTengaPermiso() {
        OcupacionListRecord ocupacion = crearOcupacion("1500");
        CustomAuthenticatedUser user = usuarioConPermiso(1L);

        Map<String, Object> row = controller.toDataTableRow(ocupacion, false, user);

        assertThat(row.get("importe")).isEqualTo("");
    }

    @Test
    void toDataTableRow_mostrarImportesTrueSinPermisoEnElArtista_noIncluyeImporte() {
        OcupacionListRecord ocupacion = crearOcupacion("1500");
        CustomAuthenticatedUser user = usuarioSinPermiso();

        Map<String, Object> row = controller.toDataTableRow(ocupacion, true, user);

        assertThat(row.get("importe")).isEqualTo("");
    }

    private OcupacionListRecord crearOcupacion(String importe) {
        return new OcupacionListRecord(
                1L,
                LocalDateTime.now(),
                1L,
                "Artista",
                importe,
                true,
                "Boda",
                "Provincia",
                "Municipio",
                "Localidad",
                false,
                false,
                "Confirmado",
                1L,
                "Usuario",
                null,
                null,
                LocalDateTime.now(),
                null,
                null,
                null,
                null);
    }

    private CustomAuthenticatedUser usuarioConPermiso(long idArtista) {
        return authenticatedUser(Map.of(idArtista, Set.of(PermisoAgenciaEnum.VER_DATOS_ECONOMICOS.name())));
    }

    private CustomAuthenticatedUser usuarioSinPermiso() {
        return authenticatedUser(Map.of(1L, Set.of("OCUPACIONES")));
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
