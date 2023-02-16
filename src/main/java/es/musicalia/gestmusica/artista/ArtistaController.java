package es.musicalia.gestmusica.artista;


import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.tarifa.TarifaSaveDto;
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


@Controller
@RequestMapping(value="artista")
public class ArtistaController {


    private UserService userService;
    private SecurityService securityService;
    private FileService fileService;
    private ArtistaService artistaService;
    private AgenciaService agenciaService;
    private LocalizacionService localizacionService;

    private Logger logger = LoggerFactory.getLogger(ArtistaController.class);

    public ArtistaController(UserService userService, SecurityService securityService, ArtistaService artistaService, FileService fileService, AgenciaService agenciaService,
                             LocalizacionService localizacionService){
        this.userService = userService;
        this.securityService = securityService;
        this.artistaService = artistaService;
        this.fileService = fileService;
        this.agenciaService = agenciaService;
        this.localizacionService = localizacionService;

    }

    @GetMapping
    public String artistas(Model model) {
        if (userService.isUserAutheticated()){
            final Usuario usuario = userService.obtenerUsuarioAutenticado();
            model.addAttribute("listaArtistas", this.artistaService.findAllArtistasForUser(usuario));
        }
        return "artistas";
    }

    @GetMapping("/crear")
    public String crearArtistas(Model model) {
        model.addAttribute("artistaDto", new ArtistaDto());
        getModelAttributeDetail(model);
        return "artista-detail-edit";
    }

    private void getModelAttributeDetail(Model model) {

        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioRecords());
        model.addAttribute("listaAgencias", this.agenciaService.findAllAgenciasForUser(userService.obtenerUsuarioAutenticado()));
        model.addAttribute("listaTipoArtista", this.artistaService.listaTipoArtista());
        model.addAttribute("listaTipoEscenario", this.artistaService.listaTipoEscenario());
        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());

    }

    @GetMapping("/{id}")
    public String detalleArtista(Model model, @PathVariable("id") Long idArtista) {
        model.addAttribute("artistaDto", this.artistaService.findArtistaDtoById(idArtista));
        getModelAttributeDetail(model);

        return "artista-detail";
    }

    @GetMapping("/edit/{id}")
    public String editArtista(Model model, @PathVariable("id") Long idArtista) {
        model.addAttribute("artistaDto", this.artistaService.findArtistaDtoById(idArtista));
        getModelAttributeDetail(model);

        return "artista-detail-edit";
    }

    @PostMapping("/guardar")
    public String guardarArtista(Model model, @ModelAttribute("artistaDto") @Valid ArtistaDto artistaDto, @RequestParam(value = "image", required = false) MultipartFile multipartFile,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Errors errors) {

        if (bindingResult.hasErrors()) {
            return "artista-detail";
        }

        try {
            final Artista artista = this.artistaService.saveArtista(artistaDto);
            artistaDto.setId(artista.getId());
            String uploadDir = "image/artista-photos/" + artista.getId();
            final String uploadedFile = this.fileService.guardarFichero(multipartFile, uploadDir);

            if (uploadedFile!=null){
                artistaDto.setLogo(uploadedFile);
                this.artistaService.saveArtista(artistaDto);
            }

            redirectAttributes.addFlashAttribute("message", "Artista guardado correctamente");
            redirectAttributes.addFlashAttribute("alertClass", "success");
            return "redirect:/artista/"+ artista.getId();

        } catch (Exception e){
            logger.error("Error guardando artista", e);
            model.addAttribute("message", "Error guardando artista");
            model.addAttribute("alertClass", "danger");
            getModelAttributeDetail(model);
            return "artista-detail";
        }


    }



}
