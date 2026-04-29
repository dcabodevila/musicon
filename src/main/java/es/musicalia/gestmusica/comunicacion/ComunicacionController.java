package es.musicalia.gestmusica.comunicacion;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.CcaaRepository;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.rol.RolRecord;
import es.musicalia.gestmusica.rol.RolRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller para gestionar comunicaciones masivas a usuarios.
 * Permite filtrar usuarios por CCAA, provincia y rol, y enviarles
 * comunicaciones por email usando el editor Quill.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('COMUNICACIONES')")
public class ComunicacionController {

    private final ComunicacionService comunicacionService;
    private final UsuarioRepository usuarioRepository;
    private final LocalizacionService localizacionService;
    private final CcaaRepository ccaaRepository;
    private final RolRepository rolRepository;

    /**
     * Página principal de comunicaciones con filtros.
     */
    @GetMapping("/comunicaciones")
    public String mostrarPaginaComunicaciones(ModelMap model) {
        cargarDatosFiltros(model);
        return "comunicaciones";
    }

    /**
     * Filtra usuarios según criterios seleccionados.
     */
    @PostMapping("/comunicaciones/filtrar")
    public String filtrarUsuarios(
            @RequestParam(required = false) Long ccaaId,
            @RequestParam(required = false) Long provinciaId,
            @RequestParam(required = false) String rolCodigo,
            ModelMap model) {

        // Convertir string vacío a null para que la query JPQL ignore el filtro
        String rolCodigoLimpio = (rolCodigo != null && !rolCodigo.isBlank()) ? rolCodigo : null;

        List<Usuario> usuariosFiltrados = usuarioRepository.findUsuariosParaComunicacion(ccaaId, provinciaId, rolCodigoLimpio);

        model.addAttribute("listaUsuarios", usuariosFiltrados);
        model.addAttribute("ccaaIdSeleccionada", ccaaId);
        model.addAttribute("provinciaIdSeleccionada", provinciaId);
        model.addAttribute("rolCodigoSeleccionado", rolCodigo);
        model.addAttribute("totalUsuarios", usuariosFiltrados.size());

        cargarDatosFiltros(model);

        log.info("Filtrados {} usuarios para comunicación (CCAA: {}, Provincia: {}, Rol: {})",
                usuariosFiltrados.size(), ccaaId, provinciaId, rolCodigo);

        return "comunicaciones";
    }

    /**
     * Página de redacción de mensaje para usuarios seleccionados.
     */
    @GetMapping("/comunicaciones/redactar")
    public String mostrarPaginaRedactar(
            @RequestParam(required = false, defaultValue = "") String ids,
            ModelMap model) {

        List<Long> userIds = parseIds(ids);
        List<Usuario> usuariosSeleccionados = usuarioRepository.findAllByIdWithRelaciones(userIds);

        model.addAttribute("usuariosSeleccionados", usuariosSeleccionados);
        model.addAttribute("totalUsuarios", usuariosSeleccionados.size());
        model.addAttribute("ids", ids);

        return "comunicaciones-redactar";
    }

    /**
     * Envía la comunicación a los usuarios seleccionados.
     */
    @PostMapping("/comunicaciones/enviar")
    public String enviarComunicacion(
            @RequestParam String ids,
            @RequestParam String asunto,
            @RequestParam String htmlBody,
            @RequestParam String textoPlano,
            RedirectAttributes redirectAttributes) {

        List<Long> userIds = parseIds(ids);

        if (userIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("alertClass", "danger");
            redirectAttributes.addFlashAttribute("message", "Debe seleccionar al menos un destinatario");
            return "redirect:/comunicaciones";
        }

        if (userIds.size() > 100) {
            redirectAttributes.addFlashAttribute("alertClass", "danger");
            redirectAttributes.addFlashAttribute("message", "No se pueden enviar más de 100 comunicaciones a la vez. Seleccionados: " + userIds.size());
            return "redirect:/comunicaciones";
        }

        ComunicacionResult resultado = comunicacionService.enviarComunicacion(userIds, asunto, htmlBody, textoPlano);

        String alertClass = resultado.fallidos() > 0 || resultado.excluidosBaja() > 0 ? "warning" : "success";
        String mensaje = String.format("Comunicación enviada: %d exitosos, %d fallidos, %d excluidos por baja",
                resultado.enviados(), resultado.fallidos(), resultado.excluidosBaja());

        redirectAttributes.addFlashAttribute("alertClass", alertClass);
        redirectAttributes.addFlashAttribute("message", mensaje);

        log.info("Comunicación enviada: {} exitosos, {} fallidos, {} excluidos por baja",
                resultado.enviados(), resultado.fallidos(), resultado.excluidosBaja());

        return "redirect:/comunicaciones";
    }

    /**
     * Carga los datos necesarios para los dropdowns de filtros.
     */
    private void cargarDatosFiltros(ModelMap model) {
        List<CodigoNombreRecord> listaCcaa = ccaaRepository.findAllCcaaOrderedByName();
        List<CodigoNombreRecord> listaProvincias = localizacionService.findAllProvincias();
        // Solo mostrar roles relevantes para comunicaciones masivas
        List<String> rolesPermitidos = List.of("ARTISTA", "AGENCIA", "AGENTE");
        List<RolRecord> listaRoles = rolRepository.findAllByCodigos(rolesPermitidos);

        model.addAttribute("listaCcaa", listaCcaa);
        model.addAttribute("listaProvincias", listaProvincias);
        model.addAttribute("listaRoles", listaRoles);
    }

    /**
     * Parsea una cadena de IDs separados por comas en una lista de Long.
     */
    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(ids.split(","))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
