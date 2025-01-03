package es.musicalia.gestmusica.agencia;


import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.util.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;


@Controller
@RequestMapping(value="agencia")
public class AgenciasController {


    private UserService userService;
    private SecurityService securityService;
    private LocalizacionService localizacionService;
    private FileService fileService;
    private AgenciaService agenciaService;
    private ArtistaService artistaService;

    private Logger logger = LoggerFactory.getLogger(AgenciasController.class);

    public AgenciasController(UserService userService, SecurityService securityService, LocalizacionService localizacionService, AgenciaService agenciaService, FileService fileService,
                              ArtistaService artistaService){
        this.userService = userService;
        this.securityService = securityService;
        this.localizacionService = localizacionService;
        this.agenciaService = agenciaService;
        this.fileService = fileService;
        this.artistaService = artistaService;

    }

    @GetMapping
    public String agencias(Model model) {
        if (userService.isUserAutheticated()){
            final Usuario usuario = userService.obtenerUsuarioAutenticado();
            model.addAttribute("listaAgencias", this.agenciaService.findAllAgenciasForUser(usuario));
        }
        return "agencias";
    }

    @GetMapping("/crear")
    public String crearAgencias(Model model) {
        model.addAttribute("agenciaDto", new AgenciaDto());
        model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioRecords());

        return "agencia-detail-edit";
    }
    @GetMapping("/edit/{id}")
    public String detalleEditarAgencia(Model model, @PathVariable("id") Long idAgencia) {
        model.addAttribute("agenciaDto", this.agenciaService.findAgenciaDtoById(idAgencia));
        model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioRecords());

        return "agencia-detail-edit";
    }
    @GetMapping("/{id}")
    public String detalleAgencia(Model model, @PathVariable("id") Long idAgencia) {
        model.addAttribute("agenciaDto", this.agenciaService.findAgenciaDtoById(idAgencia));
        model.addAttribute("listaArtistas", this.artistaService.findAllArtistasByAgenciaId(idAgencia));
        return "agencia-detail";
    }

    @PostMapping("/guardar")
    public String guardarAgencia(Model model, @ModelAttribute("agenciaDto") @Valid AgenciaDto agenciaDto,  @RequestParam(value = "image", required = false) MultipartFile multipartFile,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Errors errors) {

        if (bindingResult.hasErrors()) {
            return "agencia-detail-edit";
        }

        try {
            final Agencia agencia = this.agenciaService.saveAgencia(agenciaDto);
            String uploadDir = "image/agencia-photos/" + agencia.getId();
            final String uploadedFile = this.fileService.guardarFichero(multipartFile);

            if (uploadedFile!=null){
                agenciaDto.setLogo(uploadedFile);
                this.agenciaService.saveAgencia(agenciaDto);
            }

            redirectAttributes.addFlashAttribute("message", "Agencia guardada correctamente");
            redirectAttributes.addFlashAttribute("alertClass", "success");
            return "redirect:/agencia/"+ agencia.getId();

        } catch (Exception e){
            logger.error("Error guardando agencia", e);
            model.addAttribute("message", "Error guardando agencia");
            model.addAttribute("alertClass", "danger");
            return "agencia-detail-edit";
        }


    }



}
