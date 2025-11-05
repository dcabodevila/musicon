package es.musicalia.gestmusica.ajustes;


import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

@Slf4j
@Controller
@RequestMapping(value="ajustes")
public class AjustesController {

    private final LocalizacionService localizacionService;
    private final AgenciaService agenciaService;
    private final ArtistaService artistaService;
    private final AjustesService ajustesService;
    public AjustesController(LocalizacionService localizacionService, ArtistaService artistaService,
                             AgenciaService agenciaService, AjustesService ajustesService){

        this.localizacionService = localizacionService;
        this.artistaService = artistaService;
        this.agenciaService = agenciaService;
        this.ajustesService = ajustesService;
    }

    @GetMapping
    public String ajustes(@AuthenticationPrincipal CustomAuthenticatedUser user,
                          Model model) {

        model.addAttribute("ajustesDto", this.ajustesService.getAjustesByIdUsuario(user.getUserId()));
        model.addAttribute("listaAjustes", this.ajustesService.getAllAjustesByIdUsuario(user.getUserId()));
        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());
        model.addAttribute("listaTipoArtista", this.artistaService.listaTipoArtista());
        model.addAttribute("listaAgencias", this.agenciaService.listaAgenciasRecordActivasTarifasPublicas());

        return "ajustes";
    }

    @GetMapping("/{id}")
    public String editarAjuste(@AuthenticationPrincipal CustomAuthenticatedUser user,
                               @PathVariable Long id,
                               Model model) {

        model.addAttribute("ajustesDto", this.ajustesService.getAjustesByIdAjuste(id));
        model.addAttribute("listaAjustes", this.ajustesService.getAllAjustesByIdUsuario(user.getUserId()));
        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());
        model.addAttribute("listaTipoArtista", this.artistaService.listaTipoArtista());
        model.addAttribute("listaAgencias", this.agenciaService.listaAgenciasRecordActivasTarifasPublicas());

        return "ajustes";
    }

    @GetMapping("/nuevo")
    public String nuevoAjuste(@AuthenticationPrincipal CustomAuthenticatedUser user,
                              Model model) {

        model.addAttribute("ajustesDto", new AjustesDto());
        model.addAttribute("listaAjustes", this.ajustesService.getAllAjustesByIdUsuario(user.getUserId()));
        model.addAttribute("listaCcaa", this.localizacionService.findAllComunidades());
        model.addAttribute("listaTipoArtista", this.artistaService.listaTipoArtista());
        model.addAttribute("listaAgencias", this.agenciaService.listaAgenciasRecordActivasTarifasPublicas());

        return "ajustes";
    }


    @PostMapping("/guardar")
    public String guardarAgencia(@AuthenticationPrincipal CustomAuthenticatedUser user,Model model, @ModelAttribute("ajustesDto") @Valid AjustesDto ajustesDto,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes, Errors errors) {

        if (bindingResult.hasErrors()) {
            return "ajustes";
        }

        try {
            final Ajustes ajustes = this.ajustesService.saveAjustesDto(ajustesDto, user.getUsuario());

            redirectAttributes.addFlashAttribute("message", "Ajustes guardados correctamente");
            redirectAttributes.addFlashAttribute("alertClass", "success");
            return "redirect:/ajustes";

        } catch (Exception e){
            log.error("Error guardando agencia", e);
            model.addAttribute("message", "Error guardando ajustes");
            model.addAttribute("alertClass", "danger");
            return "ajustes";
        }


    }

    @GetMapping("/{id}/json")
    @ResponseBody
    public ResponseEntity<AjustesDto> obtenerAjusteJson(@AuthenticationPrincipal CustomAuthenticatedUser user,
                                                         @PathVariable Long id) {
        try {
            AjustesDto ajustesDto = this.ajustesService.getAjustesByIdAjuste(id);
            return ResponseEntity.ok(ajustesDto);
        } catch (Exception e) {
            log.error("Error obteniendo ajuste", e);
            return ResponseEntity.notFound().build();
        }
    }
}
