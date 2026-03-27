package es.musicalia.gestmusica.reactivacion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Endpoint público para procesar solicitudes de baja de emails de reactivación.
 * La ruta /baja/** está exenta de autenticación en WebSecurityConfig.
 */
@Slf4j
@Controller
@RequestMapping("/baja")
@RequiredArgsConstructor
public class BajaEmailController {

    private final ReactivacionEmailService reactivacionEmailService;

    @GetMapping("/email/{token}")
    public String confirmarBaja(@PathVariable String token, Model model) {
        log.info("Solicitud de baja de email con token: {}...", token.length() > 8 ? token.substring(0, 8) : token);
        model.addAttribute("token", token);
        model.addAttribute("estado", "CONFIRMAR");
        return "baja-email-confirmacion";
    }

    @PostMapping("/email/{token}")
    public String procesarBaja(@PathVariable String token, Model model) {
        log.info("Confirmación de baja de email con token: {}...", token.length() > 8 ? token.substring(0, 8) : token);
        boolean exito = reactivacionEmailService.procesarBajaPorToken(token);
        model.addAttribute("exito", exito);
        model.addAttribute("estado", "RESULTADO");
        return "baja-email-confirmacion";
    }
}
