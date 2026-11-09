package es.musicalia.gestmusica.passkey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Passkey (WebAuthn credential) registered by a user. Only the public key is stored.
 */
@Entity
@Table(name = "webauthn_credencial", schema = "gestmusica")
@Getter
@Setter
public class WebauthnCredencial {

    /** Base64url credential id. */
    @Id
    @Column(name = "credential_id")
    private String credentialId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webauthn_usuario_id", nullable = false)
    private WebauthnUsuario webauthnUsuario;

    @Column(name = "public_key")

    private byte[] publicKey;

    @Column(name = "signature_count")

    private long signatureCount;

    @Column(name = "uv_initialized")

    private boolean uvInitialized;

    @Column(name = "backup_eligible")

    private boolean backupEligible;

    @Column(name = "backup_state")

    private boolean backupState;

    @Column(name = "credential_type")

    private String credentialType;

    /** Comma separated authenticator transports (internal, hybrid, usb...). */
    @Column(name = "authenticator_transports")
    private String authenticatorTransports;

    @Column(name = "attestation_object")

    private byte[] attestationObject;

    @Column(name = "attestation_client_data_json")

    private byte[] attestationClientDataJson;

    @Column(name = "etiqueta")

    private String etiqueta;

    @Column(name = "fecha_creacion")

    private Instant fechaCreacion;

    @Column(name = "fecha_ultimo_uso")

    private Instant fechaUltimoUso;
}
