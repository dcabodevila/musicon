package es.musicalia.gestmusica.ocupacion;


import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value="ocupacion")
public class OcupacionController {


    private UserService userService;
    private SecurityService securityService;

    private OcupacionService ocupacionService;

    private Logger logger = LoggerFactory.getLogger(OcupacionController.class);

    public OcupacionController(UserService userService, SecurityService securityService, OcupacionService ocupacionService){
        this.userService = userService;
        this.securityService = securityService;
        this.ocupacionService = ocupacionService;

    }

    @GetMapping("/get/{id}")
    public ResponseEntity<OcupacionEditDto> getOcupacionEditDtoByArtistaIdAndDates(@PathVariable long id) {

        return ResponseEntity.ok(ocupacionService.findOcupacionEditDtoByArtistaIdAndDates(id));

    }

    @PostMapping("/save")
    public ResponseEntity<?> saveOcupacion(
            @RequestBody OcupacionSaveDto ocupacionSaveDto) {

        DefaultResponseBody result = new DefaultResponseBody();

        if (ocupacionService.existeOcupacionFecha(ocupacionSaveDto)) {
            result.setSuccess(false);
            result.setMessage("Ya existe una ocupación en esa fecha");
            result.setMessageType("error");

        }
        else {
            ocupacionService.saveOcupacion(ocupacionSaveDto);
            result.setSuccess(true);
            result.setMessage("Ocupación guardada");
            result.setMessageType("success");


        }

        return ResponseEntity.ok(result);


    }


}
