package es.musicalia.gestmusica.home;

import es.musicalia.gestmusica.agencia.AgenciasController;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.RegistrationForm;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.permiso.PermisoGeneralEnum;
import es.musicalia.gestmusica.usuario.CodigoVerificacion;
import es.musicalia.gestmusica.usuario.CodigoVerificacionService;
import es.musicalia.gestmusica.usuario.EmailYaExisteException;
import es.musicalia.gestmusica.usuario.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class HomeController {

    private final OcupacionService ocupacionService;

    public HomeController(OcupacionService ocupacionService){
        this.ocupacionService = ocupacionService;
    }

    @GetMapping("/")
    public String home(Model model) {
        final Map<Long, Set<String>> mapPermisosAgencia =
                ((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                        .getMapPermisosAgencia().entrySet().stream()
                        .filter(entry -> entry.getValue().contains(PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        model.addAttribute("listaOcupacionPendiente", this.ocupacionService.findOcupacionesDtoByAgenciaPendientes(mapPermisosAgencia.keySet()));
        return "main.html";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ACCESO_PANEL_ADMIN')")
    public String admin() {
        return "/admin";
    }

    @GetMapping("/user")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String user() {
        return "/user";
    }

    @GetMapping("/403")
    public String error403() {
        return "/error/403";
    }

}
