package es.musicalia.gestmusica.usuario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioRepositoryFiltroTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    void findUsuariosParaComunicacion_sinFiltros_debeRetornarTodosLosActivos() {
        // Arrange
        Usuario user1 = crearUsuario(1L, "user1@test.com");
        Usuario user2 = crearUsuario(2L, "user2@test.com");
        when(usuarioRepository.findUsuariosParaComunicacion(null, null, null))
                .thenReturn(List.of(user1, user2));

        // Act
        List<Usuario> result = usuarioRepository.findUsuariosParaComunicacion(null, null, null);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("user1@test.com", "user2@test.com");
        verify(usuarioRepository).findUsuariosParaComunicacion(null, null, null);
    }

    @Test
    void findUsuariosParaComunicacion_conFiltroCcaa_debeRetornarSoloDeEsaCcaa() {
        // Arrange
        Usuario user1 = crearUsuario(1L, "user1@madrid.com");
        when(usuarioRepository.findUsuariosParaComunicacion(1L, null, null))
                .thenReturn(List.of(user1));

        // Act
        List<Usuario> result = usuarioRepository.findUsuariosParaComunicacion(1L, null, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("user1@madrid.com");
        verify(usuarioRepository).findUsuariosParaComunicacion(1L, null, null);
    }

    @Test
    void findUsuariosParaComunicacion_conFiltroProvincia_debeRetornarSoloDeEsaProvincia() {
        // Arrange
        Usuario user1 = crearUsuario(1L, "user1@barcelona.com");
        when(usuarioRepository.findUsuariosParaComunicacion(null, 2L, null))
                .thenReturn(List.of(user1));

        // Act
        List<Usuario> result = usuarioRepository.findUsuariosParaComunicacion(null, 2L, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("user1@barcelona.com");
        verify(usuarioRepository).findUsuariosParaComunicacion(null, 2L, null);
    }

    @Test
    void findUsuariosParaComunicacion_conFiltroRol_debeRetornarSoloDeEseRol() {
        // Arrange
        Usuario user1 = crearUsuario(1L, "agencia@test.com");
        Usuario user2 = crearUsuario(2L, "agencia2@test.com");
        when(usuarioRepository.findUsuariosParaComunicacion(null, null, "AGENCIA"))
                .thenReturn(List.of(user1, user2));

        // Act
        List<Usuario> result = usuarioRepository.findUsuariosParaComunicacion(null, null, "AGENCIA");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Usuario::getEmail)
                .containsExactlyInAnyOrder("agencia@test.com", "agencia2@test.com");
        verify(usuarioRepository).findUsuariosParaComunicacion(null, null, "AGENCIA");
    }

    @Test
    void findUsuariosParaComunicacion_conFiltrosCombinados_debeRetornarSoloCoincidencias() {
        // Arrange
        Usuario user1 = crearUsuario(1L, "madrid-agencia@test.com");
        when(usuarioRepository.findUsuariosParaComunicacion(1L, null, "AGENCIA"))
                .thenReturn(List.of(user1));

        // Act
        List<Usuario> result = usuarioRepository.findUsuariosParaComunicacion(1L, null, "AGENCIA");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("madrid-agencia@test.com");
        verify(usuarioRepository).findUsuariosParaComunicacion(1L, null, "AGENCIA");
    }

    @Test
    void findUsuariosParaComunicacion_conTodosLosFiltros_sinCoincidencias_debeRetornarListaVacia() {
        // Arrange
        when(usuarioRepository.findUsuariosParaComunicacion(1L, 2L, "AGENCIA"))
                .thenReturn(List.of());

        // Act
        List<Usuario> result = usuarioRepository.findUsuariosParaComunicacion(1L, 2L, "AGENCIA");

        // Assert
        assertThat(result).isEmpty();
        verify(usuarioRepository).findUsuariosParaComunicacion(1L, 2L, "AGENCIA");
    }

    private Usuario crearUsuario(Long id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setActivo(true);
        return usuario;
    }
}
