package es.musicalia.gestmusica.home;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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
