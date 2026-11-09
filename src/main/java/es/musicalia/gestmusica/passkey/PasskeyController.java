package es.musicalia.gestmusica.passkey;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Passkeys of the signed-in user, for the "Mi perfil" page.
 */
@RestController
@RequestMapping("/passkeys")
public class PasskeyController {

    private final PasskeyService passkeyService;

    public PasskeyController(PasskeyService passkeyService) {
        this.passkeyService = passkeyService;
    }

    @GetMapping
    public List<PasskeyDto> listar(@AuthenticationPrincipal CustomAuthenticatedUser user) {
        return passkeyService.listarPasskeys(user.getUserId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal CustomAuthenticatedUser user, @PathVariable String id) {
        return passkeyService.eliminarPasskey(user.getUserId(), id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
