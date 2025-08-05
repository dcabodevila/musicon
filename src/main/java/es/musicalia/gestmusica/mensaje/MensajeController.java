package es.musicalia.gestmusica.mensaje;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="mensaje")
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @GetMapping("/marcarLeido/{id}")
    public ResponseEntity<Boolean> marcarComoLeido(@AuthenticationPrincipal CustomAuthenticatedUser user, @PathVariable Long id) {
        mensajeService.marcarComoLeido(id, user.getUserId());
        return ResponseEntity.ok(true);
    }
}
