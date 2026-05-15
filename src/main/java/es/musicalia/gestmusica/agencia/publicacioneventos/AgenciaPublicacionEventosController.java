package es.musicalia.gestmusica.agencia.publicacioneventos;

import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/api/agencia/publicacion-eventos")
public class AgenciaPublicacionEventosController {

    private final AgenciaPublicacionEventosService agenciaPublicacionEventosService;
    private final UserService userService;

    public AgenciaPublicacionEventosController(AgenciaPublicacionEventosService agenciaPublicacionEventosService,
                                               UserService userService) {
        this.agenciaPublicacionEventosService = agenciaPublicacionEventosService;
        this.userService = userService;
    }

    @GetMapping("/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> check() {
        Usuario usuario = userService.obtenerUsuarioAutenticado().orElseThrow();
        boolean shouldShow = agenciaPublicacionEventosService.debeMostrarModal(usuario.getId());
        return ResponseEntity.ok(Map.of(
                "shouldShow", shouldShow,
                "agencias", agenciaPublicacionEventosService.findAgenciasPendientesModal(usuario.getId())
        ));
    }

    @PostMapping("/activar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> activar() {
        Usuario usuario = userService.obtenerUsuarioAutenticado().orElseThrow();
        agenciaPublicacionEventosService.activarPublicacionEventos(usuario.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/rechazar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rechazar() {
        Usuario usuario = userService.obtenerUsuarioAutenticado().orElseThrow();
        agenciaPublicacionEventosService.rechazarPublicacionEventos(usuario.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/desactivar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> desactivar() {
        Usuario usuario = userService.obtenerUsuarioAutenticado().orElseThrow();
        agenciaPublicacionEventosService.desactivarPublicacionEventos(usuario.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
