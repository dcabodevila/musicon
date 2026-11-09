package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasskeyServiceTest {

    private final WebauthnUsuarioRepository webauthnUsuarioRepository = mock(WebauthnUsuarioRepository.class);
    private final WebauthnCredencialRepository credencialRepository = mock(WebauthnCredencialRepository.class);
    private PasskeyService service;

    private WebauthnUsuario deJuan;
    private WebauthnUsuario deAna;

    @BeforeEach
    void setUp() {
        service = new PasskeyService(webauthnUsuarioRepository, credencialRepository);
        deJuan = webauthnUsuario("handle-juan", 7L);
        deAna = webauthnUsuario("handle-ana", 8L);
    }

    @Test
    void listarPasskeys_debeDevolverLasDelUsuarioConEtiquetaYFechas() {
        Instant creada = Instant.parse("2026-11-09T10:00:00Z");
        Instant usada = Instant.parse("2026-11-10T08:00:00Z");
        when(webauthnUsuarioRepository.findByUsuarioId(7L)).thenReturn(Optional.of(deJuan));
        when(credencialRepository.findByWebauthnUsuarioIdOrderByFechaCreacionAsc("handle-juan"))
                .thenReturn(List.of(credencial("cred-1", deJuan, "iPhone de Juan", creada, usada)));

        List<PasskeyDto> passkeys = service.listarPasskeys(7L);

        assertThat(passkeys).containsExactly(new PasskeyDto("cred-1", "iPhone de Juan", creada, usada));
    }

    @Test
    void listarPasskeys_usuarioSinPasskeys_debeDevolverListaVacia() {
        when(webauthnUsuarioRepository.findByUsuarioId(7L)).thenReturn(Optional.empty());

        assertThat(service.listarPasskeys(7L)).isEmpty();
    }

    @Test
    void eliminarPasskey_propia_debeBorrarla() {
        when(credencialRepository.findById("cred-1")).thenReturn(Optional.of(credencial("cred-1", deJuan, "iPhone", null, null)));

        boolean eliminada = service.eliminarPasskey(7L, "cred-1");

        assertThat(eliminada).isTrue();
        verify(credencialRepository).deleteById("cred-1");
    }

    @Test
    void eliminarPasskey_deOtroUsuario_noDebeBorrarla() {
        when(credencialRepository.findById("cred-ana")).thenReturn(Optional.of(credencial("cred-ana", deAna, "Android", null, null)));

        boolean eliminada = service.eliminarPasskey(7L, "cred-ana");

        assertThat(eliminada).isFalse();
        verify(credencialRepository, never()).deleteById(any());
    }

    @Test
    void eliminarPasskey_inexistente_noDebeBorrarNada() {
        when(credencialRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThat(service.eliminarPasskey(7L, "no-existe")).isFalse();
        verify(credencialRepository, never()).deleteById(any());
    }

    private static WebauthnUsuario webauthnUsuario(String handle, long usuarioId) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        WebauthnUsuario w = new WebauthnUsuario();
        w.setId(handle);
        w.setUsuario(usuario);
        return w;
    }

    private static WebauthnCredencial credencial(String id, WebauthnUsuario propietario, String etiqueta,
                                                 Instant creada, Instant usada) {
        WebauthnCredencial c = new WebauthnCredencial();
        c.setCredentialId(id);
        c.setWebauthnUsuario(propietario);
        c.setEtiqueta(etiqueta);
        c.setFechaCreacion(creada);
        c.setFechaUltimoUso(usada);
        return c;
    }
}
