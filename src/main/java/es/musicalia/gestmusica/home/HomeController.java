package es.musicalia.gestmusica.home;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.agencia.publicacioneventos.AgenciaPublicacionEventosService;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.usuario.TipoUsuarioEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class HomeController {

    private final OcupacionService ocupacionService;
    private final AgenciaPublicacionEventosService agenciaPublicacionEventosService;

    public HomeController(OcupacionService ocupacionService,
                          AgenciaPublicacionEventosService agenciaPublicacionEventosService){
        this.ocupacionService = ocupacionService;
        this.agenciaPublicacionEventosService = agenciaPublicacionEventosService;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model) {
        final boolean isAdminHome = isAdminHome(user);

        final boolean isUsuarioValidado = user.getUsuario().isValidado();
        model.addAttribute("isUsuarioValidado", isUsuarioValidado);
        model.addAttribute("isAdminHome", isAdminHome);
        if (!isUsuarioValidado){
            model.addAttribute("message", "Tu cuenta de usuario está siendo validada por los administradores, mientras tanto, puedes echar un vistazo a las características de festia.");
            model.addAttribute("alertClass", "success");

        }

        // Banner de onboarding de agencia
        boolean mostrarBannerOnboardingAgencia =
            user.getUsuario().getTipoUsuario() == TipoUsuarioEnum.AGENCIA
            && user.getUsuario().isValidado()
            && user.getMapPermisosAgencia().isEmpty();
        model.addAttribute("mostrarBannerOnboardingAgencia", mostrarBannerOnboardingAgencia);
        model.addAttribute("mostrarModalPublicacionEventosAgencia",
                agenciaPublicacionEventosService.debeMostrarModal(user.getUsuario().getId()));

        model.addAttribute("isUsuarioAutenticado", !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser"));
        if (isAdminHome) {
            model.addAttribute("listaOcupacionPendiente", Collections.emptyList());
            return "main.html";
        }

        final Map<Long, Set<String>> mapPermisosAgencia =
                user.getMapPermisosAgencia().entrySet().stream()
                        .filter(entry -> entry.getValue().contains(PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        model.addAttribute("listaOcupacionPendiente", this.ocupacionService.findOcupacionesDtoByAgenciaPendientes(mapPermisosAgencia.keySet()));
        return "main.html";
    }

    private boolean isAdminHome(CustomAuthenticatedUser user) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> "ACCESO_PANEL_ADMIN".equals(authority.getAuthority()));
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
