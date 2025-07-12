package es.musicalia.gestmusica.representantes;

import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.UsuarioEdicionDTO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("representantes")
public class RepresentantesController {


    private final UserService userService;

    public RepresentantesController(UserService userService){
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String mainUsuarios(ModelMap model) {
        model.addAttribute("listaRepresentantes", this.userService.findAllUsuarioAdminListRecords());
        return "representantes";
    }


    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String editarUsuario(@PathVariable Long id, ModelMap model) {
        model.addAttribute("usuarioEdicionDTO", this.userService.getUsuarioEdicionDTO(id));
        return "usuario-detail";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAuthority('USUARIOS')")
    public String guardarUsuario(@Valid @ModelAttribute UsuarioEdicionDTO usuarioEdicionDTO, @RequestParam(value = "image", required = false) MultipartFile multipartFile, RedirectAttributes redirectAttributes) {
        this.userService.guardarUsuario(usuarioEdicionDTO, multipartFile);
        redirectAttributes.addFlashAttribute("alertClass", "success");
        redirectAttributes.addFlashAttribute("message", "Usuario actualizado correctamente");
        return "redirect:/usuarios/editar/" + usuarioEdicionDTO.getId();
    }

    @GetMapping("/mi-perfil")
    public String miPerfil(ModelMap model) {

        model.addAttribute("usuarioEdicionDTO", this.userService.getMiPerfil());
        model.addAttribute("roleEnumMap", RolEnum.getRoleEnumMap());
        return "usuario-detail-edit";
    }

}
