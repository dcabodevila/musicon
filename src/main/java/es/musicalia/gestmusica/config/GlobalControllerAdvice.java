package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal CustomAuthenticatedUser user , Model model) {

            if (user != null) {
                model.addAttribute("imagenUsuarioAutenticado", user.getUsuario().getImagen());
                model.addAttribute("misAgencias", user.getMapPermisosAgencia().keySet());
                model.addAttribute("misArtistas", user.getMapPermisosArtista().keySet());
            }
    }

}