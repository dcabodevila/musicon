package es.musicalia.gestmusica.acceso;


import es.musicalia.gestmusica.accesoartista.AccesoArtista;
import es.musicalia.gestmusica.accesoartista.AccesoArtistaDto;
import es.musicalia.gestmusica.accesoartista.AccesoArtistaService;
import es.musicalia.gestmusica.agencia.AgenciasController;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.permiso.PermisoRecord;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.rol.RolRecord;
import es.musicalia.gestmusica.usuario.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping(value="accesos")
public class AccesoController {
    private Logger logger = LoggerFactory.getLogger(AccesoController.class);


    private final AccesoService accesoService;
    private final UserService userService;
    private final AccesoArtistaService accesoPermisoService;
    private final ArtistaService artistaService;

    public AccesoController(AccesoService accesoService, UserService userService, AccesoArtistaService accesoPermisoService, ArtistaService artistaService) {
        this.accesoService = accesoService;
        this.userService = userService;
        this.accesoPermisoService = accesoPermisoService;
        this.artistaService = artistaService;
    }

    @PreAuthorize("hasPermission(#idAgencia, 'AGENCIA', 'GESTION_ACCESOS')")
    @GetMapping("/{idAgencia}")
    public String getAccesos(Model model, @PathVariable("idAgencia") Long idAgencia){
        model.addAttribute("listaAccesos", this.accesoService.listaAccesosAgencia(idAgencia));
        final AccesoDto accesoDto = new AccesoDto();
        accesoDto.setIdAgencia(idAgencia);
        model.addAttribute("accesoDto", accesoDto);
        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioRecords());
        final List<RolRecord> listaRoles = this.accesoService.obtenerRolesAgencia();
        model.addAttribute("listaRoles", listaRoles);

        model.addAttribute("listaAccesosArtista", this.accesoPermisoService.listaAccesosArtistaAgencia(idAgencia));
        model.addAttribute("accesoArtistaDto", new AccesoArtistaDto());
        model.addAttribute("listaPermisos", this.accesoPermisoService.obtenerPermisosTipoArtista());
        model.addAttribute("listaArtistas", this.artistaService.listaArtistaRecordByIdAgencia(idAgencia));

        return "accesos";
    }
    @PreAuthorize("hasPermission(#accesoDto.idAgencia, 'AGENCIA', 'GESTION_ACCESOS')")
    @PostMapping("/guardar")
    public String guardarAcceso(@Valid @ModelAttribute AccesoDto accesoDto, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        accesoService.guardarAcceso(accesoDto);
        redirectAttributes.addFlashAttribute("message", "Acceso guardado correctamente");

        return "redirect:/accesos/"+ accesoDto.getIdAgencia();

    }
    @GetMapping("/eliminar/{idAcceso}")
    public ResponseEntity<String> eliminarAcceso(@PathVariable("idAcceso") Long idAcceso) {
        try {
            accesoService.eliminarAcceso(idAcceso);
            return ResponseEntity.ok("Acceso eliminado correctamente.");
        } catch (Exception e){
            logger.error("Error elminando el acceso {}", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el acceso");
        }

    }

    //@PreAuthorize("hasPermission(#accesoDto.idAgencia, 'AGENCIA', 'GESTION_ACCESOS')")
    @PostMapping("/guardar-acceso-artista")
    public String guardarAccesoArtista(@Valid @ModelAttribute AccesoArtistaDto accesoDto, BindingResult bindingResult, RedirectAttributes redirectAttributes) {

        final AccesoArtista accesoArtista = accesoPermisoService.guardarAcceso(accesoDto);
        redirectAttributes.addFlashAttribute("message", "Acceso guardado correctamente");

        return "redirect:/accesos/"+ accesoArtista.getArtista().getAgencia().getId();

    }
    @GetMapping("/eliminar-acceso-artista/{idAccesoArtista}")
    public ResponseEntity<String> eliminarAccesoArtista(@PathVariable("idAccesoArtista") Long idAccesoArtista) {
        try {
            accesoPermisoService.eliminarAccesoArtista(idAccesoArtista);
            return ResponseEntity.ok("Acceso eliminado correctamente.");
        } catch (Exception e){
            logger.error("Error elminando el acceso {}", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el acceso");
        }

    }
}
