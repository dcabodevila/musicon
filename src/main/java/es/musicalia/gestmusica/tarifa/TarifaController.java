package es.musicalia.gestmusica.tarifa;


import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaDto;
import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;


@Controller
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
    public List<TarifaDto> listaTarifas(@PathVariable("id") Long idArtista) {

        //TODO: Comprobar permisos sobre el artista

        final List<TarifaDto> listaTarifas = this.tarifaService.findByArtistaId(idArtista);

        return listaTarifas;

    }
}
