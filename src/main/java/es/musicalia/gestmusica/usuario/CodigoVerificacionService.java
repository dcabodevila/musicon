package es.musicalia.gestmusica.usuario;


import es.musicalia.gestmusica.mail.EmailDto;
import es.musicalia.gestmusica.mail.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodigoVerificacionService {

    private final CodigoVerificacionRepository codigoRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.verificacion.expiracion-minutos:15}")
    private int minutosExpiracion;

    @Value("${app.verificacion.max-intentos:3}")
    private int maxIntentos;

    /**
     * Genera y envía un código de verificación de 4 dígitos
     */
    @Transactional
    public void generarYEnviarCodigo(String email, CodigoVerificacion.TipoVerificacion tipo) throws EnvioEmailException {
        log.info("Generando código de verificación para email: {} tipo: {}", email, tipo);

        // Desactivar códigos previos
        //codigoRepository.desactivarCodigosPrevios(email, tipo);

        // Generar nuevo código
        String codigo = generarCodigoAleatorio();
        
        // Crear y guardar código
        CodigoVerificacion codigoVerificacion = CodigoVerificacion.builder()
                .email(email)
                .codigo(codigo)
                .tipo(tipo)
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(minutosExpiracion))
                .usado(false)
                .activo(true)
                .build();

        codigoRepository.save(codigoVerificacion);

        // Enviar email
        enviarCodigoPorEmail(email, codigo, tipo);

        log.info("Código de verificación generado y enviado para: {}", email);
    }

    /**
     * Verifica si un código es válido
     */
    @Transactional
    public boolean verificarCodigo(String email, String codigo, CodigoVerificacion.TipoVerificacion tipo, boolean usado) {
        log.info("Verificando código para email: {} tipo: {}", email, tipo);

        Optional<CodigoVerificacion> codigoOpt = codigoRepository
                .findByEmailAndCodigoAndTipoAndActivoTrueAndUsado(email, codigo, tipo, usado);

        if (codigoOpt.isEmpty()) {
            log.warn("Código no encontrado o inválido para email: {}", email);
            return false;
        }

        CodigoVerificacion codigoVerificacion = codigoOpt.get();

        if (codigoVerificacion.isExpirado()) {
            log.warn("Código expirado para email: {}", email);
            return false;
        }

        // Marcar código como usado
        codigoVerificacion.setUsado(true);
        codigoRepository.save(codigoVerificacion);

        log.info("Código verificado correctamente para email: {}", email);
        return true;
    }

    /**
     * Verifica si existe un código válido sin marcarlo como usado
     */
    public boolean existeCodigoValido(String email, CodigoVerificacion.TipoVerificacion tipo) {
        Optional<CodigoVerificacion> codigo = codigoRepository
                .findCodigoValidoByEmailAndTipo(email, tipo, LocalDateTime.now());
        return codigo.isPresent();
    }

    /**
     * Reenvía código de verificación si es válido hacerlo
     */
    @Transactional
    public boolean reenviarCodigo(String email, CodigoVerificacion.TipoVerificacion tipo) throws EnvioEmailException {
        log.info("Intentando reenviar código para email: {}", email);

        Optional<CodigoVerificacion> codigoOpt = codigoRepository
                .findCodigoValidoByEmailAndTipo(email, tipo, LocalDateTime.now());

        if (codigoOpt.isPresent()) {
            CodigoVerificacion codigo = codigoOpt.get();
            
            // Solo reenviar si han pasado al menos 2 minutos
            if (codigo.getFechaCreacion().plusMinutes(2).isAfter(LocalDateTime.now())) {
                log.warn("Debe esperar antes de reenviar código para: {}", email);
                return false;
            }

            enviarCodigoPorEmail(email, codigo.getCodigo(), tipo);
            log.info("Código reenviado para email: {}", email);
            return true;
        }

        // Si no hay código válido, generar uno nuevo
        generarYEnviarCodigo(email, tipo);
        return true;
    }

    /**
     * Limpia códigos expirados (para ejecutar programáticamente)
     */
    @Transactional
    public void limpiarCodigosExpirados() {
        log.info("Limpiando códigos expirados");
        codigoRepository.eliminarCodigosExpirados(LocalDateTime.now().minusDays(1));
    }

    private String generarCodigoAleatorio() {
        return String.format("%04d", secureRandom.nextInt(10000));
    }

    private void enviarCodigoPorEmail(String email, String codigo, CodigoVerificacion.TipoVerificacion tipo) throws EnvioEmailException {

        EmailDto emailDto = EmailDto.builder()
                .to(email)
                .subject(obtenerAsuntoPorTipo(tipo))
                .content(construirContenidoEmailHtml(codigo, tipo))
                .plainContent(construirContenidoEmailPlain(codigo, tipo))
                .isHtml(true)
                .build();

        try {
            emailService.sendHtmlEmail(emailDto);
        } catch (Exception e) {
            log.error("Error enviando código de verificación a {}: {}", email, e.getMessage());
            throw new EnvioEmailException("No se pudo enviar el código de verificación");
        }
    }

    private String obtenerAsuntoPorTipo(CodigoVerificacion.TipoVerificacion tipo) {
        return switch (tipo) {
            case REGISTRO -> "Código de verificación - Nuevo usuario en Gestmusica";
            case RECUPERACION_PASSWORD -> "Código de verificación - Recuperación de contraseña";
            case CAMBIO_EMAIL -> "Código de verificación - Cambio de email";
        };
    }

    private String construirContenidoEmailHtml(String codigo, CodigoVerificacion.TipoVerificacion tipo) {
        String mensaje = switch (tipo) {
            case REGISTRO -> "Gracias por registrarte en Gestmusica. Para completar tu registro, utiliza el siguiente código:";
            case RECUPERACION_PASSWORD -> "Has solicitado restablecer tu contraseña. Utiliza el siguiente código:";
            case CAMBIO_EMAIL -> "Has solicitado cambiar tu email. Utiliza el siguiente código:";
        };

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                .header { background: linear-gradient(135deg, #007bff, #0056b3); color: white; padding: 30px 20px; text-align: center; }
                .header h1 { margin: 0; font-size: 28px; }
                .content { padding: 40px 20px; text-align: center; }
                .code-container { background-color: #f8f9fa; border: 2px dashed #007bff; border-radius: 10px; padding: 20px; margin: 20px 0; }
                .code { font-size: 36px; font-weight: bold; color: #007bff; letter-spacing: 8px; font-family: 'Courier New', monospace; }
                .message { font-size: 16px; color: #333; margin: 20px 0; line-height: 1.5; }
                .footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 14px; color: #666; }
                .footer a { color: #007bff; text-decoration: none; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Gestmusica</h1>
                    <p>Código de Verificación</p>
                </div>
                <div class="content">
                    <p class="message">%s</p>
                    <div class="code-container">
                        <div class="code">%s</div>
                    </div>
                    <p class="message">Este código es personal, intransferible y expira en %d minutos. No lo compartas con nadie.</p>
                    <p class="message">Si no solicitaste este código, puedes ignorar este mensaje.</p>
                    <p class="message">Visítanos en <a href="https://www.gestmusica.com">gestmusica.com</a> o contáctanos en <a href="mailto:gestmusica@gestmusica.com">gestmusica@gestmusica.com</a>.</p>
                </div>
                <div class="footer">
                    <p>Este es un mensaje automático de <strong>Gestmusica</strong></p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(mensaje, codigo, minutosExpiracion);
    }

    private String construirContenidoEmailPlain(String codigo, CodigoVerificacion.TipoVerificacion tipo) {
        String mensaje = switch (tipo) {
            case REGISTRO -> "Gracias por registrarte en Gestmusica.";
            case RECUPERACION_PASSWORD -> "Has solicitado restablecer tu contraseña.";
            case CAMBIO_EMAIL -> "Has solicitado cambiar tu email.";
        };

        return """
        %s

        Tu código es: %s

        Este código es personal, intransferible y expira en %d minutos.
        No lo compartas con nadie.

        Si no solicitaste este código, ignora este mensaje.

        Visítanos en: https://www.gestmusica.com
        Soporte: soporte@gestmusica.com
        """.formatted(mensaje, codigo, minutosExpiracion);
    }


    @Transactional
    public void marcarCodigoComoUsado(String email, String codigo) {
        log.info("Marcando código como usado para email: {}", email);

        Optional<CodigoVerificacion> codigoOpt = codigoRepository
                .findByEmailAndCodigoAndActivoTrue(email, codigo);

        if (codigoOpt.isPresent()) {
            CodigoVerificacion codigoVerificacion = codigoOpt.get();
            codigoVerificacion.setUsado(true);
            codigoRepository.save(codigoVerificacion);
            log.info("Código marcado como usado para email: {}", email);
        } else {
            log.warn("Código no encontrado para email: {}", email);
        }
    }
}