package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import static es.musicalia.gestmusica.util.GestmusicaUtils.isUserAutheticated;

@ControllerAdvice
public class GlobalControllerAdvice {


    @ModelAttribute("imagenUsuarioAutenticado")
    public String addImagenUsuarioAutenticado() {
        if (isUserAutheticated()) {

            return ((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .getUsuario().getImagen();

        }
        return null;
    }
}

