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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailgunEmailService mailgunEmailService;

    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${spring.mail.sender.name:festia}")
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


    public void enviarReporteMensualAgencia(String email, String nombreAgencia, String periodo, Long totalListados, List<Map<String, Object>> chartData) throws EnvioEmailException {
        if (!isMailEnabled) {
            log.info("Envío de correo deshabilitado por configuración. No se enviará el mensaje a: {}", email);
            return;
        }

        EmailTemplateEnum tipo = EmailTemplateEnum.REPORTE_MENSUAL_AGENCIA;
        Context context = new Context();
        context.setVariable("titulo", tipo.getTitulo());
        context.setVariable("subtitulo", tipo.getSubtitulo());
        context.setVariable("mensaje", tipo.getMensaje());
        context.setVariable("nombreAgencia", nombreAgencia);
        context.setVariable("periodo", periodo);
        context.setVariable("totalListados", totalListados);

        // Generar URL de imagen del gráfico usando QuickChart
        String chartImageUrl = generarUrlGraficoQuickChart(chartData);
        context.setVariable("chartImageUrl", chartImageUrl);

        String htmlContent = templateEngine.process(tipo.getTemplate(), context);
        String plainContent = String.format(
                "%s\n\nAgencia: %s\nPeríodo: %s\nTotal de listados generados: %d\n\nAccede a festia.es para ver más detalles.",
                tipo.getMensaje(), nombreAgencia, periodo, totalListados
        );

        EmailDto emailDto = EmailDto.builder()
                .to(email)
                .subject(tipo.getAsunto())
                .content(htmlContent)
                .plainContent(plainContent)
                .isHtml(true)
                .build();

        try {
            final MailgunResponse mailgunResponse = sendMailgunEmail(emailDto);
            log.info("Reporte mensual enviado correctamente a: {} - Respuesta: {}", email, mailgunResponse);
        } catch (Exception e) {
            log.error("Error enviando reporte mensual a {}: {}", email, e.getMessage());
            throw new EnvioEmailException("No se pudo enviar el reporte mensual por correo");
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

    private String generarUrlGraficoQuickChart(List<Map<String, Object>> chartData) {
        try {
            // Extraer labels y valores
            List<String> labels = chartData.stream()
                    .map(item -> (String) item.get("mes"))
                    .collect(Collectors.toList());
            List<Long> values = chartData.stream()
                    .map(item -> (Long) item.get("cantidad"))
                    .collect(Collectors.toList());

            // Crear configuración del gráfico en formato JSON
            String labelsJson = labels.stream()
                    .map(label -> "\"" + label + "\"")
                    .collect(Collectors.joining(","));
            String valuesJson = values.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            String chartConfig = String.format(
                    "{type:'bar',data:{labels:[%s],datasets:[{label:'Número de listados',data:[%s],backgroundColor:'rgba(0,123,255,0.5)',borderColor:'rgba(0,123,255,1)',borderWidth:1}]},options:{responsive:true,scales:{y:{beginAtZero:true,ticks:{stepSize:1}}},plugins:{legend:{display:true,position:'top'},title:{display:true,text:'Listados generados por mes'}}}}",
                    labelsJson, valuesJson
            );

            // Codificar para URL
            String encodedConfig = URLEncoder.encode(chartConfig, StandardCharsets.UTF_8);

            // Generar URL de QuickChart con dimensiones específicas
            return "https://quickchart.io/chart?width=600&height=400&c=" + encodedConfig;

        } catch (Exception e) {
            log.error("Error generando URL del gráfico", e);
            return "";
        }
    }

}
