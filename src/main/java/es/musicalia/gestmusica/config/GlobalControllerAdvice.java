package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.mensaje.MensajeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;
import java.util.Set;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final MensajeService mensajeService;
    @Value("${spring.application.version}")
    private String currentVersion;

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

}