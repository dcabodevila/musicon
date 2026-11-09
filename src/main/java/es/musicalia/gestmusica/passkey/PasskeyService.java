package es.musicalia.gestmusica.passkey;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Lets a user see and remove their own passkeys. Registration goes through Spring Security's WebAuthn endpoints.
 */
@Service
public class PasskeyService {

    private final WebauthnUsuarioRepository webauthnUsuarioRepository;
    private final WebauthnCredencialRepository credencialRepository;

    public PasskeyService(WebauthnUsuarioRepository webauthnUsuarioRepository,
                          WebauthnCredencialRepository credencialRepository) {
        this.webauthnUsuarioRepository = webauthnUsuarioRepository;
        this.credencialRepository = credencialRepository;
    }

    @Transactional(readOnly = true)
    public List<PasskeyDto> listarPasskeys(Long usuarioId) {
        return webauthnUsuarioRepository.findByUsuarioId(usuarioId)
                .map(webauthnUsuario -> credencialRepository.findByWebauthnUsuarioIdOrderByFechaCreacionAsc(webauthnUsuario.getId()))
                .orElse(List.of())
                .stream()
                .map(c -> new PasskeyDto(c.getCredentialId(), c.getEtiqueta(), c.getFechaCreacion(), c.getFechaUltimoUso()))
                .toList();
    }

    /**
     * Deletes the passkey only if it belongs to the given user.
     *
     * @return false when it does not exist or belongs to someone else
     */
    @Transactional
    public boolean eliminarPasskey(Long usuarioId, String credentialId) {
        return credencialRepository.findById(credentialId)
                .filter(c -> Objects.equals(c.getWebauthnUsuario().getUsuario().getId(), usuarioId))
                .map(c -> {
                    credencialRepository.deleteById(c.getCredentialId());
                    return true;
                })
                .orElse(false);
    }
}
