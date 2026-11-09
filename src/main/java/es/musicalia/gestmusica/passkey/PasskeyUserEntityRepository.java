package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Stores WebAuthn user entities in {@code gestmusica.webauthn_usuario}, linked to the application user.
 * The returned name is always the user's current login name, so passkeys keep working after a username change.
 * <p>
 * Lookups by name (used while registering a passkey) only resolve the signed-in user, by id: a login name can be
 * ambiguous because a username may look like another user's email.
 */
@Repository
@Transactional
public class PasskeyUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private final WebauthnUsuarioRepository webauthnUsuarioRepository;
    private final UsuarioRepository usuarioRepository;

    public PasskeyUserEntityRepository(WebauthnUsuarioRepository webauthnUsuarioRepository,
                                       UsuarioRepository usuarioRepository) {
        this.webauthnUsuarioRepository = webauthnUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        return webauthnUsuarioRepository.findById(id.toBase64UrlString())
                .map(PasskeyUserEntityRepository::toUserEntity)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        return usuarioAutenticado(username)
                .flatMap(usuarioId -> webauthnUsuarioRepository.findByUsuarioId(usuarioId))
                .map(PasskeyUserEntityRepository::toUserEntity)
                .orElse(null);
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {
        Usuario usuario = usuarioAutenticado(userEntity.getName())
                .flatMap(usuarioRepository::findById)
                .orElseThrow(() -> new IllegalStateException("A passkey can only be registered by the signed-in user"));
        WebauthnUsuario webauthnUsuario = webauthnUsuarioRepository.findById(userEntity.getId().toBase64UrlString())
                .orElseGet(WebauthnUsuario::new);
        webauthnUsuario.setId(userEntity.getId().toBase64UrlString());
        webauthnUsuario.setUsuario(usuario);
        webauthnUsuario.setNombre(userEntity.getName());
        webauthnUsuario.setNombreVisible(userEntity.getDisplayName());
        webauthnUsuarioRepository.save(webauthnUsuario);
    }

    @Override
    public void delete(Bytes id) {
        webauthnUsuarioRepository.deleteById(id.toBase64UrlString());
    }

    /** Id of the signed-in user when {@code username} is their login name; empty otherwise. */
    private static Optional<Long> usuarioAutenticado(String username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (username == null || authentication == null
                || !(authentication.getPrincipal() instanceof CustomAuthenticatedUser user)
                || !username.equals(user.getUsername())) {
            return Optional.empty();
        }
        return Optional.of(user.getUserId());
    }

    private static PublicKeyCredentialUserEntity toUserEntity(WebauthnUsuario webauthnUsuario) {
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(Bytes.fromBase64(webauthnUsuario.getId()))
                .name(CustomAuthenticatedUser.nombreDeLogin(webauthnUsuario.getUsuario()))
                .displayName(webauthnUsuario.getNombreVisible())
                .build();
    }
}
