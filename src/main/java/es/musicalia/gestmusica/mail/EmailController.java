package es.musicalia.gestmusica.mail;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@Valid @RequestBody EmailDto emailDto) {
        try {
            emailService.sendHtmlEmail(emailDto);
            return ResponseEntity.ok("Email enviado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error enviando email: " + e.getMessage());
        }
    }

    @PostMapping("/send-simple")
    public ResponseEntity<String> sendSimpleEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content) {
        try {
            emailService.sendSimpleEmail(to, subject, content);
            return ResponseEntity.ok("Email simple enviado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error enviando email: " + e.getMessage());
        }
    }

    @PostMapping("/test")
    public ResponseEntity<String> testEmailConfiguration() {
        boolean success = emailService.testEmailConfiguration();
        if (success) {
            return ResponseEntity.ok("Configuración de email correcta");
        } else {
            return ResponseEntity.badRequest().body("Error en la configuración de email");
        }
    }
}
