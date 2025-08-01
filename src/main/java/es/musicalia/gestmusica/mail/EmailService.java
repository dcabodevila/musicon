package es.musicalia.gestmusica.mail;

import es.musicalia.gestmusica.usuario.EnvioEmailException;
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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailgunEmailService mailgunEmailService;

    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${spring.mail.sender.name:Gestmusica}")
    private String senderName;
    @Value("${app.verificacion.expiracion-minutos:15}")
    private int minutosExpiracion;
    @Value("${app.verificacion.max-intentos:3}")
    private int maxIntentos;
    @Value("${app.mail.enabled:true}")
    private boolean isMailEnabled;


    public void enviarMensajePorEmail(String email, EmailTemplateEnum tipo) throws EnvioEmailException {
        if (!isMailEnabled) {
            log.info("Envío de correo deshabilitado por configuración. No se enviará el mensaje a: {}", email);
            return;
        }

        EmailDto emailDto = EmailDto.builder()
                .to(email)
                .subject(tipo.getAsunto())
                .content(buildContenidoEmailSimple(tipo))
                .plainContent(construirContenidoEmailSimplePlain(tipo))
                .isHtml(true)
                .build();

        try {
            final MailgunResponse mailgunResponse = sendMailgunEmail(emailDto);
            log.info("Email enviado correctamente a: {} - Respuesta: {}", email, mailgunResponse);
        } catch (Exception e) {
            log.error("Error email a {}: {}", email, e.getMessage());
            throw new EnvioEmailException("No se pudo enviar el la notificación por correo");
        }
    }



    public void enviarCodigoPorEmail(String email, String codigo, EmailTemplateEnum tipo) throws EnvioEmailException {

            EmailDto emailDto = EmailDto.builder()
                    .to(email)
                    .subject(tipo.getAsunto())
                    .content(buildContenidoEmailCodigoAuth(codigo, tipo))
                    .plainContent(construirContenidoEmailCodigoAuthPlain(codigo, tipo))
                    .isHtml(true)
                    .build();

        try {
            final MailgunResponse mailgunResponse = sendMailgunEmail(emailDto);
            log.info("Email enviado correctamente a: {} - Respuesta: {}", email, mailgunResponse);
        } catch (Exception e) {
            log.error("Error enviando código de verificación a {}: {}", email, e.getMessage());
            throw new EnvioEmailException("No se pudo enviar el código de verificación");
        }
    }


    private String buildContenidoEmailCodigoAuth(String codigo, EmailTemplateEnum tipo) {



        Context context = new Context();
        addContextoPorTipo(context, tipo, "codigo", codigo);
        context.setVariable("expiracionMinutos", minutosExpiracion);
        context.setVariable("contenidoExtra", ""); // puedes inyectar HTML adicional opcionalmente

        return templateEngine.process(tipo.getTemplate(), context);
    }

    private String buildContenidoEmailSimple(EmailTemplateEnum tipo) {

        Context context = new Context();
        addContextoPorTipo(context, tipo, "contenidoExtra", "");

        return templateEngine.process(tipo.getTemplate(), context);
    }

    private static void addContextoPorTipo(Context context, EmailTemplateEnum tipo, String contenidoExtra, String value) {
        context.setVariable("titulo", tipo.getTitulo() != null ? tipo.getTitulo() : "Gestmusica");
        context.setVariable("subtitulo", tipo.getSubtitulo() != null ? tipo.getSubtitulo() : "Notificación gestmusica");
        context.setVariable("mensaje", tipo.getMensaje());
        context.setVariable(contenidoExtra, value);
    }

    private String construirContenidoEmailCodigoAuthPlain(String codigo, EmailTemplateEnum tipo) {


        return """
        %s

        Tu código es: %s

        Este código es personal, intransferible y expira en %d minutos.
        No lo compartas con nadie.

        Si no solicitaste este código, ignora este mensaje.

        """.formatted(tipo.getMensaje(), codigo, minutosExpiracion);
    }

    private String construirContenidoEmailSimplePlain(EmailTemplateEnum tipo) {


        return """
        %s
      
        """.formatted(tipo.getMensaje(),  minutosExpiracion);
    }


    public MailgunResponse sendMailgunEmail(EmailDto emailDto) {

        return mailgunEmailService.sendSimpleEmail(emailDto.getTo(), emailDto.getSubject(), emailDto.getContent());
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


}
