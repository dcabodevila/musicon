package es.musicalia.gestmusica.passkey;

import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores passkeys (public keys only) in {@code gestmusica.webauthn_credencial}.
 */
@Repository
@Transactional
public class PasskeyCredentialRepository implements UserCredentialRepository {

    private final WebauthnCredencialRepository credencialRepository;
    private final WebauthnUsuarioRepository webauthnUsuarioRepository;

    public PasskeyCredentialRepository(WebauthnCredencialRepository credencialRepository,
                                       WebauthnUsuarioRepository webauthnUsuarioRepository) {
        this.credencialRepository = credencialRepository;
        this.webauthnUsuarioRepository = webauthnUsuarioRepository;
    }

    @Override
    public void save(CredentialRecord record) {
        WebauthnCredencial credencial = credencialRepository.findById(record.getCredentialId().toBase64UrlString())
                .orElseGet(WebauthnCredencial::new);
        credencial.setCredentialId(record.getCredentialId().toBase64UrlString());
        credencial.setWebauthnUsuario(webauthnUsuarioRepository.getReferenceById(record.getUserEntityUserId().toBase64UrlString()));
        credencial.setPublicKey(record.getPublicKey().getBytes());
        credencial.setSignatureCount(record.getSignatureCount());
        credencial.setUvInitialized(record.isUvInitialized());
        credencial.setBackupEligible(record.isBackupEligible());
        credencial.setBackupState(record.isBackupState());
        credencial.setCredentialType(record.getCredentialType() != null ? record.getCredentialType().getValue() : null);
        credencial.setAuthenticatorTransports(record.getTransports() == null ? null : record.getTransports().stream()
                .map(AuthenticatorTransport::getValue)
                .sorted()
                .collect(Collectors.joining(",")));
        credencial.setAttestationObject(record.getAttestationObject() != null ? record.getAttestationObject().getBytes() : null);
        credencial.setAttestationClientDataJson(record.getAttestationClientDataJSON() != null
                ? record.getAttestationClientDataJSON().getBytes() : null);
        credencial.setEtiqueta(record.getLabel());
        credencial.setFechaCreacion(record.getCreated());
        credencial.setFechaUltimoUso(record.getLastUsed());
        credencialRepository.save(credencial);
    }

    @Override
    @Transactional(readOnly = true)
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        return credencialRepository.findById(credentialId.toBase64UrlString())
                .map(PasskeyCredentialRepository::toRecord)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredentialRecord> findByUserId(Bytes userId) {
        return credencialRepository.findByWebauthnUsuarioIdOrderByFechaCreacionAsc(userId.toBase64UrlString()).stream()
                .map(PasskeyCredentialRepository::toRecord)
                .toList();
    }

    @Override
    public void delete(Bytes credentialId) {
        credencialRepository.deleteById(credentialId.toBase64UrlString());
    }

    private static CredentialRecord toRecord(WebauthnCredencial credencial) {
        return ImmutableCredentialRecord.builder()
                .credentialType(credencial.getCredentialType() != null
                        ? PublicKeyCredentialType.valueOf(credencial.getCredentialType()) : null)
                .credentialId(Bytes.fromBase64(credencial.getCredentialId()))
                .userEntityUserId(Bytes.fromBase64(credencial.getWebauthnUsuario().getId()))
                .publicKey(new ImmutablePublicKeyCose(credencial.getPublicKey()))
                .signatureCount(credencial.getSignatureCount())
                .uvInitialized(credencial.isUvInitialized())
                .transports(transports(credencial.getAuthenticatorTransports()))
                .backupEligible(credencial.isBackupEligible())
                .backupState(credencial.isBackupState())
                .attestationObject(credencial.getAttestationObject() != null ? new Bytes(credencial.getAttestationObject()) : null)
                .attestationClientDataJSON(credencial.getAttestationClientDataJson() != null
                        ? new Bytes(credencial.getAttestationClientDataJson()) : null)
                .label(credencial.getEtiqueta())
                .created(credencial.getFechaCreacion())
                .lastUsed(credencial.getFechaUltimoUso())
                .build();
    }

    private static Set<AuthenticatorTransport> transports(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(AuthenticatorTransport::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
