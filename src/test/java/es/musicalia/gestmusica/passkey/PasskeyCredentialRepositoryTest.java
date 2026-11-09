package es.musicalia.gestmusica.passkey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasskeyCredentialRepositoryTest {

    private final WebauthnCredencialRepository credencialRepository = mock(WebauthnCredencialRepository.class);
    private final WebauthnUsuarioRepository webauthnUsuarioRepository = mock(WebauthnUsuarioRepository.class);
    private PasskeyCredentialRepository repository;

    private final Bytes credentialId = Bytes.random();
    private final Bytes userHandle = Bytes.random();
    private final WebauthnUsuario webauthnUsuario = new WebauthnUsuario();

    @BeforeEach
    void setUp() {
        repository = new PasskeyCredentialRepository(credencialRepository, webauthnUsuarioRepository);
        webauthnUsuario.setId(userHandle.toBase64UrlString());
    }

    @Test
    void save_yLectura_debenConservarTodosLosCampos() {
        CredentialRecord original = credencial();
        when(credencialRepository.findById(credentialId.toBase64UrlString())).thenReturn(Optional.empty());
        when(webauthnUsuarioRepository.getReferenceById(userHandle.toBase64UrlString())).thenReturn(webauthnUsuario);

        repository.save(original);

        ArgumentCaptor<WebauthnCredencial> captor = ArgumentCaptor.forClass(WebauthnCredencial.class);
        verify(credencialRepository).save(captor.capture());
        WebauthnCredencial guardada = captor.getValue();
        assertThat(guardada.getCredentialId()).isEqualTo(credentialId.toBase64UrlString());
        assertThat(guardada.getWebauthnUsuario()).isSameAs(webauthnUsuario);

        when(credencialRepository.findById(credentialId.toBase64UrlString())).thenReturn(Optional.of(guardada));
        CredentialRecord leida = repository.findByCredentialId(credentialId);

        assertThat(leida).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void save_existente_debeActualizarContadorYUltimoUso() {
        WebauthnCredencial existente = new WebauthnCredencial();
        existente.setCredentialId(credentialId.toBase64UrlString());
        existente.setWebauthnUsuario(webauthnUsuario);
        existente.setSignatureCount(1);
        when(credencialRepository.findById(credentialId.toBase64UrlString())).thenReturn(Optional.of(existente));
        when(webauthnUsuarioRepository.getReferenceById(userHandle.toBase64UrlString())).thenReturn(webauthnUsuario);

        Instant ahora = Instant.parse("2026-11-10T09:00:00Z");
        repository.save(ImmutableCredentialRecord.fromCredentialRecord(credencial()).signatureCount(9).lastUsed(ahora).build());

        verify(credencialRepository).save(existente);
        assertThat(existente.getSignatureCount()).isEqualTo(9);
        assertThat(existente.getFechaUltimoUso()).isEqualTo(ahora);
    }

    @Test
    void findByCredentialId_desconocida_debeDevolverNull() {
        when(credencialRepository.findById(credentialId.toBase64UrlString())).thenReturn(Optional.empty());

        assertThat(repository.findByCredentialId(credentialId)).isNull();
    }

    @Test
    void findByUserId_debeDevolverLasPasskeysDelUsuario() {
        WebauthnCredencial c = new WebauthnCredencial();
        c.setCredentialId(credentialId.toBase64UrlString());
        c.setWebauthnUsuario(webauthnUsuario);
        c.setPublicKey(new byte[]{1});
        when(credencialRepository.findByWebauthnUsuarioIdOrderByFechaCreacionAsc(userHandle.toBase64UrlString()))
                .thenReturn(List.of(c));

        List<CredentialRecord> credenciales = repository.findByUserId(userHandle);

        assertThat(credenciales).hasSize(1);
        assertThat(credenciales.get(0).getCredentialId()).isEqualTo(credentialId);
        assertThat(credenciales.get(0).getUserEntityUserId()).isEqualTo(userHandle);
    }

    @Test
    void delete_debeBorrarPorCredentialId() {
        repository.delete(credentialId);

        verify(credencialRepository).deleteById(credentialId.toBase64UrlString());
    }

    private CredentialRecord credencial() {
        return ImmutableCredentialRecord.builder()
                .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
                .credentialId(credentialId)
                .userEntityUserId(userHandle)
                .publicKey(new ImmutablePublicKeyCose(new byte[]{1, 2, 3}))
                .signatureCount(3)
                .uvInitialized(true)
                .transports(Set.of(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.HYBRID))
                .backupEligible(true)
                .backupState(true)
                .attestationObject(new Bytes(new byte[]{4, 5}))
                .attestationClientDataJSON(new Bytes(new byte[]{6}))
                .created(Instant.parse("2026-11-09T10:00:00Z"))
                .lastUsed(Instant.parse("2026-11-09T11:00:00Z"))
                .label("iPhone de Juan")
                .build();
    }
}
