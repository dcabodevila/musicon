package es.musicalia.gestmusica.eventopublico;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoPublicoController {

    private final EventoPublicoService eventoPublicoService;

    /**
     * Página individual de un evento con JSON-LD para indexación de Google
     * URL pública: /eventos/evento/{id}
     */
    @GetMapping("/evento/{id}")
    public String verEventoPublico(@PathVariable Long id, Model model, HttpServletRequest request) {
        log.info("Acceso a evento público: {}", id);

        Optional<EventoPublicoDto> eventoOpt = eventoPublicoService.obtenerEventoPublico(id);

        if (eventoOpt.isEmpty()) {
            log.warn("Evento público no encontrado: {}", id);
            return "redirect:/eventos";
        }

        EventoPublicoDto evento = eventoOpt.get();

        // Construir URL base para JSON-LD
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String baseUrl = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) ||
            (scheme.equals("https") && serverPort != 443)) {
            baseUrl += ":" + serverPort;
        }
        baseUrl += contextPath;

        // Generar JSON-LD
        String jsonLd = evento.toJsonLd(baseUrl);

        // Preparar datos para la vista
        model.addAttribute("evento", evento);
        model.addAttribute("jsonLd", jsonLd);
        model.addAttribute("titulo", evento.getTituloSeo());
        model.addAttribute("descripcion", evento.getDescripcionSeo());
        model.addAttribute("urlEvento", baseUrl + "/eventos/evento/" + id);

        return "evento-publico";
    }

    /**
     * Lista de eventos públicos de un artista
     * URL pública: /eventos/artista/{id}
     */
    @GetMapping("/artista/{idArtista}")
    public String listarEventosPorArtista(@PathVariable Long idArtista, Model model) {
        log.info("Listando eventos públicos para artista: {}", idArtista);

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorArtista(idArtista);

        model.addAttribute("eventos", eventos);
        model.addAttribute("idArtista", idArtista);

        if (!eventos.isEmpty()) {
            model.addAttribute("nombreArtista", eventos.get(0).getNombreArtista());
            model.addAttribute("titulo", "Próximas actuaciones de " + eventos.get(0).getNombreArtista());
        } else {
            model.addAttribute("nombreArtista", "Artista");
            model.addAttribute("titulo", "No hay próximas actuaciones");
        }

        return "eventos-publicos";
    }

    /**
     * API REST: Obtener eventos de un artista en formato JSON
     * Útil para integraciones externas
     */
    @GetMapping(value = "/api/artista/{idArtista}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<EventoPublicoDto>> obtenerEventosApiPorArtista(@PathVariable Long idArtista) {
        log.info("API: Obteniendo eventos para artista: {}", idArtista);

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorArtista(idArtista);

        return ResponseEntity.ok(eventos);
    }

    /**
     * API REST: Obtener eventos por provincia
     */
    @GetMapping(value = "/api/provincia/{provincia}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<EventoPublicoDto>> obtenerEventosApiPorProvincia(
            @PathVariable String provincia,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {

        log.info("API: Obteniendo eventos para provincia: {}", provincia);

        LocalDate fechaDesde = null;
        LocalDate fechaHasta = null;

        try {
            if (desde != null && !desde.isBlank()) {
                fechaDesde = LocalDate.parse(desde, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (hasta != null && !hasta.isBlank()) {
                fechaHasta = LocalDate.parse(hasta, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception e) {
            log.error("Error parseando fechas: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorProvincia(
            provincia, fechaDesde, fechaHasta);

        return ResponseEntity.ok(eventos);
    }

    /**
     * Sitemap XML para Google
     * URL: /eventos/sitemap.xml
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public ResponseEntity<String> generarSitemap(HttpServletRequest request) {
        log.info("Generando sitemap de eventos públicos");

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerTodosEventosPublicos();

        // Construir URL base
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String baseUrl = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) ||
            (scheme.equals("https") && serverPort != 443)) {
            baseUrl += ":" + serverPort;
        }
        baseUrl += contextPath;

        // Generar XML del sitemap
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (EventoPublicoDto evento : eventos) {
            sitemap.append("  <url>\n");
            sitemap.append("    <loc>").append(baseUrl).append("/eventos/evento/")
                   .append(evento.getId()).append("</loc>\n");
            sitemap.append("    <lastmod>").append(LocalDate.now().toString()).append("</lastmod>\n");
            sitemap.append("    <changefreq>daily</changefreq>\n");
            sitemap.append("    <priority>0.8</priority>\n");
            sitemap.append("  </url>\n");
        }

        sitemap.append("</urlset>");

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(sitemap.toString());
    }

    /**
     * Página principal de listado de eventos con filtros
     */
    @GetMapping({"", "/"})
    public String listarTodosEventos(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            Model model) {

        log.info("Listando todos los eventos públicos");

        // Parsear fechas
        LocalDate fechaDesde = LocalDate.now(); // Por defecto: hoy
        LocalDate fechaHasta = LocalDate.now().plusMonths(6); // Por defecto: +6 meses

        try {
            if (desde != null && !desde.isBlank()) {
                fechaDesde = LocalDate.parse(desde, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (hasta != null && !hasta.isBlank()) {
                fechaHasta = LocalDate.parse(hasta, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception e) {
            log.error("Error parseando fechas: {}", e.getMessage());
        }

        // Obtener eventos filtrados
        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorProvincia(
            null, fechaDesde, fechaHasta);

        // Agrupar eventos por día
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(
                e -> e.getFecha().toLocalDate(),
                TreeMap::new,
                Collectors.toList()
            ));

        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("eventos", eventos);
        model.addAttribute("titulo", "Próximas actuaciones");
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", fechaHasta.toString());

        return "eventos-publicos";
    }

    /**
     * Eventos por provincia con filtro por municipio
     */
    @GetMapping("/provincia/{provincia}")
    public String listarEventosPorProvincia(
            @PathVariable String provincia,
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            Model model) {

        log.info("Listando eventos para provincia: {}, municipio: {}", provincia, municipio);

        // Parsear fechas
        LocalDate fechaDesde = LocalDate.now();
        LocalDate fechaHasta = LocalDate.now().plusMonths(6);

        try {
            if (desde != null && !desde.isBlank()) {
                fechaDesde = LocalDate.parse(desde, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (hasta != null && !hasta.isBlank()) {
                fechaHasta = LocalDate.parse(hasta, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception e) {
            log.error("Error parseando fechas: {}", e.getMessage());
        }

        // Obtener eventos
        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorProvincia(
            provincia, fechaDesde, fechaHasta);

        // Filtrar por municipio si se especifica
        if (municipio != null && !municipio.isBlank()) {
            String municipioLower = municipio.toLowerCase();
            eventos = eventos.stream()
                .filter(e -> e.getMunicipio().toLowerCase().contains(municipioLower))
                .collect(Collectors.toList());
        }

        // Obtener lista de municipios únicos para el filtro
        List<String> municipios = eventos.stream()
            .map(EventoPublicoDto::getMunicipio)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // Agrupar eventos por día
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(
                e -> e.getFecha().toLocalDate(),
                TreeMap::new,
                Collectors.toList()
            ));

        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("eventos", eventos);
        model.addAttribute("provincia", provincia);
        model.addAttribute("municipio", municipio);
        model.addAttribute("municipios", municipios);
        model.addAttribute("titulo", "Eventos en " + provincia);
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", fechaHasta.toString());

        return "eventos-publicos-provincia";
    }
}
