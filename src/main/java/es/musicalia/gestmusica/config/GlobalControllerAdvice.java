package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.permiso.PermisoArtistaEnum;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @ModelAttribute("misAgencias")
    public Set<Long> getNumeroMisAgencias() {
        if (isUserAutheticated()) {

            return ((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .getMapPermisosAgencia().keySet();

        }
        return new HashSet<>();
    }

    @ModelAttribute("misArtistas")
    public Set<Long> getNumeroMisArtistas() {
        if (isUserAutheticated()) {

            return ((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .getMapPermisosArtista().keySet();

        }
        return new HashSet<>();
    }

    @ModelAttribute("artistasOcupacion")
    public Set<Long> getArtistasConPermisoOcupacion() {
        if (isUserAutheticated()) {

            return ((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .getMapPermisosArtista().entrySet().stream()
                    .filter(entry -> entry.getValue().contains(PermisoArtistaEnum.OCUPACIONES.name()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

        }
        return new HashSet<>();
    }
}

