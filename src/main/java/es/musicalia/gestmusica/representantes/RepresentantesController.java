package es.musicalia.gestmusica.representantes;

import es.musicalia.gestmusica.usuario.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("representantes")
public class RepresentantesController {


    private final UserService userService;

    public RepresentantesController(UserService userService){
        this.userService = userService;
    }

    @GetMapping
    public String getRepresentantes(ModelMap model) {
        model.addAttribute("listaUsuarios", this.userService.findAllRepresentanteRecords());
        return "representantes";
    }


}
