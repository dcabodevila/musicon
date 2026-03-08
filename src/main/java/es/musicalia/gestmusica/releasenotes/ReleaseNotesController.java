package es.musicalia.gestmusica.releasenotes;

import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Controller
@RequestMapping("/release-notes")
public class ReleaseNotesController {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private static final Pattern FILENAME_VERSION_PATTERN = Pattern.compile("release-notes-([\\d.]+)\\.md$");

    @Value("${spring.application.version}")
    private String currentVersion;
    
    private final ReleaseNotesService releaseNotesService;
    private final UserService userService;

    public ReleaseNotesController(ReleaseNotesService releaseNotesService, UserService userService) {
        this.releaseNotesService = releaseNotesService;
        this.userService = userService;
    }

    @GetMapping
    public String releaseNotesActual(Model model) {
        return releaseNotesPorVersion(currentVersion, model);
    }

    @GetMapping("/{version}")
    public String releaseNotesPorVersion(@PathVariable String version, Model model) {
        // Validación de seguridad: solo formato x.y.z permitido
        if (!VERSION_PATTERN.matcher(version).matches()) {
            log.warn("Intento de acceso con versión inválida: {}", version);
            return "redirect:/release-notes";
        }

        // Obtener el usuario autenticado
        Usuario usuario = userService.obtenerUsuarioAutenticado().orElse(null);

        // Obtener el contenido SIN filtrar (mostrar todo para la página web)
        String htmlContent = releaseNotesService.getReleaseNotesContent(version, usuario, false);

        model.addAttribute("version", version);
        model.addAttribute("releaseNotesHtml", htmlContent);
        model.addAttribute("versionesDisponibles", getVersionesDisponibles());
        return "release-notes";
    }

    private List<String> getVersionesDisponibles() {
        List<String> versiones = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:release-notes/release-notes-*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null) {
                    Matcher matcher = FILENAME_VERSION_PATTERN.matcher(filename);
                    if (matcher.find()) {
                        versiones.add(matcher.group(1));
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Error al listar los ficheros de release notes", e);
        }
        // Ordenar de más reciente a más antigua
        versiones.sort(Comparator.comparingInt(ReleaseNotesController::versionToInt).reversed());
        return versiones;
    }

    private static int versionToInt(String version) {
        String[] parts = version.split("\\.");
        return Integer.parseInt(parts[0]) * 10000
                + Integer.parseInt(parts[1]) * 100
                + Integer.parseInt(parts[2]);
    }

    /**
     * API REST: Verifica si el usuario actual necesita ver las release notes
     */
    @GetMapping("/api/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkReleaseNotes() {
        try {
            Usuario usuario = userService.obtenerUsuarioAutenticado()
                    .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

            String currentAppVersion = releaseNotesService.getCurrentVersion();

            // Verificar si ya las leyó
            boolean hasRead = releaseNotesService.hasReadReleaseNotes(usuario.getId(), currentAppVersion);
            if (hasRead) {
                return ResponseEntity.ok(Map.of("shouldShow", false));
            }

            // Verificar si el usuario debe ver estas release notes según su rol (frontmatter)
            boolean shouldShow = releaseNotesService.shouldShowReleaseNotes(usuario, currentAppVersion);
            if (!shouldShow) {
                return ResponseEntity.ok(Map.of("shouldShow", false));
            }

            // Verificar si hay contenido aplicable después de filtrar bloques FOR_ROLES
            boolean hasApplicableContent = releaseNotesService.hasApplicableContent(usuario, currentAppVersion);
            if (!hasApplicableContent) {
                return ResponseEntity.ok(Map.of("shouldShow", false));
            }

            // Obtener el contenido filtrado por rol del usuario (para la modal)
            String content = releaseNotesService.getReleaseNotesContent(currentAppVersion, usuario, true);

            return ResponseEntity.ok(Map.of(
                    "shouldShow", true,
                    "version", currentAppVersion,
                    "content", content
            ));

        } catch (Exception e) {
            log.error("Error verificando release notes", e);
            return ResponseEntity.ok(Map.of("shouldShow", false));
        }
    }

    /**
     * API REST: Marca las release notes como leídas
     */
    @PostMapping("/api/mark-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@RequestParam String version) {
        try {
            Usuario usuario = userService.obtenerUsuarioAutenticado()
                    .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));

            releaseNotesService.markAsRead(usuario.getId(), version);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Release notes marcadas como leídas"
            ));

        } catch (Exception e) {
            log.error("Error marcando release notes como leídas", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Error marcando release notes como leídas: " + e.getMessage()
            ));
        }
    }
}