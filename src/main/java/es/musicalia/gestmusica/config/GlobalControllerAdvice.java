package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.HashSet;
import java.util.Set;

import static es.musicalia.gestmusica.util.GestmusicaUtils.isUserAutheticated;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

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