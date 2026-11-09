package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
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
 */
@Repository
@Transactional
public class PasskeyUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";

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
        return buscarUsuario(username)
                .flatMap(usuario -> webauthnUsuarioRepository.findByUsuarioId(usuario.getId()))
                .map(PasskeyUserEntityRepository::toUserEntity)
                .orElse(null);
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {
        Usuario usuario = buscarUsuario(userEntity.getName())
                .orElseThrow(() -> new IllegalStateException("No user found for passkey owner " + userEntity.getName()));
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

    private Optional<Usuario> buscarUsuario(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String trimmed = username.trim();
        return trimmed.matches(EMAIL_PATTERN)
                ? usuarioRepository.findUsuarioActivoByMail(trimmed)
                : usuarioRepository.findByUsername(trimmed);
    }

    private static PublicKeyCredentialUserEntity toUserEntity(WebauthnUsuario webauthnUsuario) {
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(Bytes.fromBase64(webauthnUsuario.getId()))
                .name(CustomAuthenticatedUser.nombreDeLogin(webauthnUsuario.getUsuario()))
                .displayName(webauthnUsuario.getNombreVisible())
                .build();
    }
}
