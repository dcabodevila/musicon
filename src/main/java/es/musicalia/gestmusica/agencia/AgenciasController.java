package es.musicalia.gestmusica.agencia;


import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.usuario.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@Slf4j
@Controller
@RequestMapping(value="agencia")
public class AgenciasController {


    private final UserService userService;
    private final LocalizacionService localizacionService;
    private final FileService fileService;
    private final AgenciaService agenciaService;
    private final ArtistaService artistaService;

    public AgenciasController(UserService userService, LocalizacionService localizacionService, AgenciaService agenciaService, FileService fileService,
                              ArtistaService artistaService){
        this.userService = userService;
        this.localizacionService = localizacionService;
        this.agenciaService = agenciaService;
        this.fileService = fileService;
        this.artistaService = artistaService;
    }

    @GetMapping
    public String agencias(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model) {
        model.addAttribute("listaAgencias",this.agenciaService.findAllAgenciasForUser());

        return "agencias";
    }

    @GetMapping("/mis-agencias")
    public String misAgencias(@AuthenticationPrincipal CustomAuthenticatedUser user, Model model) {

        final Map<Long, Set<String>> mapPermisosAgencia = user.getMapPermisosAgencia();
        model.addAttribute("listaAgencias", mapPermisosAgencia.isEmpty() ? new ArrayList<>() : this.agenciaService.findMisAgencias(mapPermisosAgencia.keySet()));

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
    @PreAuthorize("hasPermission(#idAgencia, 'AGENCIA', 'AGENCIA_EDITAR')")
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
            log.error("Error guardando agencia", e);
            model.addAttribute("message", "Error guardando agencia");
            model.addAttribute("alertClass", "danger");
            return "agencia-detail-edit";
        }


    }



}
