package es.musicalia.gestmusica.mail;

import es.musicalia.gestmusica.mail.EmailDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.sender.name:Gestmusica}")
    private String senderName;

    /**
     * Envía un email simple de texto plano
     */
    public void sendSimpleEmail(String to, String subject, String content) {
        log.info("Enviando email simple a: {}", to);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Email enviado exitosamente a: {}", to);

        } catch (MailException e) {
            log.error("Error enviando email simple a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    /**
     * Envía un email con formato HTML y adjuntos
     */
    public void sendHtmlEmail(EmailDto emailDto) {
        log.info("Enviando email HTML a: {}", emailDto.getTo());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Configurar remitente
            helper.setFrom(fromEmail, senderName);

            // Configurar destinatario principal
            helper.setTo(emailDto.getTo());

            // Configurar CC si existe
            if (emailDto.getCc() != null && !emailDto.getCc().isEmpty()) {
                helper.setCc(emailDto.getCc().toArray(new String[0]));
            }

            // Configurar BCC si existe
            if (emailDto.getBcc() != null && !emailDto.getBcc().isEmpty()) {
                helper.setBcc(emailDto.getBcc().toArray(new String[0]));
            }

            // Configurar asunto y contenido
            helper.setSubject(emailDto.getSubject());
            //helper.setText(emailDto.getContent(), emailDto.isHtml());
            helper.setText(emailDto.getPlainContent(), emailDto.getContent());


            // Agregar adjuntos si existen
            if (emailDto.getAttachments() != null && !emailDto.getAttachments().isEmpty()) {
                addAttachments(helper, emailDto.getAttachments());
            }

            mailSender.send(mimeMessage);
            log.info("Email HTML enviado exitosamente a: {}", emailDto.getTo());

        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            log.error("Error enviando email HTML a {}: {}", emailDto.getTo(), e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }

    /**
     * Envía múltiples emails
     */
    public void sendBulkEmails(List<EmailDto> emails) {
        log.info("Enviando {} emails en lote", emails.size());

        for (EmailDto email : emails) {
            try {
                sendHtmlEmail(email);
                // Pequeña pausa para evitar saturar el servidor SMTP
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Error enviando email a {}: {}", email.getTo(), e.getMessage());
                // Continúa con el siguiente email en caso de error
            }
        }

        log.info("Envío en lote completado");
    }

    /**
     * Envía email con plantilla HTML
     */
    public void sendTemplateEmail(String to, String subject, String templateContent,
                                  Object... templateParams) {
        String formattedContent = String.format(templateContent, templateParams);

        EmailDto emailDto = EmailDto.builder()
                .to(to)
                .subject(subject)
                .content(formattedContent)
                .isHtml(true)
                .build();

        sendHtmlEmail(emailDto);
    }

    /**
     * Envía email de notificación del sistema
     */
    public void sendSystemNotification(String to, String subject, String message) {
        String htmlContent = buildSystemNotificationTemplate(subject, message);

        EmailDto emailDto = EmailDto.builder()
                .to(to)
                .subject("[Gestmusica] " + subject)
                .content(htmlContent)
                .isHtml(true)
                .build();

        sendHtmlEmail(emailDto);
    }

    /**
     * Verifica la configuración de email
     */
    public boolean testEmailConfiguration() {
        try {
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setFrom(fromEmail);
            testMessage.setTo("dcabodevila@gmail.com"); // Enviar a sí mismo
            testMessage.setSubject("Test de configuración - Gestmusica");
            testMessage.setText("Este es un email de prueba para verificar la configuración.");

            mailSender.send(testMessage);
            log.info("Test de email exitoso");
            return true;

        } catch (Exception e) {
            log.error("Error en test de email: {}", e.getMessage());
            return false;
        }
    }

    private void addAttachments(MimeMessageHelper helper, List<String> attachmentPaths)
            throws MessagingException {
        for (String path : attachmentPaths) {
            if (StringUtils.hasText(path)) {
                File file = new File(path);
                if (file.exists()) {
                    FileSystemResource fileResource = new FileSystemResource(file);
                    helper.addAttachment(file.getName(), fileResource);
                    log.debug("Adjunto agregado: {}", file.getName());
                } else {
                    log.warn("Archivo no encontrado para adjuntar: {}", path);
                }
            }
        }
    }

    private String buildSystemNotificationTemplate(String subject, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f8f9fa; }
                    .footer { padding: 10px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>%s</h2>
                    </div>
                    <div class="content">
                        <p>%s</p>
                    </div>
                    <div class="footer">
                        <p>Este es un mensaje automático de Gestmusica</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(subject, message);
    }
}
