package es.musicalia.gestmusica.home;

import es.musicalia.gestmusica.agencia.AgenciasController;
import es.musicalia.gestmusica.auth.model.RegistrationForm;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.ocupacion.OcupacionDto;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.permiso.PermisoGeneralEnum;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.usuario.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

@Controller
public class HomeController {
    private Logger logger = LoggerFactory.getLogger(AgenciasController.class);

    private final UserService userService;
    private final SecurityService securityService;
    private final OcupacionService ocupacionService;
    private final PermisoService permisoService;
    public HomeController(UserService userService, SecurityService securityService, OcupacionService ocupacionService, PermisoService permisoService){
        this.userService = userService;
        this.securityService = securityService;
        this.ocupacionService = ocupacionService;
        this.permisoService = permisoService;
    }

    @GetMapping("/")
    public String home(Model model) {
        final Set<Long> idsAgenciasConfirmarOcupacion = this.permisoService.obtenerIdsAgenciaPorPermiso(this.userService.obtenerUsuarioAutenticado().getId(), PermisoAgenciaEnum.CONFIRMAR_OCUPACION.getDescripcion());
        List<OcupacionDto> listaOcupacion = this.ocupacionService.findOcupacionesDtoByAgenciaPendientes(idsAgenciasConfirmarOcupacion);
        model.addAttribute("listaOcupacionPendiente", listaOcupacion);
        return "main.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "/admin";
    }

    @GetMapping("/user")
    public String user() {
        return "/user";
    }

    @GetMapping("/login-redirect")
    public String loginRedirect() {
        return "redirect:/";
    }

    @GetMapping(value = "/login")
    public String login(Model model, String error, String logout) {
        if (error != null) {
            model.addAttribute("error", "Your username and password is invalid.");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");

        }
        SecurityContextHolder.clearContext();
        return "login";
    }

    @GetMapping("/registration")
    public String registration(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());

        return "registration";
    }

    @PostMapping("/registration")
    public String registration(Model model, @ModelAttribute("registrationForm") @Valid RegistrationForm registrationForm,
                               BindingResult bindingResult, Errors errors) {




        if (userService.usernameExists(registrationForm.getUsername().trim())){
            bindingResult.rejectValue("username", "error.username-exists", "El nombre de usuario ya existe");
        }
        if (!registrationForm.getPassword().equals(registrationForm.getRetryPassword())){
            bindingResult.rejectValue("password", "error.password-retry", "Las contraseñas no coinciden");
            bindingResult.rejectValue("retryPassword", "error.password-retry", "Las contraseñas no coinciden");
        }

        if (bindingResult.hasErrors()) {
            return "registration";
        }

        try {

            userService.saveRegistration(registrationForm);
        }
        catch (Exception e){
            bindingResult.rejectValue("error-guardado", "error.error-guardado", "Error en el guardado del registro");
            model.addAttribute("errors", errors);

            logger.error("Error en el guardado del registro", e);

        }

        return "redirect:/";
    }

    @GetMapping("/403")
    public String error403() {
        return "/error/403";
    }

}
