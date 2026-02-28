package es.musicalia.gestmusica.releasenotes;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

        String htmlContent;
        try {
            String resourcePath = "release-notes/release-notes-" + version + ".md";
            ClassPathResource resource = new ClassPathResource(resourcePath);
            String markdown = resource.getContentAsString(StandardCharsets.UTF_8);

            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdown);
            htmlContent = HtmlRenderer.builder().build().render(document);
        } catch (IOException e) {
            log.warn("No se encontró release notes para la versión {}", version);
            htmlContent = "<p>No hay release notes disponibles para la versión <strong>" + version + "</strong>.</p>";
        }

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
}