package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.auth.model.UsuarioUserDetailsService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the application user that owns a verified passkey, by the user id linked to its WebAuthn user handle
 * (never by login name, which can be ambiguous).
 */
@Component
public class PasskeyOwnerLoader {

    private final WebauthnUsuarioRepository webauthnUsuarioRepository;
    private final UsuarioUserDetailsService usuarioUserDetailsService;

    public PasskeyOwnerLoader(WebauthnUsuarioRepository webauthnUsuarioRepository,
                              UsuarioUserDetailsService usuarioUserDetailsService) {
        this.webauthnUsuarioRepository = webauthnUsuarioRepository;
        this.usuarioUserDetailsService = usuarioUserDetailsService;
    }

    @Transactional
    public UserDetails loadOwner(Bytes userHandle) {
        Long usuarioId = webauthnUsuarioRepository.findById(userHandle.toBase64UrlString())
                .map(webauthnUsuario -> webauthnUsuario.getUsuario().getId())
                .orElseThrow(() -> new BadCredentialsException("Unknown passkey owner"));
        return usuarioUserDetailsService.loadUserByUsuarioId(usuarioId, true);
    }
}
