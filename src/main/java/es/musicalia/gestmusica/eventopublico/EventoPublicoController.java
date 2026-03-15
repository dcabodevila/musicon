package es.musicalia.gestmusica.eventopublico;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    private static final String EVENT_IMAGE_URL =
        "https://res.cloudinary.com/hseoceuyz/image/upload/v1760835633/landing-festia_epbr7a.png";
    private static final String ORGANIZER_NAME_FALLBACK = "festia.es";

    private final EventoPublicoService eventoPublicoService;
    private final LocalizacionService localizacionService;

    /**
     * URL legada: /eventos/evento/{id}
     * Redirecciona 301 a la URL canonica con slug.
     */
    @GetMapping("/evento/{id:\\d+}")
    public RedirectView redirigirEventoPublicoLegacy(@PathVariable Long id, HttpServletRequest request) {
        EventoPublicoDto evento = obtenerEventoONotFound(id);
        return crearRedireccionPermanente(construirUrlAbsoluta(request, evento.getPathPublico()));
    }

    /**
     * URL canonica: /eventos/evento/{id}-{slug}
     */
    @GetMapping("/evento/{id:\\d+}-{slug}")
    public ModelAndView verEventoPublico(@PathVariable Long id, @PathVariable String slug, HttpServletRequest request) {
        log.info("Acceso a evento publico: {}", id);

        EventoPublicoDto evento = obtenerEventoONotFound(id);
        String slugCanonico = evento.getSlug();
        String urlCanonica = construirUrlAbsoluta(request, evento.getPathPublico());

        if (!slugCanonico.equals(slug)) {
            return new ModelAndView(crearRedireccionPermanente(urlCanonica));
        }

        String baseUrl = construirBaseUrl(request);

        ModelAndView mv = new ModelAndView("evento-publico");
        String organizerName = (evento.getNombreAgencia() != null && !evento.getNombreAgencia().isBlank())
            ? evento.getNombreAgencia()
            : ORGANIZER_NAME_FALLBACK;
        mv.addObject("evento", evento);
        String imageUrl = (evento.getLogoArtista() != null && !evento.getLogoArtista().isBlank())
            ? evento.getLogoArtista()
            : EVENT_IMAGE_URL;
        mv.addObject("jsonLd", evento.toJsonLd(baseUrl, organizerName, evento.getUrlOrganizador(), imageUrl));
        mv.addObject("titulo", evento.getTituloSeo());
        mv.addObject("descripcion", evento.getDescripcionSeo());
        mv.addObject("urlEvento", urlCanonica);
        mv.addObject("canonicalUrl", urlCanonica);
        mv.addObject("metaRobots", "index,follow");
        return mv;
    }

    /**
     * Lista de eventos publicos de un artista.
     */
    @GetMapping("/artista/{idArtista}")
    public String listarEventosPorArtista(@PathVariable Long idArtista, Model model, HttpServletRequest request) {
        log.info("Listando eventos publicos para artista: {}", idArtista);

        LocalDate fechaDesde = LocalDate.now();
        LocalDate fechaHasta = LocalDate.now().plusMonths(1);

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, idArtista, fechaDesde, fechaHasta);
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getFecha().toLocalDate(), TreeMap::new, Collectors.toList()));

        model.addAttribute("eventos", eventos);
        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("idArtista", idArtista);
        model.addAttribute("canonicalUrl", construirUrlAbsoluta(request, "/eventos/artista/" + idArtista));
        model.addAttribute("metaRobots", eventos.isEmpty() ? "noindex,follow" : "index,follow");
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", fechaHasta.toString());
        model.addAttribute("provincia", null);
        model.addAttribute("municipio", null);
        model.addAttribute("idArtistaSeleccionado", idArtista);
        List<EventoPublicoDto> eventosCatalogo = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, null, LocalDate.now(), null);
        model.addAttribute("provincias", obtenerProvinciasOrdenadas());
        model.addAttribute("municipiosFiltro", obtenerMunicipiosParaFiltro());
        model.addAttribute("artistasDisponibles", obtenerArtistasOrdenados(eventosCatalogo));

        if (!eventos.isEmpty()) {
            model.addAttribute("nombreArtista", eventos.get(0).getNombreArtista());
            model.addAttribute("titulo", "Próximas actuaciones de " + eventos.get(0).getNombreArtista() + " | Festia");
            model.addAttribute("descripcion", "Agenda de conciertos y actuaciones de " + eventos.get(0).getNombreArtista()
                + ". Consulta fechas, municipios y detalles de cada evento en Festia.");
        } else {
            model.addAttribute("nombreArtista", "Artista");
            model.addAttribute("titulo", "No hay próximas actuaciones | Festia");
            model.addAttribute("descripcion", "No hay actuaciones programadas para este artista en este momento. Revisa otras fechas y artistas en Festia.");
        }

        return "eventos-publicos";
    }

    /**
     * API REST: Obtener eventos de un artista en formato JSON.
     */
    @GetMapping(value = "/api/artista/{idArtista}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<EventoPublicoDto>> obtenerEventosApiPorArtista(@PathVariable Long idArtista) {
        log.info("API: Obteniendo eventos para artista: {}", idArtista);
        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorArtista(idArtista);
        return ResponseEntity.ok(eventos);
    }

    /**
     * API REST: Obtener eventos por provincia.
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

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorProvincia(provincia, fechaDesde, fechaHasta);
        return ResponseEntity.ok(eventos);
    }

    /**
     * Sitemap XML para Google.
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public ResponseEntity<String> generarSitemap(HttpServletRequest request) {
        log.info("Generando sitemap de eventos publicos");

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerTodosEventosPublicos();
        String baseUrl = construirBaseUrl(request);

        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (EventoPublicoDto evento : eventos) {
            sitemap.append("  <url>\n");
            sitemap.append("    <loc>").append(baseUrl).append(evento.getPathPublico()).append("</loc>\n");
            sitemap.append("    <lastmod>").append(evento.getLastModDate()).append("</lastmod>\n");
            sitemap.append("    <changefreq>weekly</changefreq>\n");
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
     * Pagina principal de listado de eventos con filtros.
     */
    @GetMapping({"", "/"})
    public String listarTodosEventos(
        @RequestParam(required = false) String provincia,
        @RequestParam(required = false) String municipio,
        @RequestParam(required = false) Long idArtista,
        @RequestParam(required = false) String desde,
        @RequestParam(required = false) String hasta,
        Model model,
        HttpServletRequest request) {

        log.info("Listando todos los eventos publicos");

        LocalDate fechaDesde = LocalDate.now();
        LocalDate fechaHasta = LocalDate.now().plusMonths(1);

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

        List<EventoPublicoDto> eventosCatalogo = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, null, LocalDate.now(), null);
        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosFiltrados(
            provincia, municipio, idArtista, fechaDesde, fechaHasta);
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getFecha().toLocalDate(), TreeMap::new, Collectors.toList()));

        boolean filtrosAplicados =
            (provincia != null && !provincia.isBlank()) ||
            (municipio != null && !municipio.isBlank()) ||
            idArtista != null ||
            (desde != null && !desde.isBlank()) ||
            (hasta != null && !hasta.isBlank());
        boolean noIndex = filtrosAplicados || eventos.isEmpty();

        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("eventos", eventos);
        model.addAttribute("titulo", construirTituloListado(provincia, municipio, idArtista));
        model.addAttribute("descripcion", construirDescripcionListado(provincia, municipio, idArtista));
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", fechaHasta.toString());
        model.addAttribute("canonicalUrl", construirUrlAbsoluta(request, "/eventos"));
        model.addAttribute("metaRobots", noIndex ? "noindex,follow" : "index,follow");
        model.addAttribute("provincia", provincia);
        model.addAttribute("municipio", municipio);
        model.addAttribute("idArtistaSeleccionado", idArtista);
        model.addAttribute("provincias", obtenerProvinciasOrdenadas());
        model.addAttribute("municipiosFiltro", obtenerMunicipiosParaFiltro());
        model.addAttribute("artistasDisponibles", obtenerArtistasOrdenados(eventosCatalogo));

        return "eventos-publicos";
    }

    /**
     * Eventos por provincia con filtro por municipio.
     */
    @GetMapping("/provincia/{provincia}")
    public RedirectView listarEventosPorProvincia(
        @PathVariable String provincia,
        HttpServletRequest request) {

        log.info("Redirigiendo eventos por provincia '{}' a listado principal con filtro", provincia);
        String provinciaCodificada = UriUtils.encodeQueryParam(provincia, StandardCharsets.UTF_8);
        String urlDestino = construirUrlAbsoluta(request, "/eventos?provincia=" + provinciaCodificada);
        return crearRedireccionPermanente(urlDestino);
    }

    private EventoPublicoDto obtenerEventoONotFound(Long id) {
        Optional<EventoPublicoDto> eventoOpt = eventoPublicoService.obtenerEventoPublico(id);
        if (eventoOpt.isEmpty()) {
            log.warn("Evento publico no encontrado: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
        }
        return eventoOpt.get();
    }

    private String construirBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String baseUrl = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            baseUrl += ":" + serverPort;
        }
        return baseUrl + contextPath;
    }

    private String construirUrlAbsoluta(HttpServletRequest request, String path) {
        return construirBaseUrl(request) + path;
    }

    private RedirectView crearRedireccionPermanente(String destinationUrl) {
        RedirectView redirectView = new RedirectView(destinationUrl);
        redirectView.setExposeModelAttributes(false);
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return redirectView;
    }

    private List<String> obtenerProvinciasOrdenadas() {
        return localizacionService.findAllProvincias().stream()
            .map(CodigoNombreRecord::nombre)
            .filter(nombre -> nombre != null && !nombre.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.toList());
    }

    private List<MunicipioFiltro> obtenerMunicipiosParaFiltro() {
        return localizacionService.findAllProvincias().stream()
            .flatMap(provincia -> localizacionService.findAllMunicipiosByIdProvincia(provincia.id()).stream()
                .map(municipio -> new MunicipioFiltro(municipio.nombre(), provincia.nombre())))
            .distinct()
            .sorted(Comparator.comparing(MunicipioFiltro::provincia, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(MunicipioFiltro::nombre, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    private List<EventoPublicoDto> obtenerArtistasOrdenados(List<EventoPublicoDto> eventos) {
        Map<Long, EventoPublicoDto> artistasUnicos = eventos.stream()
            .collect(Collectors.toMap(
                EventoPublicoDto::getIdArtista,
                evento -> evento,
                (first, second) -> first,
                LinkedHashMap::new));

        return artistasUnicos.values().stream()
            .sorted(Comparator.comparing(EventoPublicoDto::getNombreArtista, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    private String construirTituloListado(String provincia, String municipio, Long idArtista) {
        if (idArtista != null) {
            return "Eventos musicales por artista | Festia";
        }
        if (municipio != null && !municipio.isBlank() && provincia != null && !provincia.isBlank()) {
            return "Eventos musicales en " + municipio + " (" + provincia + ") | Festia";
        }
        if (provincia != null && !provincia.isBlank()) {
            return "Eventos musicales en " + provincia + " | Festia";
        }
        return "Próximos eventos y actuaciones musicales | Festia";
    }

    private String construirDescripcionListado(String provincia, String municipio, Long idArtista) {
        if (idArtista != null) {
            return "Consulta actuaciones por artista, municipio y fecha en Festia. Accede al detalle de cada concierto y evento musical.";
        }
        if (municipio != null && !municipio.isBlank() && provincia != null && !provincia.isBlank()) {
            return "Agenda de actuaciones en " + municipio + " (" + provincia + "). Filtra por artista y fecha para encontrar conciertos y eventos musicales.";
        }
        if (provincia != null && !provincia.isBlank()) {
            return "Agenda de actuaciones en " + provincia + ". Filtra por artista, municipio y fecha para encontrar conciertos y eventos musicales.";
        }
        return "Agenda de próximos conciertos y actuaciones musicales en Festia. Busca por artista, localidad y fecha.";
    }

    private record MunicipioFiltro(String nombre, String provincia) {
    }
}
