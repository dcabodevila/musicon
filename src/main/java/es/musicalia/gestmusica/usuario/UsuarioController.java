package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("usuarios")
public class UsuarioController {


    private final UserService userService;
    private final AccesoService accesoService;
    private final LocalizacionService localizacionService;
    private final EmailService emailService;

    public UsuarioController(UserService userService, AccesoService accesoService, LocalizacionService localizacionService, EmailService emailService){
        this.userService = userService;
        this.accesoService = accesoService;
        this.localizacionService = localizacionService;
        this.emailService = emailService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String mainUsuarios(ModelMap model) {
        model.addAttribute("listaUsuarios", this.userService.findAllUsuarioAdminListRecords());
        return "usuarios";
    }

    @GetMapping("/validar/{id}")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String validarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        final Usuario usuarioValidado = this.userService.validarUsuario(id);
        try {
            this.emailService.enviarMensajePorEmail(usuarioValidado.getEmail(), EmailTemplateEnum.USUARIO_VALIDADO);
        } catch (Exception e) {
            log.error("Error enviando correos validar usuario {}", e.getMessage());
        }


        redirectAttributes.addFlashAttribute("alertClass", "success");
        redirectAttributes.addFlashAttribute("message", "Usuario validado correctamente");

        return "redirect:/usuarios";
    }

    @GetMapping("/activar/{id}")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String toggleActivarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        this.userService.toggleActivarUsuario(id);
        redirectAttributes.addFlashAttribute("alertClass", "success");
        redirectAttributes.addFlashAttribute("message", "Estado del usuario actualizado correctamente");
        return "redirect:/usuarios";
    }

    @GetMapping("/{id}")
    public String verUsuario(@AuthenticationPrincipal CustomAuthenticatedUser user,@PathVariable Long id, ModelMap model) {
        model.addAttribute("usuarioEdicionDTO", this.userService.getUsuarioEdicionDTO(id));
        model.addAttribute("permisoVerDatosUsuario", this.accesoService.isUsuarioAccesoEnAgencia(user.getMapPermisosAgencia().keySet(), id));
        return "usuario-edit";
    }

    @GetMapping("/editar/{id}")
    public String editarUsuario(@PathVariable Long id, ModelMap model) {
        model.addAttribute("usuarioEdicionDTO", this.userService.getUsuarioEdicionDTO(id));
        model.addAttribute("listaAccesos", this.accesoService.findAllAccesosDetailRecordByIdUsuario(id));
        model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());

        return "usuario-detail-edit";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@Valid @ModelAttribute UsuarioEdicionDTO usuarioEdicionDTO, @RequestParam(value = "image", required = false) MultipartFile multipartFile, RedirectAttributes redirectAttributes) {
        this.userService.guardarUsuario(usuarioEdicionDTO, multipartFile);
        redirectAttributes.addFlashAttribute("alertClass", "success");
        redirectAttributes.addFlashAttribute("message", "Usuario actualizado correctamente");
        return "redirect:/usuarios/editar/" + usuarioEdicionDTO.getId();
    }

    @GetMapping("/mi-perfil")
    public String miPerfil(@AuthenticationPrincipal CustomAuthenticatedUser user, ModelMap model) {

        model.addAttribute("usuarioEdicionDTO", this.userService.getMiPerfil(user.getUserId()));
        model.addAttribute("listaAccesos", this.accesoService.getMisAccesos(user.getUserId()));
        model.addAttribute("listaProvincias", this.localizacionService.findAllProvincias());
        return "usuario-detail-edit";
    }

}
