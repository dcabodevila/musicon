package es.musicsoft.musicon.usuario;

import es.musicsoft.musicon.auth.model.RegistrationForm;
import es.musicsoft.musicon.auth.model.SecurityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("usuarios")
public class UsuarioController {


    private UserService userService;
    private SecurityService securityService;

    public UsuarioController(UserService userService, SecurityService securityService){
        this.userService = userService;
        this.securityService = securityService;

    }

    @GetMapping("/")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String mainUsuarios(ModelMap model) {

        return "usuarios";
    }




}
