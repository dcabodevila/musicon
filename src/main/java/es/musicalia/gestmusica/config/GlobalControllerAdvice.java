package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;
import java.util.Set;

@ControllerAdvice
public class GlobalControllerAdvice {

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

            }
    }

}