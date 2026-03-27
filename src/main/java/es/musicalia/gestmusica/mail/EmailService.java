package es.musicalia.gestmusica.mail;

import es.musicalia.gestmusica.usuario.EnvioEmailException;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.reactivacion.ReactivacionTokenService;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailgunEmailService mailgunEmailService;
    private final UsuarioRepository usuarioRepository;
    private final ReactivacionTokenService reactivacionTokenService;

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
    @Value("${app.base-url:https://festia.es}")
    private String baseUrl;


    public void enviarMensajePorEmail(String email, EmailTemplateEnum tipo) throws EnvioEmailException {
        if (!isMailEnabled) {
            log.info("Envío de correo deshabilitado por configuración. No se enviará el mensaje a: {}", email);
            return;
        }

        EmailDto emailDto = EmailDto.builder()
                .to(email)
                .subject(tipo.getAsunto())
                .content(buildContenidoEmailSimple(email, tipo))
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
                    .content(buildContenidoEmailCodigoAuth(email, codigo, tipo))
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

    public void enviarCorreoPlano(String email, String asunto, String contenido) throws EnvioEmailException {
        if (!isMailEnabled) {
            log.info("Envío de correo deshabilitado por configuración. No se enviará el mensaje a: {}", email);
            return;
        }

        EmailDto emailDto = EmailDto.builder()
                .to(email)
                .subject(asunto)
                .content(contenido)
                .plainContent(contenido)
                .isHtml(false)
                .build();

        try {
            final MailgunResponse mailgunResponse = sendMailgunEmail(emailDto);
            log.info("Email enviado correctamente a: {} - Respuesta: {}", email, mailgunResponse);
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", email, e.getMessage());
            throw new EnvioEmailException("No se pudo enviar el correo");
        }
    }


    private String buildContenidoEmailCodigoAuth(String email, String codigo, EmailTemplateEnum tipo) {



        Context context = new Context();
        addContextoPorTipo(context, tipo, "codigo", codigo);
        context.setVariable("expiracionMinutos", minutosExpiracion);
        context.setVariable("contenidoExtra", ""); // puedes inyectar HTML adicional opcionalmente
        context.setVariable("urlBaja", construirUrlBajaPorEmail(email).orElse(null));

        return templateEngine.process(tipo.getTemplate(), context);
    }

    private String buildContenidoEmailSimple(String email, EmailTemplateEnum tipo) {

        Context context = new Context();
        addContextoPorTipo(context, tipo, "contenidoExtra", "");
        context.setVariable("urlBaja", construirUrlBajaPorEmail(email).orElse(null));

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
        Optional<Usuario> optUsuario = usuarioRepository.findUsuarioByMail(emailDto.getTo());

        if (optUsuario.isPresent() && optUsuario.get().isEmailBaja()) {
            log.info("Email no enviado a {} porque el usuario está dado de baja", emailDto.getTo());
            MailgunResponse response = new MailgunResponse();
            response.setStatus("skipped");
            response.setMessage("Usuario dado de baja para correos electrónicos");
            return response;
        }

        String contenido = emailDto.getContent();
        if (emailDto.isHtml()) {
            contenido = enriquecerContenidoHtmlConBaja(contenido, optUsuario);
        } else {
            contenido = enriquecerContenidoPlanoConBaja(contenido, optUsuario);
        }

        return mailgunEmailService.sendSimpleEmail(emailDto.getTo(), emailDto.getSubject(), contenido, emailDto.getCc());
    }

    public void enviarCorreoHtmlConCc(String to, List<String> cc, String asunto, String contenidoHtml) throws EnvioEmailException {
        if (!isMailEnabled) {
            log.info("Envío de correo deshabilitado por configuración. No se enviará el mensaje a: {}", to);
            return;
        }

        EmailDto emailDto = EmailDto.builder()
                .to(to)
                .cc(cc)
                .subject(asunto)
                .content(contenidoHtml)
                .plainContent(contenidoHtml)
                .isHtml(true)
                .build();

        try {
            final MailgunResponse mailgunResponse = sendMailgunEmail(emailDto);
            log.info("Email enviado correctamente a: {} - Respuesta: {}", to, mailgunResponse);
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", to, e.getMessage());
            throw new EnvioEmailException("No se pudo enviar el correo");
        }
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
        context.setVariable("urlBaja", construirUrlBajaPorEmail(email).orElse(null));

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

    private Optional<String> construirUrlBajaPorEmail(String email) {
        Optional<Usuario> optUsuario = usuarioRepository.findUsuarioByMail(email)
                .filter(usuario -> !usuario.isEmailBaja());

        if (optUsuario.isEmpty()) {
            return Optional.empty();
        }

        try {
            String token = reactivacionTokenService.generarYPersistirToken(optUsuario.get().getId());
            return Optional.of(baseUrl + "/baja/email/" + token);
        } catch (Exception e) {
            log.warn("No se pudo generar token de baja para {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    private String enriquecerContenidoHtmlConBaja(String html, Optional<Usuario> optUsuario) {
        if (html == null || html.isBlank()) {
            return html;
        }
        if (html.contains("/baja/email/")) {
            return html;
        }

        Optional<String> urlBaja = optUsuario
                .filter(usuario -> !usuario.isEmailBaja())
                .flatMap(this::construirUrlBajaPorUsuario);

        if (urlBaja.isEmpty()) {
            return html;
        }

        String footerBaja = """
                <div style=\"margin-top:24px;padding-top:16px;border-top:1px solid #e5e7eb;text-align:center;font-size:12px;color:#6b7280;\">
                  Si prefieres dejar de recibir estos emails,
                  <a href=\"%s\" style=\"color:#2563eb;text-decoration:none;\">darte de baja</a>.
                </div>
                """.formatted(urlBaja.get());

        if (html.toLowerCase().contains("</body>")) {
            return html.replaceFirst("(?i)</body>", footerBaja + "</body>");
        }
        return html + footerBaja;
    }

    private String enriquecerContenidoPlanoConBaja(String contenido, Optional<Usuario> optUsuario) {
        if (contenido == null || contenido.isBlank()) {
            return contenido;
        }
        if (contenido.contains("/baja/email/")) {
            return contenido;
        }

        Optional<String> urlBaja = optUsuario
                .filter(usuario -> !usuario.isEmailBaja())
                .flatMap(this::construirUrlBajaPorUsuario);

        if (urlBaja.isEmpty()) {
            return contenido;
        }
        return contenido + "\n\nSi prefieres dejar de recibir estos emails, podés darte de baja aquí: " + urlBaja.get();
    }

    private Optional<String> construirUrlBajaPorUsuario(Usuario usuario) {
        try {
            String token = reactivacionTokenService.generarYPersistirToken(usuario.getId());
            return Optional.of(baseUrl + "/baja/email/" + token);
        } catch (Exception e) {
            log.warn("No se pudo generar token de baja para usuario {}: {}", usuario.getId(), e.getMessage());
            return Optional.empty();
        }
    }

}
