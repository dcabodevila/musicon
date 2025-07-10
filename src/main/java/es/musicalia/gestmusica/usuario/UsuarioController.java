package es.musicalia.gestmusica.usuario;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import es.musicalia.gestmusica.rol.RolEnum;

@Controller
@RequestMapping("usuarios")
public class UsuarioController {


    private final UserService userService;

    public UsuarioController(UserService userService){
        this.userService = userService;
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
        this.userService.validarUsuario(id);
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

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String editarUsuario(@PathVariable Long id, ModelMap model) {
        model.addAttribute("usuarioEdicionDTO", this.userService.getUsuarioEdicionDTO(id));
        model.addAttribute("roleEnumMap", RolEnum.getRoleEnumMap());
        return "usuario-detail-edit";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String guardarUsuario(@Valid @ModelAttribute UsuarioEdicionDTO usuarioEdicionDTO, @RequestParam(value = "image", required = false) MultipartFile multipartFile, RedirectAttributes redirectAttributes) {
        this.userService.guardarUsuario(usuarioEdicionDTO, multipartFile);
        redirectAttributes.addFlashAttribute("alertClass", "success");
        redirectAttributes.addFlashAttribute("message", "Usuario actualizado correctamente");
        return "redirect:/usuarios/editar/" + usuarioEdicionDTO.getId();
    }


}
