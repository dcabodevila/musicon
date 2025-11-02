package es.musicalia.gestmusica.actividad;


import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.usuario.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping(value="actividad")
public class ActividadController {


    private final ActividadService actividadService;
    private final SessionRegistry sessionRegistry;
    private final UserService userService;


    public ActividadController(ActividadService actividadService, SessionRegistry sessionRegistry, UserService userService){

        this.actividadService = actividadService;
        this.sessionRegistry = sessionRegistry;
        this.userService = userService;
    }

    @GetMapping
    public String actividad(@AuthenticationPrincipal CustomAuthenticatedUser user,
                          Model model) {
        model.addAttribute("actividadTarifas", this.actividadService.findActividadTarifas());
        model.addAttribute("actividadOcupaciones", this.actividadService.findActividadOcupaciones());
        model.addAttribute("usuariosConectados", obtenerUsuariosConectados());
        model.addAttribute("usuarios", this.userService.findAllUsuarioRecords());
        return "actividad";
    }


    private List<UsuarioConectadoDto> obtenerUsuariosConectados() {
        List<UsuarioConectadoDto> usuariosConectados = new ArrayList<>();

        if (sessionRegistry != null) {
            List<Object> principals = sessionRegistry.getAllPrincipals();

            for (Object principal : principals) {
                if (principal instanceof CustomAuthenticatedUser) {
                    CustomAuthenticatedUser user = (CustomAuthenticatedUser) principal;
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);

                    for (SessionInformation session : sessions) {
                        if (!session.isExpired()) {
                            UsuarioConectadoDto usuarioDto = new UsuarioConectadoDto();
                            usuarioDto.setNombre(user.getUsername());
                            usuarioDto.setNombreCompleto(user.getUsername());
                            usuarioDto.setUltimaActividad(
                                    LocalDateTime.ofInstant(session.getLastRequest().toInstant(), ZoneId.systemDefault())
                            );
                            usuarioDto.setSessionId(session.getSessionId());

                            usuariosConectados.add(usuarioDto);
                        }
                    }
                }
            }
        }

        return usuariosConectados;
    }




}
