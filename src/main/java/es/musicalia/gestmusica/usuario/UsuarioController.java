package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.auth.model.SecurityService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("usuarios")
public class UsuarioController {


    private UserService userService;
    private SecurityService securityService;

    public UsuarioController(UserService userService, SecurityService securityService){
        this.userService = userService;
        this.securityService = securityService;

    }

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String mainUsuarios(ModelMap model) {

        return "usuarios";
    }




}
