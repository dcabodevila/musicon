package es.musicalia.gestmusica.passkey;

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
    private String credentialId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "webauthn_usuario_id", nullable = false)
    private WebauthnUsuario webauthnUsuario;

    private byte[] publicKey;

    private long signatureCount;

    private boolean uvInitialized;

    private boolean backupEligible;

    private boolean backupState;

    private String credentialType;

    /** Comma separated authenticator transports (internal, hybrid, usb...). */
    private String authenticatorTransports;

    private byte[] attestationObject;

    private byte[] attestationClientDataJson;

    private String etiqueta;

    private Instant fechaCreacion;

    private Instant fechaUltimoUso;
}
