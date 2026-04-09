package es.musicalia.gestmusica.sitemap;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Genera el sitemap índice en la raíz del dominio (https://festia.es/sitemap.xml).
 * <p>
 * Este sitemap índice referencia los sub-sitemaps de cada sección del sitio,
 * siguiendo la recomendación de Google para sitios con múltiples tipos de contenido.
 * <p>
 * Estructura:
 * <pre>
 * https://festia.es/sitemap.xml               → índice (este controller)
 *   └── https://festia.es/eventos/sitemap.xml → eventos, artistas, provincias, municipios
 * </pre>
 */
@Slf4j
@RestController
public class SitemapIndexController {

    private static final String BASE_URL = "https://festia.es";

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public ResponseEntity<String> generarSitemapIndice(HttpServletRequest request) {
        log.info("Generando sitemap índice");

        String baseUrl = construirBaseUrl(request);
        String today = java.time.LocalDate.now().toString();

        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Sub-sitemap de eventos (generado dinámicamente por EventoPublicoController)
        sitemap.append("  <sitemap>\n");
        sitemap.append("    <loc>").append(baseUrl).append("/eventos/sitemap.xml</loc>\n");
        sitemap.append("    <lastmod>").append(today).append("</lastmod>\n");
        sitemap.append("  </sitemap>\n");

        sitemap.append("</sitemapindex>");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .cacheControl(org.springframework.http.CacheControl.maxAge(1, java.util.concurrent.TimeUnit.HOURS))
                .body(sitemap.toString());
    }

    private String construirBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String baseUrl = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            baseUrl += ":" + serverPort;
        }
        return baseUrl;
    }
}