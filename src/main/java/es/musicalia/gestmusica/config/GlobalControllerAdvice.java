package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.cloudinary.CloudinaryException;
import es.musicalia.gestmusica.mensaje.MensajeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Set;

@Slf4j
@ControllerAdvice
public class GlobalControllerAdvice {

    private final MensajeService mensajeService;
    @Value("${spring.application.version}")
    private String currentVersion;
    
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    public GlobalControllerAdvice(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal CustomAuthenticatedUser user , Model model) {

            if (user != null) {
                model.addAttribute("imagenUsuarioAutenticado", user.getUsuario().getImagen());

                model.addAttribute("misAgencias", user.getMapPermisosAgencia().keySet());
                final Map<Long, Set<String>> mapPermisosArtista = user.getMapPermisosArtista();
                model.addAttribute("misArtistas", mapPermisosArtista.keySet());
                boolean hasPermisoOcupaciones = mapPermisosArtista.values().stream()
                        .anyMatch(permisos -> permisos != null && permisos.contains("OCUPACIONES"));

                model.addAttribute("hasPermisoOcupaciones", hasPermisoOcupaciones);
                model.addAttribute("mensajesNoLeidos", this.mensajeService.obtenerMensajesRecibidos(user.getUserId()));
                model.addAttribute("currentVersion", currentVersion);


            }
    }

    /**
     * Maneja excepciones de Cloudinary que no fueron capturadas por el controller.
     * Redirige al home con un mensaje de error amigable para el usuario.
     */
    @ExceptionHandler(CloudinaryException.class)
    public String handleCloudinaryException(CloudinaryException ex, RedirectAttributes redirectAttributes) {
        log.error("Error de Cloudinary no manejado: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("message", ex.getMessage());
        redirectAttributes.addFlashAttribute("alertClass", "danger");
        return "redirect:/";
    }

    /**
     * Maneja archivos que exceden el límite configurado en Spring (max-file-size).
     * Se dispara ANTES de llegar al controller gracias a resolve-lazily=true.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, 
                                               RedirectAttributes redirectAttributes) {
        log.warn("Archivo excede el límite de {} configurado en Spring", maxFileSize);
        redirectAttributes.addFlashAttribute("message", 
                String.format("El archivo es demasiado grande. El tamaño máximo permitido es %s. "
                        + "Por favor, reducí el tamaño del archivo antes de subirlo.", maxFileSize));
        redirectAttributes.addFlashAttribute("alertClass", "danger");
        return "redirect:/";
    }

}