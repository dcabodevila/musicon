package es.musicalia.gestmusica.config;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestCsrfProbeController {

    /** Renders the token the way a form does, so the repository issues it. */
    @GetMapping("/test-csrf/token")
    @ResponseBody
    public String token(CsrfToken csrfToken) {
        return csrfToken.getToken();
    }

    @PostMapping("/test-csrf/save")
    @ResponseBody
    public String save() {
        return "saved";
    }
}
