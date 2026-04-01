package es.musicalia.gestmusica.releasenotes;

import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UserService;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ReleaseNotesServiceImpl implements ReleaseNotesService {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL
    );

    private static final Pattern FOR_ROLES_PATTERN = Pattern.compile(
            "<!--\\s*FOR_ROLES:\\s*([^>]+?)\\s*-->(.*?)<!--\\s*END_FOR_ROLES\\s*-->",
            Pattern.DOTALL
    );

    private final ReleaseNotesReadRepository releaseNotesReadRepository;
    private final UserService userService;
    private final ResourceLoader resourceLoader;
    private final ReleaseNotesVersionPolicy releaseNotesVersionPolicy;

    @Value("${project.version:1.0.9}")
    private String currentVersion;

    public ReleaseNotesServiceImpl(ReleaseNotesReadRepository releaseNotesReadRepository,
                                   UserService userService,
                                   ResourceLoader resourceLoader,
                                   ReleaseNotesVersionPolicy releaseNotesVersionPolicy) {
        this.releaseNotesReadRepository = releaseNotesReadRepository;
        this.userService = userService;
        this.resourceLoader = resourceLoader;
        this.releaseNotesVersionPolicy = releaseNotesVersionPolicy;
    }

    @Override
    public boolean hasReadReleaseNotes(Long usuarioId, String version) {
        Optional<String> effectiveVersion = getEffectiveVersion(version);
        if (effectiveVersion.isEmpty()) {
            log.warn("Versión inválida para hasReadReleaseNotes: {}", version);
            return false;
        }

        String effective = effectiveVersion.get();
        if (releaseNotesReadRepository.existsByUsuarioIdAndVersion(usuarioId, effective)) {
            return true;
        }

        return releaseNotesVersionPolicy.toEffectivePrefix(effective)
                .map(prefix -> releaseNotesReadRepository.existsByUsuarioIdAndVersionStartingWith(usuarioId, prefix))
                .orElse(false);
    }

    @Override
    @Transactional
    public void markAsRead(Long usuarioId, String version) {
        String effectiveVersion = getEffectiveVersion(version)
                .orElseThrow(() -> new IllegalArgumentException("Versión inválida: " + version));

        if (!hasReadReleaseNotes(usuarioId, effectiveVersion)) {
            Usuario usuario = userService.findById(usuarioId);
            ReleaseNotesRead releaseNotesRead = new ReleaseNotesRead(usuario, effectiveVersion);
            releaseNotesReadRepository.save(releaseNotesRead);
            log.info("Release notes {} marcadas como leídas para usuario {}", effectiveVersion, usuarioId);
        }
    }

    @Override
    public String getCurrentVersion() {
        return currentVersion;
    }

    @Override
    public Optional<String> getCurrentEffectiveVersion() {
        return getEffectiveVersion(currentVersion);
    }

    @Override
    public Optional<String> getEffectiveVersion(String version) {
        return releaseNotesVersionPolicy.toEffectiveVersion(version);
    }

    @Override
    public String getReleaseNotesContent(String version) {
        return getReleaseNotesContent(version, null, false);
    }

    @Override
    public String getReleaseNotesContent(String version, Usuario usuario, boolean filterByRole) {
        String effectiveVersion = getEffectiveVersion(version).orElse(version);

        try {
            String filename = "classpath:release-notes/release-notes-" + effectiveVersion + ".md";
            Resource resource = resourceLoader.getResource(filename);

            if (!resource.exists()) {
                log.warn("No se encontró el archivo de release notes para la versión {}", effectiveVersion);
                return "<p>No hay release notes disponibles para esta versión.</p>";
            }

            String markdown = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Extraer roles antes de remover el frontmatter
            List<String> roles = extractRolesFromReleaseNotes(effectiveVersion);

            // Remover frontmatter antes de convertir a HTML
            markdown = removeFrontmatter(markdown);

            // Filtrar contenido por rol del usuario si está habilitado
            if (filterByRole && usuario != null) {
                markdown = filterContentByRole(markdown, usuario);
            } else if (!filterByRole) {
                // Si no filtramos, remover solo los comentarios HTML
                markdown = markdown.replaceAll("<!--\\s*FOR_ROLES:\\s*[^>]+?\\s*-->", "")
                                  .replaceAll("<!--\\s*END_FOR_ROLES\\s*-->", "");
            }

            // Convertir Markdown a HTML
            Parser parser = Parser.builder().build();
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String html = renderer.render(parser.parse(markdown));

            // Añadir badges de roles al inicio si existen
            if (roles != null && !roles.isEmpty()) {
                html = generateRolesBadges(roles) + html;
            }

            // Ajustar rutas de imágenes relativas
            html = html.replace("src=\"/img/", "src=\"/img/");

            return html;

        } catch (IOException e) {
            log.error("Error leyendo release notes para versión {}", effectiveVersion, e);
            return "<p>Error cargando las release notes.</p>";
        }
    }

    @Override
    public boolean shouldShowReleaseNotes(Usuario usuario, String version) {
        Optional<String> effectiveVersion = getEffectiveVersion(version);
        if (effectiveVersion.isEmpty()) {
            log.warn("Versión inválida para shouldShowReleaseNotes: {}", version);
            return false;
        }

        try {
            // Los administradores ven todas las release notes
            if (usuario.getRolGeneral() != null &&
                RolEnum.ROL_ADMINISTRADOR.getCodigo().equals(usuario.getRolGeneral().getCodigo())) {
                return true;
            }

            List<String> allowedRoles = extractRolesFromReleaseNotes(effectiveVersion.get());

            // Si no hay roles especificados, mostrar a todos (retrocompatibilidad)
            if (allowedRoles == null || allowedRoles.isEmpty()) {
                return true;
            }

            // Verificar si el rol del usuario está en la lista de roles permitidos
            if (usuario.getRolGeneral() != null) {
                String userRoleCodigo = usuario.getRolGeneral().getCodigo();
                return allowedRoles.contains(userRoleCodigo);
            }

            // Si el usuario no tiene rol, no mostrar
            return false;

        } catch (Exception e) {
            log.error("Error verificando si mostrar release notes para usuario {} y versión {}",
                     usuario.getId(), effectiveVersion.get(), e);
            return false;
        }
    }

    /**
     * Extrae la lista de roles del frontmatter de las release notes
     */
    private List<String> extractRolesFromReleaseNotes(String version) {
        try {
            String filename = "classpath:release-notes/release-notes-" + version + ".md";
            Resource resource = resourceLoader.getResource(filename);

            if (!resource.exists()) {
                return Collections.emptyList();
            }

            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String frontmatterYaml = extractFrontmatter(content);

            if (frontmatterYaml == null || frontmatterYaml.isEmpty()) {
                return Collections.emptyList();
            }

            return parseRolesFromYaml(frontmatterYaml);

        } catch (IOException e) {
            log.error("Error extrayendo roles de release notes versión {}", version, e);
            return Collections.emptyList();
        }
    }

    /**
     * Extrae el contenido del frontmatter YAML
     */
    private String extractFrontmatter(String markdown) {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(markdown);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Remueve el frontmatter del contenido markdown
     */
    private String removeFrontmatter(String markdown) {
        return FRONTMATTER_PATTERN.matcher(markdown).replaceFirst("");
    }

    /**
     * Parsea los roles desde el YAML del frontmatter
     */
    @SuppressWarnings("unchecked")
    private List<String> parseRolesFromYaml(String yamlContent) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(yamlContent);

            if (data == null || !data.containsKey("roles")) {
                return Collections.emptyList();
            }

            Object rolesObj = data.get("roles");
            if (rolesObj instanceof List) {
                return (List<String>) rolesObj;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Error parseando YAML de roles", e);
            return Collections.emptyList();
        }
    }

    /**
     * Genera badges HTML para los roles aplicables
     */
    private String generateRolesBadges(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div style=\"margin-bottom: 1rem;\">");

        for (String roleCodigo : roles) {
            String roleLabel = getRoleLabelFromCodigo(roleCodigo);
            String badgeClass = getRoleBadgeClass(roleCodigo);

            html.append("<span class=\"badge ")
                .append(badgeClass)
                .append("\" style=\"margin-right: 0.5rem;\">")
                .append(roleLabel)
                .append("</span>");
        }

        html.append("</div>");
        return html.toString();
    }

    /**
     * Obtiene la etiqueta legible del rol desde su código
     */
    private String getRoleLabelFromCodigo(String codigo) {
        try {
            for (RolEnum rol : RolEnum.values()) {
                if (rol.getCodigo().equals(codigo)) {
                    return rol.getDescripcion();
                }
            }
        } catch (Exception e) {
            log.warn("Error obteniendo label del rol: {}", codigo);
        }
        return codigo; // Fallback: devolver el código si no se encuentra
    }

    /**
     * Obtiene la clase CSS del badge según el rol
     */
    private String getRoleBadgeClass(String codigo) {
        return switch (codigo) {
            case "ADMIN" -> "bg-danger";
            case "AGENCIA" -> "bg-primary";
            case "REPRE" -> "bg-success";
            case "AGENTE" -> "bg-info";
            case "ARTISTA" -> "bg-warning text-dark";
            default -> "bg-secondary";
        };
    }

    /**
     * Filtra el contenido markdown basado en los bloques FOR_ROLES
     * Solo mantiene los bloques aplicables al rol del usuario
     */
    private String filterContentByRole(String markdown, Usuario usuario) {
        // Los administradores ven todo el contenido
        boolean isAdmin = usuario.getRolGeneral() != null &&
                RolEnum.ROL_ADMINISTRADOR.getCodigo().equals(usuario.getRolGeneral().getCodigo());

        if (isAdmin) {
            // Remover solo los comentarios HTML, mantener el contenido
            return markdown.replaceAll("<!--\\s*FOR_ROLES:\\s*[^>]+?\\s*-->", "")
                          .replaceAll("<!--\\s*END_FOR_ROLES\\s*-->", "");
        }

        String userRoleCodigo = usuario.getRolGeneral() != null ?
                usuario.getRolGeneral().getCodigo() : null;

        StringBuffer result = new StringBuffer();
        Matcher matcher = FOR_ROLES_PATTERN.matcher(markdown);

        while (matcher.find()) {
            String rolesString = matcher.group(1).trim();
            String blockContent = matcher.group(2);

            // Parsear los roles del bloque (separados por comas)
            List<String> blockRoles = Arrays.stream(rolesString.split(","))
                    .map(String::trim)
                    .toList();

            // Verificar si el usuario puede ver este bloque
            boolean canSeeBlock = userRoleCodigo != null && blockRoles.contains(userRoleCodigo);

            if (canSeeBlock) {
                // Mantener el contenido del bloque sin los comentarios
                matcher.appendReplacement(result, Matcher.quoteReplacement(blockContent));
            } else {
                // Eliminar el bloque completo
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    @Override
    public boolean hasApplicableContent(Usuario usuario, String version) {
        Optional<String> effectiveVersion = getEffectiveVersion(version);
        if (effectiveVersion.isEmpty()) {
            log.warn("Versión inválida para hasApplicableContent: {}", version);
            return false;
        }

        try {
            // Los administradores siempre ven contenido
            if (usuario.getRolGeneral() != null &&
                RolEnum.ROL_ADMINISTRADOR.getCodigo().equals(usuario.getRolGeneral().getCodigo())) {
                return true;
            }

            String filename = "classpath:release-notes/release-notes-" + effectiveVersion.get() + ".md";
            Resource resource = resourceLoader.getResource(filename);

            if (!resource.exists()) {
                return false;
            }

            String markdown = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Remover frontmatter
            markdown = removeFrontmatter(markdown);

            // Verificar si hay bloques FOR_ROLES en el contenido
            Matcher matcher = FOR_ROLES_PATTERN.matcher(markdown);

            // Si no hay bloques FOR_ROLES, hay contenido aplicable (contenido general para todos)
            if (!matcher.find()) {
                return true;
            }

            // Si hay bloques FOR_ROLES, verificar si al menos uno aplica al usuario
            String userRoleCodigo = usuario.getRolGeneral() != null ?
                    usuario.getRolGeneral().getCodigo() : null;

            if (userRoleCodigo == null) {
                return false;
            }

            // Reiniciar el matcher para verificar todos los bloques
            matcher.reset();
            while (matcher.find()) {
                String rolesString = matcher.group(1).trim();
                List<String> blockRoles = Arrays.stream(rolesString.split(","))
                        .map(String::trim)
                        .toList();

                if (blockRoles.contains(userRoleCodigo)) {
                    return true; // Al menos un bloque aplica al usuario
                }
            }

            return false; // Ningún bloque aplica al usuario

        } catch (IOException e) {
            log.error("Error verificando contenido aplicable para usuario {} y versión {}",
                     usuario.getId(), effectiveVersion.get(), e);
            return false;
        }
    }
}
