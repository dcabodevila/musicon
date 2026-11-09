package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void findByUsername_debeResolverAlUsuarioAutenticadoPorSuId() {
        autenticar(usuario);
        when(webauthnUsuarioRepository.findByUsuarioId(7L)).thenReturn(Optional.of(webauthnUsuario("jperez")));

        assertThat(repository.findByUsername("jperez").getId()).isEqualTo(userHandle);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void findByUsername_deOtroUsuario_debeDevolverNullSinBuscarPorNombreNiEmail() {
        autenticar(usuario);

        assertThat(repository.findByUsername("juan@festia.es")).isNull();
        assertThat(repository.findByUsername("otro")).isNull();
        verifyNoInteractions(usuarioRepository, webauthnUsuarioRepository);
    }

    @Test
    void findByUsername_sinUsuarioAutenticado_debeDevolverNull() {
        assertThat(repository.findByUsername("jperez")).isNull();
        verifyNoInteractions(usuarioRepository, webauthnUsuarioRepository);
    }

    @Test
    void findByUsername_sinPasskeys_debeDevolverNull() {
        autenticar(usuario);
        when(webauthnUsuarioRepository.findByUsuarioId(7L)).thenReturn(Optional.empty());

        assertThat(repository.findByUsername("jperez")).isNull();
    }

    @Test
    void save_debeVincularAlUsuarioAutenticado() {
        autenticar(usuario);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
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
    void save_paraOtroUsuario_debeFallarSinGuardar() {
        autenticar(usuario);

        assertThatThrownBy(() -> repository.save(ImmutablePublicKeyCredentialUserEntity.builder()
                .id(userHandle).name("otro").displayName("Otro").build()))
                .isInstanceOf(IllegalStateException.class);
        verify(webauthnUsuarioRepository, never()).save(any());
    }

    @Test
    void save_sinUsuarioAutenticado_debeFallar() {
        assertThatThrownBy(() -> repository.save(ImmutablePublicKeyCredentialUserEntity.builder()
                .id(userHandle).name("jperez").displayName("Juan").build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_debeBorrarPorUserHandle() {
        repository.delete(userHandle);

        verify(webauthnUsuarioRepository).deleteById(userHandle.toBase64UrlString());
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticar(Usuario usuario) {
        usuario.setPassword("{noop}x");
        CustomAuthenticatedUser principal = new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(), Map.of(), Map.of());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
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
