package es.musicalia.gestmusica.fecha;


import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.tarifa.TarifaDto;
import es.musicalia.gestmusica.usuario.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping(value="fecha")
public class FechaController {


    private final UserService userService;
    private final SecurityService securityService;
    private final FechaService fechaService;




    private Logger logger = LoggerFactory.getLogger(FechaController.class);

    public FechaController(UserService userService, SecurityService securityService, FechaService fechaService){
        this.userService = userService;
        this.securityService = securityService;
        this.fechaService = fechaService;
    }

    @GetMapping("/list/{id}")
//    @PreAuthorize("hasAuthority('" + Constantes.Permisos.SOLICITUDES_RESERVA_CONSULTAR + "')")
    @ResponseBody
    public List<FechaDto> listaTarifas(@PathVariable("id") Long idArtista, @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        //TODO: Comprobar permisos sobre el artista

        return this.fechaService.findFechaDtoByArtistaId(idArtista, start, end);

    }



}
