package es.musicalia.gestmusica.tarifa;


import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.usuario.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping(value="tarifa")
public class TarifaController {


    private UserService userService;
    private SecurityService securityService;

    private TarifaService tarifaService;
    private ArtistaService artistaService;

    private Logger logger = LoggerFactory.getLogger(TarifaController.class);

    public TarifaController(UserService userService, SecurityService securityService,  AgenciaService agenciaService,
                            ArtistaService artistaService, TarifaService tarifaService){
        this.userService = userService;
        this.securityService = securityService;
        this.artistaService = artistaService;
        this.tarifaService = tarifaService;

    }

    @GetMapping("/list/{id}")
//    @PreAuthorize("hasAuthority('" + Constantes.Permisos.SOLICITUDES_RESERVA_CONSULTAR + "')")
    @ResponseBody
    public List<TarifaDto> listaTarifas(@PathVariable("id") Long idArtista, @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        //TODO: Comprobar permisos sobre el artista
        logger.info("start: "+ start.toString() + " end: "+ end.toString());

        final List<TarifaDto> listaTarifas = this.tarifaService.findByArtistaId(idArtista, start, end);

        return listaTarifas;

    }
}
