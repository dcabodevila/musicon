package es.musicalia.gestmusica.actividad;


import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping(value="actividad")
public class ActividadController {


    private final ActividadService actividadService;

    public ActividadController(ActividadService actividadService){

        this.actividadService = actividadService;
    }

    @GetMapping
    public String actividad(@AuthenticationPrincipal CustomAuthenticatedUser user,
                          Model model) {
        model.addAttribute("actividadTarifas", this.actividadService.findActividadTarifas());
        model.addAttribute("actividadOcupaciones", this.actividadService.findActividadOcupaciones());


        return "actividad";
    }





}
