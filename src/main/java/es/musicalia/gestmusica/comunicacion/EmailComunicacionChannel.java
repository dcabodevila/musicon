package es.musicalia.gestmusica.comunicacion;

import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.usuario.EnvioEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Optional;

/**
 * Implementación de {@link ComunicacionChannel} para envío por email.
 * Utiliza {@link EmailService} para enviar mensajes HTML procesados con Thymeleaf.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailComunicacionChannel implements ComunicacionChannel {

    private final EmailService emailService;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void enviar(String destinatario, String asunto, String contenidoHtml) throws Exception {
        log.debug("Enviando comunicación por email a: {}", destinatario);
        try {
            // Procesar el contenido HTML con el template de comunicación
            String htmlFinal = buildContenidoEmailComunicacion(asunto, contenidoHtml, Optional.empty());
            emailService.enviarCorreoHtmlConCc(destinatario, null, asunto, htmlFinal);
            log.debug("Email enviado exitosamente a: {}", destinatario);
        } catch (EnvioEmailException e) {
            log.error("Error al enviar email a {}: {}", destinatario, e.getMessage());
            throw e;
        }
    }

    /**
     * Construye el contenido HTML del email usando el template de comunicación.
     */
    private String buildContenidoEmailComunicacion(String asunto, String contenidoHtml, Optional<String> urlBaja) {
        Context context = new Context();
        context.setVariable("asunto", asunto);
        context.setVariable("mensaje", contenidoHtml);
        context.setVariable("urlBaja", urlBaja.orElse(null));

        return templateEngine.process("comunicacion-email", context);
    }
}
