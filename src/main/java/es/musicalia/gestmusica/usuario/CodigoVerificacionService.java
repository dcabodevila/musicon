package es.musicalia.gestmusica.usuario;


import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
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


    /**
     * Genera y envía un código de verificación de 4 dígitos
     */
    @Transactional
    public void generarYEnviarCodigo(String email, EmailTemplateEnum tipo) throws EnvioEmailException {
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
        this.emailService.enviarCodigoPorEmail(email, codigo, tipo);

        log.info("Código de verificación generado y enviado para: {}", email);
    }

    /**
     * Verifica si un código es válido
     */
    @Transactional
    public boolean verificarCodigo(String email, String codigo, EmailTemplateEnum tipo, boolean usado) {
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
    public boolean existeCodigoValido(String email, EmailTemplateEnum tipo) {
        Optional<CodigoVerificacion> codigo = codigoRepository
                .findCodigoValidoByEmailAndTipo(email, tipo, LocalDateTime.now());
        return codigo.isPresent();
    }

    /**
     * Reenvía código de verificación si es válido hacerlo
     */
    @Transactional
    public boolean reenviarCodigo(String email, EmailTemplateEnum tipo) throws EnvioEmailException {
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

            this.emailService.enviarCodigoPorEmail(email, codigo.getCodigo(), tipo);
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