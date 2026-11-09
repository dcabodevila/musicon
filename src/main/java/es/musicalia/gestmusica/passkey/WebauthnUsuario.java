package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * WebAuthn user entity (user handle) linked to a {@link Usuario}. One per user.
 */
@Entity
@Table(name = "webauthn_usuario", schema = "gestmusica")
@Getter
@Setter
public class WebauthnUsuario {

    /** Base64url user handle shared with the authenticators. */
    @Id
    @Column(name = "id")
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "nombre")

    private String nombre;

    @Column(name = "nombre_visible")

    private String nombreVisible;
}
