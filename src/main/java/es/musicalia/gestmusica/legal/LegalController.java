package es.musicalia.gestmusica.legal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/legal")
public class LegalController {

    @GetMapping("/aviso-legal")
    public String avisoLegal() {
        return "legal/aviso-legal";
    }

    @GetMapping("/privacidad")
    public String politicaPrivacidad() {
        return "legal/politica-privacidad";
    }

    @GetMapping("/cookies")
    public String politicaCookies() {
        return "legal/politica-cookies";
    }
}
