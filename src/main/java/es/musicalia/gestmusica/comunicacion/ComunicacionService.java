package es.musicalia.gestmusica.comunicacion;

import es.musicalia.gestmusica.mensaje.Mensaje;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.usuario.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para gestionar el envío de comunicaciones masivas a usuarios.
 * Soporta filtrado por baja de email y procesamiento asíncrono.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComunicacionService {

    private final UsuarioRepository usuarioRepository;
    private final ComunicacionChannel comunicacionChannel;
    private final MensajeService mensajeService;
    private final UserService userService;

    /**
     * Envía una comunicación a una lista de usuarios.
     * Filtra usuarios dados de baja y maneja errores individuales sin detener el proceso.
     *
     * @param usuarioIds lista de IDs de usuarios destinatarios
     * @param asunto asunto del mensaje
     * @param htmlBody contenido HTML del mensaje
     * @return resultado del envío con contadores de éxito, fallo y excluidos
     */
    @Transactional
    public ComunicacionResult enviarComunicacion(List<Long> usuarioIds, String asunto, String htmlBody, String textoPlano) {
        if (usuarioIds == null || usuarioIds.isEmpty()) {
            log.debug("Lista de usuarios vacía, no se envía nada");
            return new ComunicacionResult(0, 0, 0);
        }

        log.info("Iniciando envío de comunicación a {} usuarios", usuarioIds.size());

        List<Usuario> usuarios = usuarioRepository.findAllById(usuarioIds);
        log.debug("Recuperados {} usuarios de la base de datos", usuarios.size());

        int enviados = 0;
        int fallidos = 0;
        int excluidosBaja = 0;

        Usuario remitente = userService.obtenerUsuarioAutenticado().orElse(null);

        for (Usuario usuario : usuarios) {
            try {
                if (usuario.isEmailBaja()) {
                    log.debug("Usuario {} excluido por baja de email", usuario.getId());
                    excluidosBaja++;
                    continue;
                }

                if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                    log.warn("Usuario {} no tiene email, no se puede enviar", usuario.getId());
                    fallidos++;
                    continue;
                }

                comunicacionChannel.enviar(usuario.getEmail(), asunto, htmlBody);
                enviados++;
                log.debug("Comunicación enviada a: {}", usuario.getEmail());

                // Notificación interna al destinatario
                if (remitente != null) {
                    try {
                        Mensaje mensaje = new Mensaje();
                        mensaje.setUsuarioRemite(remitente);
                        mensaje.setUsuarioReceptor(usuario);
                        mensaje.setAsunto(asunto);
                        mensaje.setMensaje(textoPlano);
                        mensaje.setImagen("fa-envelope text-primary");
                        mensajeService.enviarMensaje(mensaje, usuario.getId());
                        log.debug("Notificación interna enviada a usuario {}", usuario.getId());
                    } catch (Exception ex) {
                        log.warn("No se pudo enviar notificación interna a usuario {}: {}", usuario.getId(), ex.getMessage());
                    }
                }

                // Delay para respetar rate limits de Mailgun (5 req/segundo aprox)
                try {
                    Thread.sleep(201);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Envío interrumpido durante delay");
                }

            } catch (Exception e) {
                log.error("Error enviando comunicación a usuario {} ({}): {}",
                        usuario.getId(), usuario.getEmail(), e.getMessage());
                fallidos++;
            }
        }

        log.info("Envío completado: {} enviados, {} fallidos, {} excluidos por baja",
                enviados, fallidos, excluidosBaja);

        return new ComunicacionResult(enviados, fallidos, excluidosBaja);
    }
}
