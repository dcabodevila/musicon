package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasskeyUserEntityRepositoryTest {

    private final WebauthnUsuarioRepository webauthnUsuarioRepository = mock(WebauthnUsuarioRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private PasskeyUserEntityRepository repository;

    private final Bytes userHandle = Bytes.random();
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        repository = new PasskeyUserEntityRepository(webauthnUsuarioRepository, usuarioRepository);
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("jperez");
        usuario.setEmail("juan@festia.es");
        usuario.setNombre("Juan");
    }

    @Test
    void findById_debeDevolverElUsernameActualDelUsuario() {
        WebauthnUsuario guardado = webauthnUsuario("nombre-antiguo");
        when(webauthnUsuarioRepository.findById(userHandle.toBase64UrlString())).thenReturn(Optional.of(guardado));

        PublicKeyCredentialUserEntity entity = repository.findById(userHandle);

        assertThat(entity.getId()).isEqualTo(userHandle);
        assertThat(entity.getName()).isEqualTo("jperez");
        assertThat(entity.getDisplayName()).isEqualTo("Juan Pérez");
    }

    @Test
    void findById_desconocido_debeDevolverNull() {
        when(webauthnUsuarioRepository.findById(userHandle.toBase64UrlString())).thenReturn(Optional.empty());

        assertThat(repository.findById(userHandle)).isNull();
    }

    @Test
    void findByUsername_debeResolverElUsuarioPorUsername() {
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(usuario));
        when(webauthnUsuarioRepository.findByUsuarioId(7L)).thenReturn(Optional.of(webauthnUsuario("jperez")));

        assertThat(repository.findByUsername("jperez").getId()).isEqualTo(userHandle);
    }

    @Test
    void findByUsername_conEmail_debeResolverElUsuarioActivoPorEmail() {
        when(usuarioRepository.findUsuarioActivoByMail("juan@festia.es")).thenReturn(Optional.of(usuario));
        when(webauthnUsuarioRepository.findByUsuarioId(7L)).thenReturn(Optional.of(webauthnUsuario("juan@festia.es")));

        assertThat(repository.findByUsername("juan@festia.es").getId()).isEqualTo(userHandle);
    }

    @Test
    void findByUsername_sinPasskeys_debeDevolverNull() {
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(usuario));
        when(webauthnUsuarioRepository.findByUsuarioId(7L)).thenReturn(Optional.empty());

        assertThat(repository.findByUsername("jperez")).isNull();
    }

    @Test
    void save_debeVincularElUsuarioDeLaAplicacion() {
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(usuario));
        when(webauthnUsuarioRepository.findById(userHandle.toBase64UrlString())).thenReturn(Optional.empty());

        repository.save(ImmutablePublicKeyCredentialUserEntity.builder()
                .id(userHandle).name("jperez").displayName("Juan Pérez").build());

        ArgumentCaptor<WebauthnUsuario> captor = ArgumentCaptor.forClass(WebauthnUsuario.class);
        verify(webauthnUsuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(userHandle.toBase64UrlString());
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(captor.getValue().getNombre()).isEqualTo("jperez");
        assertThat(captor.getValue().getNombreVisible()).isEqualTo("Juan Pérez");
    }

    @Test
    void save_conUsuarioInexistente_debeFallar() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.save(ImmutablePublicKeyCredentialUserEntity.builder()
                .id(userHandle).name("nadie").displayName("Nadie").build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_debeBorrarPorUserHandle() {
        repository.delete(userHandle);

        verify(webauthnUsuarioRepository).deleteById(userHandle.toBase64UrlString());
    }

    private WebauthnUsuario webauthnUsuario(String nombre) {
        WebauthnUsuario w = new WebauthnUsuario();
        w.setId(userHandle.toBase64UrlString());
        w.setUsuario(usuario);
        w.setNombre(nombre);
        w.setNombreVisible("Juan Pérez");
        return w;
    }
}
