package es.musicalia.gestmusica.ajustes;


import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaDto;
import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.listado.ListadoDto;
import es.musicalia.gestmusica.listado.ListadoService;
import es.musicalia.gestmusica.listado.TipoOcupacionEnum;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.Date;


@Controller
@RequestMapping(value="ajustes")
public class AjustesController {


    private Logger logger = LoggerFactory.getLogger(AjustesController.class);
    private final UserService userService;
    private final LocalizacionService localizacionService;
    private final AgenciaService agenciaService;
    private final ArtistaService artistaService;
    private final AjustesService ajustesService;
    public AjustesController(UserService userService, LocalizacionService localizacionService, ArtistaService artistaService,
                             AgenciaService agenciaService, AjustesService ajustesService){

        this.userService = userService;
        this.localizacionService = localizacionService;
        this.artistaService = artistaService;
        this.agenciaService = agenciaService;
        this.ajustesService = ajustesService;
    }

    @GetMapping
    public String ajustes(Model model) {

        final Usuario usuario = userService.obtenerUsuarioAutenticado();
        model.addAttribute("ajustesDto", this.ajustesService.getAjustesByIdUsuario(usuario.getId()));
        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());
        model.addAttribute("listaTipoArtista", this.artistaService.listaTipoArtista());
        model.addAttribute("listaAgencias", this.agenciaService.listaAgenciasRecordActivasTarifasPublicas());

        return "ajustes";
    }

    @PostMapping("/guardar")
    public String guardarAgencia(Model model, @ModelAttribute("ajustesDto") @Valid AjustesDto ajustesDto,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Errors errors) {

        if (bindingResult.hasErrors()) {
            return "ajustes";
        }

        try {
            final Ajustes ajustes = this.ajustesService.saveAjustesDto(ajustesDto, this.userService.obtenerUsuarioAutenticado());

            redirectAttributes.addFlashAttribute("message", "Ajustes guardados correctamente");
            redirectAttributes.addFlashAttribute("alertClass", "success");
            return "redirect:/ajustes";

        } catch (Exception e){
            logger.error("Error guardando agencia", e);
            model.addAttribute("message", "Error guardando ajustes");
            model.addAttribute("alertClass", "danger");
            return "ajustes";
        }


    }





    }
