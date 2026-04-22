package es.musicalia.gestmusica.eventopublico;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoPublicoController {
    private static final String EVENT_IMAGE_URL =
        "https://res.cloudinary.com/hseoceuyz/image/upload/v1760835633/landing-festia_epbr7a.png";
    private static final String ORGANIZER_NAME_FALLBACK = "festia.es";

    // Textos SEO únicos por provincia
    private static final Map<String, String> TEXTOS_PROVINCIA = new HashMap<>();
    static {
        // Galicia
        TEXTOS_PROVINCIA.put("Coruña", "Consulta las próximas fiestas y verbenas en A Coruña. Encuentra orquestas gallegas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Lugo", "Descubre las actuaciones musicales programadas en Lugo. Orquestas gallegas, bandas de verbena y artistas en fiestas populares de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Ourense", "Agenda de conciertos y actuaciones en Ourense. Consulta las fechas de orquestas, grupos musicales y discotecas móviles en los municipios de la provincia para las fiestas.");
        TEXTOS_PROVINCIA.put("Pontevedra", "Consulta las próximas fiestas y verbenas en Pontevedra. Encuentra orquestas gallegas, discotecas móviles y grupos musicales con fechas confirmadas  en los municipios de la provincia.");

        // Principado de Asturias
        TEXTOS_PROVINCIA.put("Asturias", "Programación de orquestas y grupos musicales en Asturias. Fiestas populares, verbenas y eventos culturales con artistas confirmados en los concejos del principado.");

        // Cantabria
        TEXTOS_PROVINCIA.put("Cantabria", "Actuaciones musicales en Cantabria. Consulta la agenda de orquestas, grupos y artistas para fiestas populares y verbenas en los municipios de la comunidad.");

        // País Vasco
        TEXTOS_PROVINCIA.put("Álava", "Fiestas y verbenas en Álava con orquestas y grupos musicales. Consulta fechas, municipios y artistas confirmados para las celebraciones de la provincia.");
        TEXTOS_PROVINCIA.put("Bizkaia", "Agenda musical de Bizkaia. Orquestas, grupos y artistas en las fiestas populares y verbenas de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Gipuzkoa", "Programación de fiestas en Gipuzkoa. Descubre las orquestas, bandas y grupos musicales en los municipios de la provincia para las celebraciones.");

        // Navarra
        TEXTOS_PROVINCIA.put("Navarra", "Verbenas y fiestas populares en Navarra. Consulta las actuaciones de orquestas y grupos musicales programadas en los municipios de la comunidad.");

        // La Rioja
        TEXTOS_PROVINCIA.put("La Rioja", "Agenda de conciertos y verbenas en La Rioja. Orquestas, artistas y grupos musicales en las fiestas populares de los municipios de la comunidad.");

        // Castilla y León
        TEXTOS_PROVINCIA.put("León", "Fiestas y verbenas en León con orquestas y grupos musicales. Consulta fechas, municipios y artistas confirmados para las celebraciones de la provincia.");
        TEXTOS_PROVINCIA.put("Zamora", "Agenda musical de Zamora. Orquestas, grupos folklóricos y artistas en las fiestas populares y verbenas de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Salamanca", "Conciertos y actuaciones en Salamanca. Descubre las orquestas, bandas y grupos musicales programados en las fiestas de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Burgos", "Programación de fiestas en Burgos. Orquestas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Palencia", "Verbenas y fiestas populares en Palencia. Consulta las actuaciones de orquestas y grupos musicales programadas en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Valladolid", "Agenda de conciertos y verbenas en Valladolid. Orquestas, artistas y grupos musicales en las fiestas populares de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Soria", "Fiestas y actuaciones musicales en Soria. Descubre las orquestas, grupos y artistas programados en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Segovia", "Programación musical en Segovia. Orquestas, bandas y grupos en las fiestas populares y verbenas de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Ávila", "Consulta las verbenas y fiestas de Ávila. Actuaciones de orquestas, grupos musicales y artistas en los municipios de la provincia.");

        // Madrid
        TEXTOS_PROVINCIA.put("Madrid", "Agenda de eventos musicales en Madrid. Conciertos, fiestas populares y verbenas con orquestas y artistas en los municipios de la comunidad.");

        // Castilla-La Mancha
        TEXTOS_PROVINCIA.put("Toledo", "Fiestas y actuaciones en Toledo. Programación de orquestas, grupos musicales y discotecas móviles en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Ciudad Real", "Verbenas populares en Ciudad Real. Consulta las orquestas y grupos musicales programados en las fiestas de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Albacete", "Actuaciones musicales en Albacete. Orquestas, grupos y artistas en las fiestas populares de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Cuenca", "Programación de fiestas en Cuenca. Descubre las orquestas, bandas y grupos musicales en los municipios de la provincia para las celebraciones.");
        TEXTOS_PROVINCIA.put("Guadalajara", "Fiestas y verbenas en Guadalajara. Agenda de orquestas y grupos musicales programados en los municipios de la provincia.");

        // Extremadura
        TEXTOS_PROVINCIA.put("Badajoz", "Consulta las próximas fiestas y verbenas en Badajoz. Encuentra orquestas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Cáceres", "Descubre las actuaciones musicales programadas en Cáceres. Orquestas, bandas y artistas en verbenas y fiestas populares de los municipios de la provincia.");

        // Cataluña
        TEXTOS_PROVINCIA.put("Barcelona", "Agenda de conciertos y actuaciones en Barcelona. Consulta las fechas de orquestas, grupos musicales y discotecas móviles en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Girona", "Programación de fiestas en Girona. Orquestas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Lleida", "Verbenas y fiestas populares en Lleida. Consulta las actuaciones de orquestas y grupos musicales programadas en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Tarragona", "Actuaciones musicales en Tarragona. Orquestas, grupos y artistas en las fiestas populares de los municipios de la provincia.");

        // Comunidad Valenciana
        TEXTOS_PROVINCIA.put("Valencia", "Fiestas y verbenas en Valencia con orquestas y grupos musicales. Consulta fechas, municipios y artistas confirmados para las celebraciones de la provincia.");
        TEXTOS_PROVINCIA.put("Alicante", "Agenda musical de Alicante. Orquestas, grupos y artistas en las fiestas populares y verbenas de la Costa Blanca.");
        TEXTOS_PROVINCIA.put("Castellón", "Conciertos y actuaciones en Castellón. Descubre las orquestas, bandas y grupos musicales programados en las fiestas de los municipios de la provincia.");

        // Islas Baleares
        TEXTOS_PROVINCIA.put("Baleares", "Programación de orquestas y grupos musicales en Baleares. Fiestas populares, verbenas y eventos en Mallorca, Menorca, Ibiza y Formentera.");

        // Islas Canarias
        TEXTOS_PROVINCIA.put("Las Palmas", "Consulta las próximas fiestas y verbenas en Las Palmas. Encuentra orquestas, grupos y artistas con fechas confirmadas en Gran Canaria, Lanzarote y Fuerteventura.");
        TEXTOS_PROVINCIA.put("Tenerife", "Agenda de eventos musicales en Santa Cruz de Tenerife. Conciertos, fiestas populares y verbenas en Tenerife, La Gomera, La Palma y El Hierro.");

        // Andalucía
        TEXTOS_PROVINCIA.put("Sevilla", "Fiestas y actuaciones en Sevilla. Programación de orquestas, grupos musicales y discotecas móviles en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Málaga", "Verbenas populares en Málaga. Consulta las orquestas y grupos musicales programados en las fiestas de la Costa del Sol.");
        TEXTOS_PROVINCIA.put("Cádiz", "Actuaciones musicales en Cádiz. Orquestas, grupos y artistas en las fiestas populares de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Córdoba", "Programación de fiestas en Córdoba. Descubre las orquestas, bandas y grupos musicales en los municipios de la provincia para las celebraciones.");
        TEXTOS_PROVINCIA.put("Granada", "Agenda de conciertos y verbenas en Granada. Orquestas, artistas y grupos musicales en las fiestas populares de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Jaén", "Fiestas y actuaciones musicales en Jaén. Descubre las orquestas, grupos y artistas programados en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Almería", "Consulta las verbenas y fiestas de Almería. Actuaciones de orquestas, grupos musicales y artistas en los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Huelva", "Agenda de eventos musicales en Huelva. Conciertos, fiestas populares y verbenas con orquestas y artistas en los municipios de la provincia.");

        // Aragón
        TEXTOS_PROVINCIA.put("Zaragoza", "Fiestas y verbenas en Zaragoza con orquestas y grupos musicales. Consulta fechas, municipios y artistas confirmados para las celebraciones de la provincia.");
        TEXTOS_PROVINCIA.put("Huesca", "Agenda musical de Huesca. Orquestas, grupos y artistas en las fiestas populares y verbenas de los municipios de la provincia.");
        TEXTOS_PROVINCIA.put("Teruel", "Conciertos y actuaciones en Teruel. Descubre las orquestas, bandas y grupos musicales programados en las fiestas de los municipios de la provincia.");

        // Murcia
        TEXTOS_PROVINCIA.put("Murcia", "Programación de fiestas en Murcia. Orquestas, discotecas móviles y grupos musicales con fechas confirmadas en los municipios de la región.");

        // Ceuta y Melilla
        TEXTOS_PROVINCIA.put("Ceuta", "Actuaciones musicales en Ceuta. Consulta la agenda de orquestas, grupos y artistas para fiestas populares y verbenas en la ciudad autónoma.");
        TEXTOS_PROVINCIA.put("Melilla", "Fiestas y actuaciones musicales en Melilla. Descubre las orquestas, grupos y artistas programados en la ciudad autónoma.");
    }

    private final EventoPublicoService eventoPublicoService;
    private final LocalizacionService localizacionService;
    private final ObjectMapper objectMapper;

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
            ? EventoPublicoDto.normalizeImageUrl(evento.getLogoArtista())
            : EVENT_IMAGE_URL;
        mv.addObject("jsonLd", evento.toJsonLd(baseUrl, organizerName, evento.getUrlOrganizador(), imageUrl));
        mv.addObject("breadcrumbJsonLd", buildBreadcrumbJsonLd(baseUrl, evento));
        mv.addObject("titulo", evento.getTituloSeo());
        mv.addObject("descripcion", evento.getDescripcionSeo());
        mv.addObject("ogImage", imageUrl);
        mv.addObject("urlEvento", urlCanonica);
        mv.addObject("canonicalUrl", urlCanonica);
        mv.addObject("metaRobots", "index,follow");

        List<EventoPublicoDto> eventosRelacionados = eventoPublicoService
            .obtenerEventosPublicosPorArtista(evento.getIdArtista())
            .stream()
            .filter(e -> !e.getId().equals(id))
            .sorted(Comparator.comparing(EventoPublicoDto::getFecha))
            .limit(10)
            .collect(Collectors.toList());
        mv.addObject("eventosRelacionados", eventosRelacionados);
        mv.addObject("googleCalendarUrl", buildGoogleCalendarUrl(evento, baseUrl));

        return mv;
    }

    /**
     * Descarga del evento en formato iCal (.ics) — compatible con Apple Calendar, Outlook, etc.
     */
    @GetMapping(value = "/evento/{id:\\d+}-{slug}/ical")
    @ResponseBody
    public ResponseEntity<String> descargarIcal(@PathVariable Long id, @PathVariable String slug) {
        EventoPublicoDto evento = obtenerEventoONotFound(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
            .header("Content-Disposition", "attachment; filename=\"festia-evento-" + id + ".ics\"")
            .body(buildIcal(evento));
    }

    /**
     * Lista de eventos publicos de un artista.
     */
    @GetMapping("/artista/{idArtista}")
    public String listarEventosPorArtista(@PathVariable Long idArtista, Model model, HttpServletRequest request) {
        log.info("Listando eventos publicos para artista: {}", idArtista);

        LocalDate fechaDesde = LocalDate.now();

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, idArtista, fechaDesde, null);
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getFecha().toLocalDate(), TreeMap::new, Collectors.toList()));

        String canonicalUrl = construirUrlAbsoluta(request, "/eventos/artista/" + idArtista);
        boolean indexable = !eventos.isEmpty();

        model.addAttribute("eventos", eventos);
        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("idArtista", idArtista);
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("metaRobots", indexable ? "index,follow" : "noindex,follow");
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", null);
        model.addAttribute("provincia", null);
        model.addAttribute("municipio", null);
        model.addAttribute("idArtistaSeleccionado", idArtista);
        List<EventoPublicoDto> eventosCatalogo = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, null, LocalDate.now(), null);
        model.addAttribute("provincias", obtenerProvinciasOrdenadas());
        // Municipio select starts empty (AJAX loaded)
        model.addAttribute("municipiosProvincia", List.of());
        model.addAttribute("artistasDisponibles", obtenerArtistasOrdenados(eventosCatalogo));
        model.addAttribute("contextoPagina", "artista");

        if (!eventos.isEmpty()) {
            EventoPublicoDto primerEvento = eventos.get(0);
            String nombreArtista = primerEvento.getNombreArtista();
            String titulo = "Conciertos y Fechas de " + nombreArtista + " | Festia";
            String descripcion = "Descubre todas las fechas confirmadas de " + nombreArtista
                + ". Fiestas, verbenas y actuaciones con horarios y ubicaciones en Festia.";
            String ogImage = (primerEvento.getLogoArtista() != null && !primerEvento.getLogoArtista().isBlank())
                ? EventoPublicoDto.normalizeImageUrl(primerEvento.getLogoArtista()) : EVENT_IMAGE_URL;
            String baseUrl = construirBaseUrl(request);

            model.addAttribute("nombreArtista", nombreArtista);
            model.addAttribute("titulo", titulo);
            model.addAttribute("descripcion", descripcion);
            model.addAttribute("ogImage", ogImage);
            model.addAttribute("jsonLd", buildArtistaJsonLd(eventos, baseUrl, canonicalUrl, titulo, descripcion, ogImage));
        } else {
            model.addAttribute("nombreArtista", "Artista");
            model.addAttribute("titulo", "No hay próximas actuaciones | Festia");
            model.addAttribute("descripcion", "No hay actuaciones programadas para este artista en este momento. Revisa otras fechas y artistas en Festia.");
            model.addAttribute("ogImage", EVENT_IMAGE_URL);
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

        // Página de información (alta prioridad, cambia poco)
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(baseUrl).append("/info</loc>\n");
        sitemap.append("    <changefreq>monthly</changefreq>\n");
        sitemap.append("    <priority>0.9</priority>\n");
        sitemap.append("  </url>\n");

        // Página principal de eventos (alta prioridad, cambia diariamente)
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(baseUrl).append("/eventos</loc>\n");
        sitemap.append("    <changefreq>daily</changefreq>\n");
        sitemap.append("    <priority>0.9</priority>\n");
        sitemap.append("  </url>\n");

        for (EventoPublicoDto evento : eventos) {
            sitemap.append("  <url>\n");
            sitemap.append("    <loc>").append(baseUrl).append(evento.getPathPublico()).append("</loc>\n");
            sitemap.append("    <lastmod>").append(evento.getLastModDate()).append("</lastmod>\n");
            sitemap.append("    <changefreq>daily</changefreq>\n");
            sitemap.append("    <priority>0.8</priority>\n");
            sitemap.append("  </url>\n");
        }

        // Páginas de artista (una por artista con eventos activos)
        Map<Long, EventoPublicoDto> artistasUnicos = eventos.stream()
            .collect(Collectors.toMap(
                EventoPublicoDto::getIdArtista,
                e -> e,
                (a, b) -> a.getLastModDate().isAfter(b.getLastModDate()) ? a : b));
        for (Map.Entry<Long, EventoPublicoDto> entry : artistasUnicos.entrySet()) {
            sitemap.append("  <url>\n");
            sitemap.append("    <loc>").append(baseUrl).append("/eventos/artista/").append(entry.getKey()).append("</loc>\n");
            sitemap.append("    <lastmod>").append(entry.getValue().getLastModDate()).append("</lastmod>\n");
            sitemap.append("    <changefreq>daily</changefreq>\n");
            sitemap.append("    <priority>0.7</priority>\n");
            sitemap.append("  </url>\n");
        }

        // Páginas de provincia (una por provincia distinta, excluir "provisional")
        Map<String, List<EventoPublicoDto>> eventosPorProvincia = eventos.stream()
            .filter(e -> e.getProvincia() != null && !e.getProvincia().isBlank()
                && !e.getProvincia().toLowerCase().contains("provisional"))
            .collect(Collectors.groupingBy(EventoPublicoDto::getProvincia));
        for (Map.Entry<String, List<EventoPublicoDto>> entry : eventosPorProvincia.entrySet()) {
            String nombreProvincia = entry.getKey();
            LocalDate lastMod = entry.getValue().stream()
                .map(EventoPublicoDto::getLastModDate)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());
            String locProvincia = baseUrl + "/eventos/provincia/"
                + UriUtils.encodePath(nombreProvincia, StandardCharsets.UTF_8);
            sitemap.append("  <url>\n");
            sitemap.append("    <loc>").append(locProvincia).append("</loc>\n");
            sitemap.append("    <lastmod>").append(lastMod).append("</lastmod>\n");
            sitemap.append("    <changefreq>weekly</changefreq>\n");
            sitemap.append("    <priority>0.7</priority>\n");
            sitemap.append("  </url>\n");
        }

        // Páginas de municipio (una por municipio distinto)
        Map<String, List<EventoPublicoDto>> eventosPorMunicipio = eventos.stream()
            .filter(e -> e.getMunicipio() != null && !e.getMunicipio().isBlank())
            .collect(Collectors.groupingBy(EventoPublicoDto::getMunicipio));
        for (Map.Entry<String, List<EventoPublicoDto>> entry : eventosPorMunicipio.entrySet()) {
            String nombreMunicipio = entry.getKey();
            LocalDate lastMod = entry.getValue().stream()
                .map(EventoPublicoDto::getLastModDate)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());
            String locMunicipio = baseUrl + "/eventos/municipio/"
                + UriUtils.encodePath(nombreMunicipio, StandardCharsets.UTF_8);
            sitemap.append("  <url>\n");
            sitemap.append("    <loc>").append(locMunicipio).append("</loc>\n");
            sitemap.append("    <lastmod>").append(lastMod).append("</lastmod>\n");
            sitemap.append("    <changefreq>weekly</changefreq>\n");
            sitemap.append("    <priority>0.6</priority>\n");
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
        @RequestParam(required = false, defaultValue = "1") int page,
        Model model,
        HttpServletRequest request) {

        log.info("Listando todos los eventos publicos");

        LocalDate fechaDesde = LocalDate.now();
        LocalDate fechaHasta = LocalDate.now().plusDays(45);

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

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, 20, Sort.by("fecha").ascending().and(Sort.by("artista.nombre").ascending()));

        List<EventoPublicoDto> eventosCatalogo = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, null, LocalDate.now(), null);
        Page<EventoPublicoDto> paginaEventos = eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(
            provincia, municipio, idArtista, fechaDesde, fechaHasta, pageable);

        int totalPaginas = paginaEventos.getTotalPages();
        if (totalPaginas > 0 && page > totalPaginas) {
            StringBuilder redirectUrl = new StringBuilder("/eventos?page=" + totalPaginas);
            if (provincia != null && !provincia.isBlank()) redirectUrl.append("&provincia=").append(UriUtils.encodeQueryParam(provincia, StandardCharsets.UTF_8));
            if (municipio != null && !municipio.isBlank()) redirectUrl.append("&municipio=").append(UriUtils.encodeQueryParam(municipio, StandardCharsets.UTF_8));
            if (idArtista != null) redirectUrl.append("&idArtista=").append(idArtista);
            if (desde != null && !desde.isBlank()) redirectUrl.append("&desde=").append(desde);
            if (hasta != null && !hasta.isBlank()) redirectUrl.append("&hasta=").append(hasta);
            return "redirect:" + redirectUrl;
        }

        List<EventoPublicoDto> eventos = paginaEventos.getContent();
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getFecha().toLocalDate(), TreeMap::new, Collectors.toList()));

        boolean filtrosAplicados =
            (provincia != null && !provincia.isBlank()) ||
            (municipio != null && !municipio.isBlank()) ||
            idArtista != null ||
            (desde != null && !desde.isBlank()) ||
            (hasta != null && !hasta.isBlank());
        boolean noIndex = filtrosAplicados || paginaEventos.getTotalElements() == 0;

        String titulo = construirTituloListado(provincia, municipio, idArtista, (int) paginaEventos.getTotalElements());
        String descripcion = construirDescripcionListado(provincia, municipio, idArtista);
        String canonicalUrl = construirUrlAbsoluta(request, "/eventos");

        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("eventos", eventos);
        model.addAttribute("titulo", titulo);
        model.addAttribute("descripcion", descripcion);
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", fechaHasta != null ? fechaHasta.toString() : null);
        model.addAttribute("fechaMaxFiltro", LocalDate.now().plusDays(45).toString());
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("metaRobots", noIndex ? "noindex,follow" : "index,follow");
        model.addAttribute("provincia", provincia);
        model.addAttribute("municipio", municipio);
        model.addAttribute("idArtistaSeleccionado", idArtista);
        model.addAttribute("provincias", obtenerProvinciasOrdenadas());
        // Pre-cargar municipios cuando provincia está seleccionada (preservar estado del filtro)
        if (provincia != null && !provincia.isBlank()) {
            model.addAttribute("municipiosProvincia",
                localizacionService.findMunicipiosByProvinciaNombre(provincia));
        } else {
            model.addAttribute("municipiosProvincia", List.of());
        }
        model.addAttribute("artistasDisponibles", obtenerArtistasOrdenados(eventosCatalogo));
        model.addAttribute("ogImage", EVENT_IMAGE_URL);
        model.addAttribute("contextoPagina", "catalogo");
        model.addAttribute("urlBase", "/eventos");
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("totalEventos", paginaEventos.getTotalElements());
        model.addAttribute("hayPaginaAnterior", page > 1);
        model.addAttribute("hayPaginaSiguiente", page < totalPaginas);
        model.addAttribute("paginaAnterior", page - 1);
        model.addAttribute("paginaSiguiente", page + 1);

        if (page > 1) {
            model.addAttribute("metaRobots", "noindex,follow");
        }

        if (!noIndex && !eventos.isEmpty()) {
            String baseUrl = construirBaseUrl(request);
            List<EventoPublicoDto> eventosParaJsonLd = eventos.stream()
                .filter(EventoPublicoDto::isIndexableForJsonLd)
                .limit(50)
                .collect(Collectors.toList());
            if (!eventosParaJsonLd.isEmpty()) {
                model.addAttribute("jsonLd", buildItemListJsonLd(eventosParaJsonLd, baseUrl, titulo, canonicalUrl));
            }
        }

        return "eventos-publicos";
    }

    /**
     * Página indexable de eventos por provincia con JSON-LD Schema.org.
     * Incluye redirección 301 para normalización de casing en URLs.
     */
    @GetMapping("/provincia/{provincia}")
    public Object listarEventosPorProvincia(
        @PathVariable String provincia,
        @RequestParam(required = false, defaultValue = "1") int page,
        Model model,
        HttpServletRequest request) {

        String provinciaTrim = provincia.trim();
        log.info("Listando eventos publicos para provincia: {}", provinciaTrim);

        // Buscar provincia para obtener nombre canónico y validar existencia
        Optional<Provincia> provinciaOpt = localizacionService.findProvinciaByNombreUpperCase(provinciaTrim);

        if (provinciaOpt.isEmpty()) {
            // Provincia no existe - dejar que el flujo normal continúe (mostrará vacío)
            // o podríamos devolver 404. Por ahora mantenemos compatibilidad.
            log.warn("Provincia no encontrada: {}", provinciaTrim);
        } else {
            String nombreCanonico = provinciaOpt.get().getNombre();

            // 301 Redirect si el casing de la URL no coincide con el nombre canónico de la DB
            if (!nombreCanonico.equals(provinciaTrim)) {
                StringBuilder redirectUrl = new StringBuilder("/eventos/provincia/" + UriUtils.encodePath(nombreCanonico, StandardCharsets.UTF_8));
                if (page > 1) {
                    redirectUrl.append("?page=").append(page);
                }
                log.info("Redirigiendo 301: {} -> {}", provinciaTrim, nombreCanonico);
                return "redirect:" + redirectUrl.toString();
            }
        }

        // Usar nombre canónico de la DB para SEO
        String nombreProvinciaCanonico = provinciaOpt.map(Provincia::getNombre).orElse(provinciaTrim);

        LocalDate fechaDesde = LocalDate.now();
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, 20, Sort.by("fecha").ascending().and(Sort.by("artista.nombre").ascending()));

        Page<EventoPublicoDto> paginaEventos = eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(
            nombreProvinciaCanonico, null, null, fechaDesde, null, pageable);

        int totalPaginas = paginaEventos.getTotalPages();
        if (totalPaginas > 0 && page > totalPaginas) {
            String urlBase = "/eventos/provincia/" + UriUtils.encodePath(nombreProvinciaCanonico, StandardCharsets.UTF_8);
            return "redirect:" + urlBase + "?page=" + totalPaginas;
        }

        List<EventoPublicoDto> eventos = paginaEventos.getContent();
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getFecha().toLocalDate(), TreeMap::new, Collectors.toList()));

        String baseUrl = construirBaseUrl(request);
        String pathProvincia = "/eventos/provincia/" + UriUtils.encodePath(nombreProvinciaCanonico, StandardCharsets.UTF_8);
        String canonicalUrl = construirUrlAbsoluta(request, pathProvincia);
        boolean indexable = paginaEventos.getTotalElements() > 0;

        String year = String.valueOf(java.time.Year.now().getValue());
        String titulo = "Fiestas y Orquestas en " + nombreProvinciaCanonico + " " + year + " | Festia";
        String descripcion = "Descubre las fiestas populares y verbenas de " + nombreProvinciaCanonico
            + ". Orquestas, grupos musicales y discotecas móviles con fechas y horarios confirmados.";

        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("eventos", eventos);
        model.addAttribute("provincia", nombreProvinciaCanonico);
        model.addAttribute("titulo", titulo);
        model.addAttribute("descripcion", descripcion);
        model.addAttribute("textoProvincia", TEXTOS_PROVINCIA.getOrDefault(nombreProvinciaCanonico,
            "Consulta las próximas fiestas y verbenas en " + nombreProvinciaCanonico +
            ". Encuentra orquestas, discotecas móviles y grupos musicales con fechas confirmadas."));
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("metaRobots", indexable ? "index,follow" : "noindex,follow");
        model.addAttribute("ogImage", EVENT_IMAGE_URL);

        if (indexable && !eventos.isEmpty()) {
            List<EventoPublicoDto> eventosJsonLd = eventos.stream()
                .filter(EventoPublicoDto::isIndexableForJsonLd)
                .limit(50)
                .collect(Collectors.toList());
            if (!eventosJsonLd.isEmpty()) {
                model.addAttribute("jsonLd", buildItemListJsonLd(eventosJsonLd, baseUrl, titulo, canonicalUrl));
                model.addAttribute("breadcrumbJsonLd", buildBreadcrumbProvinciaJsonLd(baseUrl, nombreProvinciaCanonico));
            }
        }

        model.addAttribute("provincias", obtenerProvinciasOrdenadas());
        // Pre-cargar municipios de esta provincia (no todos los 8000)
        model.addAttribute("municipiosProvincia", localizacionService.findMunicipiosByProvinciaNombre(nombreProvinciaCanonico));
        model.addAttribute("artistasDisponibles", obtenerArtistasOrdenados(eventos));
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", null);
        model.addAttribute("idArtistaSeleccionado", null);
        model.addAttribute("municipio", null);
        model.addAttribute("contextoPagina", "provincia");
        model.addAttribute("urlBase", "/eventos/provincia/" + UriUtils.encodePath(nombreProvinciaCanonico, StandardCharsets.UTF_8));
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("totalEventos", paginaEventos.getTotalElements());
        model.addAttribute("hayPaginaAnterior", page > 1);
        model.addAttribute("hayPaginaSiguiente", page < totalPaginas);
        model.addAttribute("paginaAnterior", page - 1);
        model.addAttribute("paginaSiguiente", page + 1);

        if (page > 1) {
            model.addAttribute("metaRobots", "noindex,follow");
        }

        return "eventos-publicos";
    }

    /**
     * Página indexable de eventos por municipio con JSON-LD Schema.org.
     * Incluye redirección 301 para normalización de casing en URLs.
     */
    @GetMapping("/municipio/{municipio}")
    public Object listarEventosPorMunicipio(
        @PathVariable String municipio,
        @RequestParam(required = false, defaultValue = "1") int page,
        Model model,
        HttpServletRequest request) {

        String municipioTrim = municipio.trim();
        log.info("Listando eventos publicos para municipio: {}", municipioTrim);

        LocalDate fechaDesde = LocalDate.now();
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, 10, Sort.by("fecha").ascending().and(Sort.by("artista.nombre").ascending()));

        Page<EventoPublicoDto> paginaEventos = eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(
            null, municipioTrim, null, fechaDesde, null, pageable);

        int totalPaginas = paginaEventos.getTotalPages();
        if (totalPaginas > 0 && page > totalPaginas) {
            String urlBase = "/eventos/municipio/" + UriUtils.encodePath(municipioTrim, StandardCharsets.UTF_8);
            return "redirect:" + urlBase + "?page=" + totalPaginas;
        }

        List<EventoPublicoDto> eventos = paginaEventos.getContent();
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getFecha().toLocalDate(), TreeMap::new, Collectors.toList()));

        // Para los metadatos SEO y breadcrumb, necesitamos la provincia del municipio.
        // Como la página está paginada, consultamos el primer evento sin paginar para obtener la provincia.
        String provinciaDelMunicipio = eventos.stream()
            .findFirst()
            .map(EventoPublicoDto::getProvincia)
            .orElse("");

        // Buscar provincia canónica y verificar redirección 301
        String nombreProvinciaCanonico = provinciaDelMunicipio;
        if (!provinciaDelMunicipio.isBlank()) {
            Optional<Provincia> provinciaOpt = localizacionService.findProvinciaByNombreUpperCase(provinciaDelMunicipio);
            if (provinciaOpt.isPresent()) {
                nombreProvinciaCanonico = provinciaOpt.get().getNombre();
            }
        }

        // Nota: Para municipios no implementamos redirección 301 de casing porque
        // no tenemos tabla de municipios canónicos en este momento.
        // La provincia sí se normaliza mediante findProvinciaByNombreUpperCase.

        String baseUrl = construirBaseUrl(request);
        String pathMunicipio = "/eventos/municipio/" + UriUtils.encodePath(municipioTrim, StandardCharsets.UTF_8);
        String canonicalUrl = construirUrlAbsoluta(request, pathMunicipio);
        boolean indexable = paginaEventos.getTotalElements() > 0;

        String year = String.valueOf(java.time.Year.now().getValue());
        String tituloConProvincia = nombreProvinciaCanonico.isBlank()
            ? "Fiestas y Orquestas en " + municipioTrim + " " + year + " | Festia"
            : "Fiestas y Orquestas en " + municipioTrim + ", " + nombreProvinciaCanonico + " " + year + " | Festia";
        String descripcion = "Consulta las fiestas y verbenas de " + municipioTrim
            + ". Orquestas, grupos musicales y discotecas móviles con fechas, horarios y lugares confirmados.";

        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("eventos", eventos);
        model.addAttribute("municipio", municipioTrim);
        model.addAttribute("provincia", nombreProvinciaCanonico);
        model.addAttribute("titulo", tituloConProvincia);
        model.addAttribute("descripcion", descripcion);
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("metaRobots", indexable ? "index,follow" : "noindex,follow");
        model.addAttribute("ogImage", EVENT_IMAGE_URL);

        if (indexable && !eventos.isEmpty()) {
            List<EventoPublicoDto> eventosJsonLd = eventos.stream()
                .filter(EventoPublicoDto::isIndexableForJsonLd)
                .limit(50)
                .collect(Collectors.toList());
            if (!eventosJsonLd.isEmpty()) {
                model.addAttribute("jsonLd", buildItemListJsonLd(eventosJsonLd, baseUrl, tituloConProvincia, canonicalUrl));
                model.addAttribute("breadcrumbJsonLd",
                    buildBreadcrumbMunicipioJsonLd(baseUrl, municipioTrim, nombreProvinciaCanonico));
            }
        }

        model.addAttribute("provincias", obtenerProvinciasOrdenadas());
        // Pre-cargar municipios de la provincia de este municipio
        model.addAttribute("municipiosProvincia",
            nombreProvinciaCanonico.isBlank() ? List.of() : localizacionService.findMunicipiosByProvinciaNombre(nombreProvinciaCanonico));
        model.addAttribute("artistasDisponibles", obtenerArtistasOrdenados(eventos));
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", null);
        model.addAttribute("idArtistaSeleccionado", null);
        model.addAttribute("contextoPagina", "municipio");
        model.addAttribute("urlBase", "/eventos/municipio/" + UriUtils.encodePath(municipioTrim, StandardCharsets.UTF_8));
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("totalEventos", paginaEventos.getTotalElements());
        model.addAttribute("hayPaginaAnterior", page > 1);
        model.addAttribute("hayPaginaSiguiente", page < totalPaginas);
        model.addAttribute("paginaAnterior", page - 1);
        model.addAttribute("paginaSiguiente", page + 1);

        if (page > 1) {
            model.addAttribute("metaRobots", "noindex,follow");
        }

        return "eventos-publicos";
    }

    /**
     * API REST: Obtener eventos por municipio.
     */
    @GetMapping(value = "/api/municipio/{municipio}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<EventoPublicoDto>> obtenerEventosApiPorMunicipio(
        @PathVariable String municipio,
        @RequestParam(required = false) String desde,
        @RequestParam(required = false) String hasta) {

        log.info("API: Obteniendo eventos para municipio: {}", municipio);

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

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorMunicipio(
            municipio.trim(), fechaDesde, fechaHasta);
        return ResponseEntity.ok(eventos);
    }

    /**
     * API REST: Obtener municipios por provincia (para filtros AJAX).
     * Province name resolution is case-insensitive.
     */
    @GetMapping(value = "/api/municipios", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> obtenerMunicipiosPorProvincia(
        @RequestParam(required = false) String provincia) {

        // 400: Missing parameter
        if (provincia == null || provincia.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Parámetro 'provincia' obligatorio"));
        }

        String provinciaTrim = provincia.trim();
        log.info("API: Obteniendo municipios para provincia: {}", provinciaTrim);

        // Verify province exists (404 if not found)
        Optional<Provincia> provinciaOpt = localizacionService.findProvinciaByNombreUpperCase(provinciaTrim);
        if (provinciaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Provincia no encontrada"));
        }

        // Get canonical province name from DB
        String nombreCanonico = provinciaOpt.get().getNombre();

        // Get municipalities
        List<CodigoNombreRecord> municipios = localizacionService.findMunicipiosByProvinciaNombre(provinciaTrim);

        // Map to response DTO with canonical province name
        List<MunicipioResponse> response = municipios.stream()
            .map(m -> new MunicipioResponse(m.nombre(), nombreCanonico))
            .toList();

        return ResponseEntity.ok(response);
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
        return construirTituloListado(provincia, municipio, idArtista, 0);
    }

    private String construirTituloListado(String provincia, String municipio, Long idArtista, int totalEventos) {
        String year = String.valueOf(java.time.Year.now().getValue());
        if (idArtista != null) {
            return "Conciertos y Fechas del Artista | Festia";
        }
        if (municipio != null && !municipio.isBlank() && provincia != null && !provincia.isBlank()) {
            return "Fiestas y Orquestas en " + municipio + ", " + provincia + " " + year + " | Festia";
        }
        if (provincia != null && !provincia.isBlank()) {
            return "Fiestas y Orquestas en " + provincia + " " + year + " | Festia";
        }
        if (totalEventos > 0) {
            return "Fiestas, Verbenas y Orquestas en España " + year + " | Festia";
        }
        return "Fiestas, Verbenas y Orquestas en España | Festia";
    }

    private String construirDescripcionListado(String provincia, String municipio, Long idArtista) {
        if (idArtista != null) {
            return "Descubre todas las fechas confirmadas del artista. Consulta conciertos, verbenas y fiestas populares con horarios y ubicaciones en Festia.";
        }
        if (municipio != null && !municipio.isBlank() && provincia != null && !provincia.isBlank()) {
            return "Consulta las fiestas y verbenas de " + municipio + " (" + provincia + "). Orquestas, discotecas móviles y grupos musicales con fechas y horarios confirmados.";
        }
        if (provincia != null && !provincia.isBlank()) {
            return "Agenda de fiestas populares y verbenas en " + provincia + ". Encuentra orquestas, grupos musicales y discotecas móviles filtrando por municipio y fecha.";
        }
        return "Consulta la agenda de fiestas, verbenas y orquestas en España. " +
            "Descubre conciertos y actuaciones cerca de ti filtrando por provincia, municipio, artista y fecha.";
    }

    // ── Calendario helpers ──────────────────────────────────────────────────────

    private String buildGoogleCalendarUrl(EventoPublicoDto evento, String baseUrl) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String start = evento.getFecha().format(fmt);
        String end   = evento.getFecha().plusHours(3).format(fmt);
        String location = (evento.getLugarParaMapa() != null ? evento.getLugarParaMapa() + ", " : "")
            + evento.getMunicipio() + ", " + evento.getProvincia();

        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
            + "&text=" + UriUtils.encodeQueryParam(evento.getTituloEvento(), StandardCharsets.UTF_8)
            + "&dates=" + start + "/" + end
            + "&details=" + UriUtils.encodeQueryParam(evento.getDescripcionSeo(), StandardCharsets.UTF_8)
            + "&location=" + UriUtils.encodeQueryParam(location, StandardCharsets.UTF_8)
            + "&sprop=url:" + UriUtils.encodeQueryParam(baseUrl + evento.getPathPublico(), StandardCharsets.UTF_8);
    }

    private String buildIcal(EventoPublicoDto evento) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String now   = LocalDateTime.now().format(fmt) + "Z";
        String start = evento.getFecha().format(fmt);
        String end   = evento.getFecha().plusHours(3).format(fmt);
        String location = (evento.getLugarParaMapa() != null ? evento.getLugarParaMapa() + ", " : "")
            + evento.getMunicipio() + ", " + evento.getProvincia();

        return "BEGIN:VCALENDAR\r\n"
            + "VERSION:2.0\r\n"
            + "PRODID:-//festia.es//Festia//ES\r\n"
            + "BEGIN:VEVENT\r\n"
            + "UID:" + evento.getId() + "@festia.es\r\n"
            + "DTSTAMP:" + now + "\r\n"
            + "DTSTART;TZID=Europe/Madrid:" + start + "\r\n"
            + "DTEND;TZID=Europe/Madrid:" + end + "\r\n"
            + "SUMMARY:" + escapeIcal(evento.getTituloEvento()) + "\r\n"
            + "DESCRIPTION:" + escapeIcal(evento.getDescripcionSeo()) + "\r\n"
            + "LOCATION:" + escapeIcal(location) + "\r\n"
            + "END:VEVENT\r\n"
            + "END:VCALENDAR";
    }

    private String escapeIcal(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    // ── JSON-LD helpers ─────────────────────────────────────────────────────────

    private String serializarJsonLd(Object data) {
        try {
            return objectMapper.writeValueAsString(data).replace("</", "<\\/");
        } catch (JsonProcessingException ex) {
            log.error("Error serializando JSON-LD", ex);
            return "{}";
        }
    }

    /**
     * Genera JSON-LD ItemList con los MusicEvent de la lista de eventos.
     */
    private String buildItemListJsonLd(List<EventoPublicoDto> eventos, String baseUrl, String listName, String listUrl) {
        List<Map<String, Object>> items = new ArrayList<>();
        int position = 1;
        for (EventoPublicoDto evento : eventos) {
            String organizerName = (evento.getNombreAgencia() != null && !evento.getNombreAgencia().isBlank())
                ? evento.getNombreAgencia() : ORGANIZER_NAME_FALLBACK;
String imageUrl = (evento.getLogoArtista() != null && !evento.getLogoArtista().isBlank())
                ? EventoPublicoDto.normalizeImageUrl(evento.getLogoArtista()) : EVENT_IMAGE_URL;
            Map<String, Object> listItem = new LinkedHashMap<>();
            listItem.put("@type", "ListItem");
            listItem.put("position", position++);
            listItem.put("item", evento.toJsonLdMap(baseUrl, organizerName, evento.getUrlOrganizador(), imageUrl));
            items.add(listItem);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@type", "ItemList");
        root.put("name", listName);
        root.put("url", listUrl);
        root.put("numberOfItems", items.size());
        root.put("itemListElement", items);

        return serializarJsonLd(root);
    }

    /**
     * Genera JSON-LD combinado [MusicGroup, ItemList] para la página de artista.
     */
    private String buildArtistaJsonLd(List<EventoPublicoDto> eventos, String baseUrl, String artistaUrl,
                                      String listName, String listDescription, String imageUrl) {
        EventoPublicoDto primerEvento = eventos.get(0);

        Map<String, Object> musicGroup = new LinkedHashMap<>();
        musicGroup.put("@context", "https://schema.org");
        musicGroup.put("@type", "MusicGroup");
        musicGroup.put("name", primerEvento.getNombreArtista());
        musicGroup.put("url", artistaUrl);
        if (imageUrl != null && !imageUrl.isBlank()) {
            musicGroup.put("image", imageUrl);
        }
        musicGroup.put("description", listDescription);

        List<Map<String, Object>> items = new ArrayList<>();
        int position = 1;
        for (EventoPublicoDto evento : eventos.subList(0, Math.min(eventos.size(), 50))) {
            String organizerName = (evento.getNombreAgencia() != null && !evento.getNombreAgencia().isBlank())
                ? evento.getNombreAgencia() : ORGANIZER_NAME_FALLBACK;
            String evtImage = (evento.getLogoArtista() != null && !evento.getLogoArtista().isBlank())
                ? EventoPublicoDto.normalizeImageUrl(evento.getLogoArtista()) : EVENT_IMAGE_URL;
            Map<String, Object> listItem = new LinkedHashMap<>();
            listItem.put("@type", "ListItem");
            listItem.put("position", position++);
            listItem.put("item", evento.toJsonLdMap(baseUrl, organizerName, evento.getUrlOrganizador(), evtImage));
            items.add(listItem);
        }

        Map<String, Object> itemList = new LinkedHashMap<>();
        itemList.put("@context", "https://schema.org");
        itemList.put("@type", "ItemList");
        itemList.put("name", listName);
        itemList.put("url", artistaUrl);
        itemList.put("numberOfItems", items.size());
        itemList.put("itemListElement", items);

        return serializarJsonLd(List.of(musicGroup, itemList));
    }

    /**
     * Genera JSON-LD BreadcrumbList para la página de provincia (3 niveles).
     */
    private String buildBreadcrumbProvinciaJsonLd(String baseUrl, String provincia) {
        List<Map<String, Object>> breadcrumbs = new ArrayList<>();

        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("@type", "ListItem");
        item1.put("position", 1);
        item1.put("name", "Festia");
        item1.put("item", baseUrl);
        breadcrumbs.add(item1);

        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("@type", "ListItem");
        item2.put("position", 2);
        item2.put("name", "Eventos");
        item2.put("item", baseUrl + "/eventos");
        breadcrumbs.add(item2);

        Map<String, Object> item3 = new LinkedHashMap<>();
        item3.put("@type", "ListItem");
        item3.put("position", 3);
        item3.put("name", provincia);
        item3.put("item", baseUrl + "/eventos/provincia/" + UriUtils.encodePath(provincia, StandardCharsets.UTF_8));
        breadcrumbs.add(item3);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@type", "BreadcrumbList");
        root.put("itemListElement", breadcrumbs);

        return serializarJsonLd(root);
    }

    /**
     * Genera JSON-LD BreadcrumbList para la página de municipio (4 niveles).
     */
    private String buildBreadcrumbMunicipioJsonLd(String baseUrl, String municipio, String provincia) {
        List<Map<String, Object>> breadcrumbs = new ArrayList<>();

        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("@type", "ListItem");
        item1.put("position", 1);
        item1.put("name", "Festia");
        item1.put("item", baseUrl);
        breadcrumbs.add(item1);

        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("@type", "ListItem");
        item2.put("position", 2);
        item2.put("name", "Eventos");
        item2.put("item", baseUrl + "/eventos");
        breadcrumbs.add(item2);

        if (!provincia.isBlank()) {
            Map<String, Object> item3 = new LinkedHashMap<>();
            item3.put("@type", "ListItem");
            item3.put("position", 3);
            item3.put("name", provincia);
            item3.put("item", baseUrl + "/eventos/provincia/" + UriUtils.encodePath(provincia, StandardCharsets.UTF_8));
            breadcrumbs.add(item3);
        }

        Map<String, Object> itemMunicipio = new LinkedHashMap<>();
        itemMunicipio.put("@type", "ListItem");
        itemMunicipio.put("position", provincia.isBlank() ? 3 : 4);
        itemMunicipio.put("name", municipio);
        itemMunicipio.put("item", baseUrl + "/eventos/municipio/" + UriUtils.encodePath(municipio, StandardCharsets.UTF_8));
        breadcrumbs.add(itemMunicipio);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@type", "BreadcrumbList");
        root.put("itemListElement", breadcrumbs);

        return serializarJsonLd(root);
    }

    /**
     * Genera JSON-LD BreadcrumbList para la página de evento individual.
     */
    private String buildBreadcrumbJsonLd(String baseUrl, EventoPublicoDto evento) {
        List<Map<String, Object>> breadcrumbs = new ArrayList<>();

        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("@type", "ListItem");
        item1.put("position", 1);
        item1.put("name", "Festia");
        item1.put("item", baseUrl);
        breadcrumbs.add(item1);

        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("@type", "ListItem");
        item2.put("position", 2);
        item2.put("name", "Eventos");
        item2.put("item", baseUrl + "/eventos");
        breadcrumbs.add(item2);

        Map<String, Object> item3 = new LinkedHashMap<>();
        item3.put("@type", "ListItem");
        item3.put("position", 3);
        item3.put("name", evento.getNombreArtista());
        item3.put("item", baseUrl + "/eventos/artista/" + evento.getIdArtista());
        breadcrumbs.add(item3);

        Map<String, Object> item4 = new LinkedHashMap<>();
        item4.put("@type", "ListItem");
        item4.put("position", 4);
        item4.put("name", evento.getTituloEvento());
        breadcrumbs.add(item4);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@type", "BreadcrumbList");
        root.put("itemListElement", breadcrumbs);

        return serializarJsonLd(root);
    }
}
