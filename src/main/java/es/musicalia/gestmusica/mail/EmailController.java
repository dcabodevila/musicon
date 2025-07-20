package es.musicalia.gestmusica.mail;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final MailgunEmailService emailService;

    public EmailController(MailgunEmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send-sandbox")
    public MailgunResponse sendTestEmail(@RequestParam String to) {
        return emailService.sendSimpleEmail(to, "Hello from Spring Boot", "Mensaje de prueba vía Mailgun API");
    }
}
