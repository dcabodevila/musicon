package es.musicalia.gestmusica.eventopublico;
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
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
    private final EventoPublicoService eventoPublicoService;
    private final EventoPublicoCatalogoFacade eventoPublicoCatalogoFacade;
    private final LocalizacionService localizacionService;
    private final EventoPublicStructuredDataBuilder eventoPublicStructuredDataBuilder;
    private final EventoPublicoDateWindow eventoPublicoDateWindow;

    /**
     * URL legada: /eventos/evento/{id}
     * Redirecciona 301 a la URL canonica con slug.
     */
    @GetMapping("/evento/{id:\\d+}")
    public RedirectView redirigirEventoPublicoLegacy(@PathVariable Long id, HttpServletRequest request) {
        EventoPublicoDto evento = obtenerEventoONotFound(id);
        return EventoPublicoUrlHelper.crearRedireccionPermanente(
            EventoPublicoUrlHelper.construirUrlAbsoluta(request, evento.getPathPublico())
        );
    }

    /**
     * URL canonica: /eventos/evento/{id}-{slug}
     */
    @GetMapping("/evento/{id:\\d+}-{slug}")
    public ModelAndView verEventoPublico(@PathVariable Long id, @PathVariable String slug, HttpServletRequest request) {
        log.info("Acceso a evento publico: {}", id);

        EventoPublicoDto evento = obtenerEventoONotFound(id);
        String slugCanonico = evento.getSlug();
        String urlCanonica = EventoPublicoUrlHelper.construirUrlAbsoluta(request, evento.getPathPublico());

        if (!slugCanonico.equals(slug)) {
            return new ModelAndView(EventoPublicoUrlHelper.crearRedireccionPermanente(urlCanonica));
        }

        ModelAndView mv = new ModelAndView("evento-publico");
        mv.addObject("evento", evento);
        String imageUrl = (evento.getLogoArtista() != null && !evento.getLogoArtista().isBlank())
            ? EventoPublicoDto.normalizeImageUrl(evento.getLogoArtista())
            : EVENT_IMAGE_URL;
        mv.addObject("jsonLd", eventoPublicStructuredDataBuilder.buildEventJsonLd(evento, urlCanonica, imageUrl));
        String baseUrl = EventoPublicoUrlHelper.construirBaseUrl(request);
        mv.addObject("breadcrumbJsonLd", eventoPublicStructuredDataBuilder.buildBreadcrumbEventoJsonLd(baseUrl, evento));
        mv.addObject("titulo", evento.getTituloSeo());
        mv.addObject("descripcion", evento.getDescripcionSeo());
        mv.addObject("ogImage", imageUrl);
        mv.addObject("urlEvento", urlCanonica);
        mv.addObject("canonicalUrl", urlCanonica);
        mv.addObject("metaRobots", "index,follow");

        LocalDate fechaDesdeRelacionados = eventoPublicoDateWindow.today();
        List<EventoPublicoDto> eventosRelacionados = eventoPublicoService.obtenerEventosRelacionadosPublicos(
            id,
            evento.getIdArtista(),
            fechaDesdeRelacionados,
            eventoPublicoDateWindow.publicHorizon(),
            10
        );
        mv.addObject("eventosRelacionados", eventosRelacionados);
        mv.addObject("googleCalendarUrl", EventoPublicoCalendarLinks.buildGoogleCalendarUrl(evento, baseUrl));

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
            .body(EventoPublicoCalendarLinks.buildIcal(evento));
    }

    @GetMapping(value = "/artista/{idArtista}/calendar/{token}.ics")
    @ResponseBody
    public ResponseEntity<String> descargarCalendarioArtista(@PathVariable Long idArtista, @PathVariable String token) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
            .header("Content-Disposition", "inline; filename=\"festia-artista-" + idArtista + ".ics\"")
            .body(eventoPublicoService.obtenerFeedCalendarioArtista(idArtista, token));
    }

    /**
     * Lista de eventos publicos de un artista.
     */
    @GetMapping("/artista/{idArtista}")
    public String listarEventosPorArtista(@PathVariable Long idArtista, Model model, HttpServletRequest request) {
        log.info("Listando eventos publicos para artista: {}", idArtista);

        EventoPublicoDateWindow.DateRange dateRange = eventoPublicoDateWindow.effectiveUpcomingWindow(null, null);
        LocalDate fechaDesde = dateRange.fechaDesde();
        LocalDate fechaHasta = dateRange.fechaHasta();

        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, idArtista, fechaDesde, fechaHasta);
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getFecha().toLocalDate(), TreeMap::new, Collectors.toList()));

        String canonicalUrl = EventoPublicoUrlHelper.construirUrlAbsoluta(request, "/eventos/artista/" + idArtista);
        boolean indexable = !eventos.isEmpty();

        model.addAttribute("eventos", eventos);
        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("idArtista", idArtista);
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("metaRobots", indexable ? "index,follow" : "noindex,follow");
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", fechaHasta.toString());
        model.addAttribute("provincia", null);
        model.addAttribute("municipio", null);
        model.addAttribute("idArtistaSeleccionado", idArtista);
        List<EventoPublicoDto> eventosCatalogo = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, null, fechaDesde, fechaHasta);
        model.addAttribute("provincias", eventoPublicoCatalogoFacade.obtenerProvinciasPublicasOrdenadas());
        // Municipio select starts empty (AJAX loaded)
        model.addAttribute("municipiosProvincia", List.of());
        model.addAttribute("artistasDisponibles", eventoPublicoCatalogoFacade.obtenerArtistasOrdenados(eventosCatalogo));
        model.addAttribute("quickLinks", eventoPublicoCatalogoFacade.obtenerQuickLinksPublicos());
        model.addAttribute("contextoPagina", "artista");

        if (!eventos.isEmpty()) {
            EventoPublicoDto primerEvento = eventos.get(0);
            String nombreArtista = primerEvento.getNombreArtista();
            String titulo = "Conciertos y Fechas de " + nombreArtista + " | Festia";
            String descripcion = "Descubre todas las fechas confirmadas de " + nombreArtista
                + ". Fiestas, verbenas y actuaciones con horarios y ubicaciones en Festia.";
            String ogImage = (primerEvento.getLogoArtista() != null && !primerEvento.getLogoArtista().isBlank())
                ? EventoPublicoDto.normalizeImageUrl(primerEvento.getLogoArtista()) : EVENT_IMAGE_URL;
            String baseUrl = EventoPublicoUrlHelper.construirBaseUrl(request);

            model.addAttribute("nombreArtista", nombreArtista);
            model.addAttribute("titulo", titulo);
            model.addAttribute("descripcion", descripcion);
            model.addAttribute("ogImage", ogImage);
            model.addAttribute("jsonLd", eventoPublicStructuredDataBuilder.buildArtistaJsonLd(eventos, baseUrl, canonicalUrl, titulo, descripcion, ogImage));
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

        EventoPublicoDateWindow.DateRange dateRange = eventoPublicoDateWindow.effectiveUpcomingWindow(fechaDesde, fechaHasta);
        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorProvincia(
            provincia,
            dateRange.fechaDesde(),
            dateRange.fechaHasta()
        );
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
        String baseUrl = EventoPublicoUrlHelper.construirBaseUrl(request);

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

        // Landing de eventos de hoy (alta prioridad, cambia diariamente)
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(baseUrl).append("/eventos/hoy</loc>\n");
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
            .filter(e -> e.getProvincia() != null && !e.getProvincia().isBlank())
            .filter(e -> !eventoPublicoCatalogoFacade.esProvinciaExcluidaPublica(e.getProvincia()))
            .collect(Collectors.groupingBy(e -> eventoPublicoCatalogoFacade.normalizarProvinciaCanonica(e.getProvincia())));
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
            .filter(e -> e.getProvincia() != null && !eventoPublicoCatalogoFacade.esProvinciaExcluidaPublica(e.getProvincia()))
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
     * Landing publica estable con las actuaciones musicales del dia actual.
     */
    @GetMapping("/hoy")
    public String listarEventosHoy(Model model, HttpServletRequest request) {
        log.info("Listando eventos publicos de hoy");

        LocalDate hoy = eventoPublicoDateWindow.today();
        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosFiltrados(
            null, null, null, hoy, hoy);
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getFecha().toLocalDate(), TreeMap::new, Collectors.toList()));

        String titulo = "Fiestas hoy, verbenas y actuaciones musicales | Festia";
        String descripcion = "Consulta fiestas hoy, verbenas hoy y actuaciones musicales de hoy en España. Orquestas, grupos y artistas con fecha, hora y ubicación en Festia.";
        String canonicalUrl = EventoPublicoUrlHelper.construirUrlAbsoluta(request, "/eventos/hoy");
        String baseUrl = EventoPublicoUrlHelper.construirBaseUrl(request);
        boolean indexable = !eventos.isEmpty();

        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("eventos", eventos);
        model.addAttribute("titulo", titulo);
        model.addAttribute("descripcion", descripcion);
        model.addAttribute("fechaDesde", hoy.toString());
        model.addAttribute("fechaHasta", hoy.toString());
        model.addAttribute("fechaMaxFiltro", hoy.toString());
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("metaRobots", indexable ? "index,follow" : "noindex,follow");
        model.addAttribute("provincia", null);
        model.addAttribute("municipio", null);
        model.addAttribute("idArtistaSeleccionado", null);
        model.addAttribute("provincias", eventoPublicoCatalogoFacade.obtenerProvinciasPublicasOrdenadas());
        model.addAttribute("municipiosProvincia", List.of());
        model.addAttribute("artistasDisponibles", eventoPublicoCatalogoFacade.obtenerArtistasOrdenados(eventos));
        model.addAttribute("quickLinks", eventoPublicoCatalogoFacade.obtenerQuickLinksPublicos());
        model.addAttribute("ogImage", EVENT_IMAGE_URL);
        model.addAttribute("contextoPagina", "hoy");
        model.addAttribute("urlBase", "/eventos/hoy");
        model.addAttribute("paginaActual", 1);
        model.addAttribute("totalPaginas", 1);
        model.addAttribute("totalEventos", eventos.size());
        model.addAttribute("hayPaginaAnterior", false);
        model.addAttribute("hayPaginaSiguiente", false);
        model.addAttribute("textoHoy", "Actuaciones de orquestas y bandas musicales confirmadas para el día actual. Disfruta las festias!");

        if (indexable) {
            List<EventoPublicoDto> eventosJsonLd = eventos.stream()
                .filter(EventoPublicoDto::isIndexableForJsonLd)
                .limit(50)
                .collect(Collectors.toList());
            if (!eventosJsonLd.isEmpty()) {
                model.addAttribute("jsonLd", eventoPublicStructuredDataBuilder.buildItemListJsonLd(eventosJsonLd, baseUrl, titulo, canonicalUrl));
                model.addAttribute("breadcrumbJsonLd", eventoPublicStructuredDataBuilder.buildBreadcrumbHoyJsonLd(baseUrl));
            }
        }

        return "eventos-publicos";
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

        LocalDate fechaDesdeSolicitada = null;
        LocalDate fechaHastaSolicitada = null;

        try {
            if (desde != null && !desde.isBlank()) {
                fechaDesdeSolicitada = LocalDate.parse(desde, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (hasta != null && !hasta.isBlank()) {
                fechaHastaSolicitada = LocalDate.parse(hasta, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception e) {
            log.error("Error parseando fechas: {}", e.getMessage());
        }

        EventoPublicoDateWindow.DateRange dateRange = eventoPublicoDateWindow.effectiveUpcomingWindow(fechaDesdeSolicitada, fechaHastaSolicitada);
        LocalDate fechaDesde = dateRange.fechaDesde();
        LocalDate fechaHasta = dateRange.fechaHasta();

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, 20, Sort.by("fecha").ascending().and(Sort.by("artista.nombre").ascending()));

        EventoPublicoCatalogoFacade.EventoPublicoCatalogoView catalogoView = eventoPublicoCatalogoFacade.prepararCatalogoPublico(
            new EventoPublicoCatalogoFacade.EventoPublicoCatalogoRequest(
                provincia, municipio, idArtista, fechaDesde, fechaHasta, pageable, page
            )
        );

        Page<EventoPublicoDto> paginaEventos = filtrarPaginaPorRango(catalogoView.paginaEventos(), fechaDesde, fechaHasta);

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

        String titulo = catalogoView.titulo();
        String descripcion = catalogoView.descripcion();
        String canonicalUrl = EventoPublicoUrlHelper.construirUrlAbsoluta(request, "/eventos");

        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("eventos", eventos);
        model.addAttribute("titulo", titulo);
        model.addAttribute("descripcion", descripcion);
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", fechaHasta.toString());
        model.addAttribute("fechaMaxFiltro", eventoPublicoDateWindow.publicHorizon().toString());
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("metaRobots", noIndex ? "noindex,follow" : "index,follow");
        model.addAttribute("provincia", provincia);
        model.addAttribute("municipio", municipio);
        model.addAttribute("idArtistaSeleccionado", idArtista);
        model.addAttribute("provincias", catalogoView.provincias());
        model.addAttribute("municipiosProvincia", catalogoView.municipiosProvincia());
        model.addAttribute("artistasDisponibles", catalogoView.artistasDisponibles());
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
        model.addAttribute("quickLinks", catalogoView.quickLinks());

        if (page > 1) {
            model.addAttribute("metaRobots", "noindex,follow");
        }

        if (!noIndex && !eventos.isEmpty()) {
            String baseUrl = EventoPublicoUrlHelper.construirBaseUrl(request);
            List<EventoPublicoDto> eventosParaJsonLd = eventos.stream()
                .filter(EventoPublicoDto::isIndexableForJsonLd)
                .limit(50)
                .collect(Collectors.toList());
            if (!eventosParaJsonLd.isEmpty()) {
                model.addAttribute("jsonLd", eventoPublicStructuredDataBuilder.buildItemListJsonLd(eventosParaJsonLd, baseUrl, titulo, canonicalUrl));
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

        String provinciaTrim = UriUtils.decode(provincia.trim(), StandardCharsets.UTF_8);
        String provinciaPublica = eventoPublicoCatalogoFacade.normalizarProvinciaCanonica(provinciaTrim);
        log.info("Listando eventos publicos para provincia: {}", provinciaTrim);

        if (!provinciaPublica.equals(provinciaTrim)) {
            StringBuilder redirectUrl = new StringBuilder("/eventos/provincia/" + UriUtils.encodePath(provinciaPublica, StandardCharsets.UTF_8));
            if (page > 1) {
                redirectUrl.append("?page=").append(page);
            }
            log.info("Redirigiendo 301 alias provincia: {} -> {}", provinciaTrim, provinciaPublica);
            return EventoPublicoUrlHelper.crearRedireccionPermanente(
                EventoPublicoUrlHelper.construirUrlAbsoluta(request, redirectUrl.toString())
            );
        }

        // Buscar provincia para obtener nombre canónico y validar existencia
        String nombreProvinciaConsulta = eventoPublicoCatalogoFacade.normalizarProvinciaParaConsulta(provinciaPublica);
        Optional<Provincia> provinciaOpt = localizacionService.findProvinciaByNombreUpperCase(nombreProvinciaConsulta);

        if (provinciaOpt.isEmpty()) {
            // Provincia no existe - dejar que el flujo normal continúe (mostrará vacío)
            // o podríamos devolver 404. Por ahora mantenemos compatibilidad.
            log.warn("Provincia no encontrada: {}", provinciaTrim);
        } else {
            String nombreCanonico = eventoPublicoCatalogoFacade.normalizarProvinciaCanonica(provinciaOpt.get().getNombre());

            // 301 Redirect si el casing de la URL no coincide con el nombre canónico de la DB
            if (!nombreCanonico.equals(provinciaPublica)) {
                StringBuilder redirectUrl = new StringBuilder("/eventos/provincia/" + UriUtils.encodePath(nombreCanonico, StandardCharsets.UTF_8));
                if (page > 1) {
                    redirectUrl.append("?page=").append(page);
                }
                log.info("Redirigiendo 301: {} -> {}", provinciaTrim, nombreCanonico);
                return EventoPublicoUrlHelper.crearRedireccionPermanente(
                    EventoPublicoUrlHelper.construirUrlAbsoluta(request, redirectUrl.toString())
                );
            }
        }

        // Usar nombre canónico de la DB para SEO
        String nombreProvinciaCanonico = provinciaOpt
            .map(Provincia::getNombre)
            .map(eventoPublicoCatalogoFacade::normalizarProvinciaCanonica)
            .orElse(provinciaPublica);
        String nombreProvinciaConsultaCanonico = provinciaOpt
            .map(Provincia::getNombre)
            .orElse(nombreProvinciaConsulta);

        LocalDate fechaDesde = eventoPublicoDateWindow.today();
        LocalDate fechaHasta = eventoPublicoDateWindow.publicHorizon();
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, 20, Sort.by("fecha").ascending().and(Sort.by("artista.nombre").ascending()));

        Page<EventoPublicoDto> paginaEventos = filtrarPaginaPorRango(
            eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(
                nombreProvinciaConsultaCanonico, null, null, fechaDesde, fechaHasta, pageable),
            fechaDesde,
            fechaHasta
        );

        int totalPaginas = paginaEventos.getTotalPages();
        if (totalPaginas > 0 && page > totalPaginas) {
            String urlBase = "/eventos/provincia/" + UriUtils.encodePath(nombreProvinciaCanonico, StandardCharsets.UTF_8);
            return "redirect:" + urlBase + "?page=" + totalPaginas;
        }

        List<EventoPublicoDto> eventos = paginaEventos.getContent();
        Map<LocalDate, List<EventoPublicoDto>> eventosPorDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getFecha().toLocalDate(), TreeMap::new, Collectors.toList()));

        String baseUrl = EventoPublicoUrlHelper.construirBaseUrl(request);
        String pathProvincia = "/eventos/provincia/" + UriUtils.encodePath(nombreProvinciaCanonico, StandardCharsets.UTF_8);
        String canonicalUrl = EventoPublicoUrlHelper.construirUrlAbsoluta(request, pathProvincia);
        boolean indexable = paginaEventos.getTotalElements() > 0;

        String year = String.valueOf(java.time.Year.now().getValue());
        ProvinciaSeoMetadata.SeoCopy seoProvinciaCopy = obtenerSeoProvinciaCopy(nombreProvinciaCanonico, year);
        String titulo = seoProvinciaCopy.titulo();
        String descripcion = seoProvinciaCopy.descripcion();

        model.addAttribute("eventosPorDia", eventosPorDia);
        model.addAttribute("eventos", eventos);
        model.addAttribute("provincia", nombreProvinciaCanonico);
        model.addAttribute("titulo", titulo);
        model.addAttribute("descripcion", descripcion);
        model.addAttribute("textoProvincia", ProvinciaSeoMetadata.textoProvinciaPara(nombreProvinciaCanonico));
        model.addAttribute("seoProvinciaTitulo", seoProvinciaCopy.bloqueTitulo());
        model.addAttribute("seoProvinciaParrafo1", seoProvinciaCopy.parrafo1());
        model.addAttribute("seoProvinciaParrafo2", seoProvinciaCopy.parrafo2());
        model.addAttribute("seoProvinciaParrafo3", seoProvinciaCopy.parrafo3());
        model.addAttribute("seoProvinciaParrafo4", seoProvinciaCopy.parrafo4());
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("metaRobots", indexable ? "index,follow" : "noindex,follow");
        model.addAttribute("ogImage", EVENT_IMAGE_URL);

        if (indexable && !eventos.isEmpty()) {
            List<EventoPublicoDto> eventosJsonLd = eventos.stream()
                .filter(EventoPublicoDto::isIndexableForJsonLd)
                .limit(50)
                .collect(Collectors.toList());
            if (!eventosJsonLd.isEmpty()) {
                model.addAttribute("jsonLd", eventoPublicStructuredDataBuilder.buildItemListJsonLd(eventosJsonLd, baseUrl, titulo, canonicalUrl));
                model.addAttribute("breadcrumbJsonLd", eventoPublicStructuredDataBuilder.buildBreadcrumbProvinciaJsonLd(baseUrl, nombreProvinciaCanonico));
            }
        }

        model.addAttribute("provincias", eventoPublicoCatalogoFacade.obtenerProvinciasPublicasOrdenadas());
        // Pre-cargar municipios de esta provincia (no todos los 8000)
        model.addAttribute("municipiosProvincia", eventoPublicoCatalogoFacade.obtenerMunicipiosPublicosPorProvincia(nombreProvinciaConsultaCanonico));
        model.addAttribute("artistasDisponibles", eventoPublicoCatalogoFacade.obtenerArtistasOrdenados(eventos));
        model.addAttribute("quickLinks", eventoPublicoCatalogoFacade.obtenerQuickLinksPublicos(nombreProvinciaCanonico, null));
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", fechaHasta.toString());
        model.addAttribute("fechaMaxFiltro", fechaHasta.toString());
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

        LocalDate fechaDesde = eventoPublicoDateWindow.today();
        LocalDate fechaHasta = eventoPublicoDateWindow.publicHorizon();
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, 10, Sort.by("fecha").ascending().and(Sort.by("artista.nombre").ascending()));

        Page<EventoPublicoDto> paginaEventos = filtrarPaginaPorRango(
            eventoPublicoService.obtenerEventosPublicosFiltradosPaginados(
                null, municipioTrim, null, fechaDesde, fechaHasta, pageable),
            fechaDesde,
            fechaHasta
        );

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
        String nombreProvinciaCanonico = eventoPublicoCatalogoFacade.normalizarProvinciaCanonica(provinciaDelMunicipio);
        String nombreProvinciaConsultaCanonico = provinciaDelMunicipio;
        if (!provinciaDelMunicipio.isBlank()) {
            Optional<Provincia> provinciaOpt = localizacionService.findProvinciaByNombreUpperCase(provinciaDelMunicipio);
            if (provinciaOpt.isPresent()) {
                nombreProvinciaConsultaCanonico = provinciaOpt.get().getNombre();
                nombreProvinciaCanonico = eventoPublicoCatalogoFacade.normalizarProvinciaCanonica(nombreProvinciaConsultaCanonico);
            }
        }

        // Nota: Para municipios no implementamos redirección 301 de casing porque
        // no tenemos tabla de municipios canónicos en este momento.
        // La provincia sí se normaliza mediante findProvinciaByNombreUpperCase.

        String baseUrl = EventoPublicoUrlHelper.construirBaseUrl(request);
        String pathMunicipio = "/eventos/municipio/" + UriUtils.encodePath(municipioTrim, StandardCharsets.UTF_8);
        String canonicalUrl = EventoPublicoUrlHelper.construirUrlAbsoluta(request, pathMunicipio);
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
                model.addAttribute("jsonLd", eventoPublicStructuredDataBuilder.buildItemListJsonLd(eventosJsonLd, baseUrl, tituloConProvincia, canonicalUrl));
                model.addAttribute("breadcrumbJsonLd",
                    eventoPublicStructuredDataBuilder.buildBreadcrumbMunicipioJsonLd(baseUrl, municipioTrim, nombreProvinciaCanonico));
            }
        }

        model.addAttribute("provincias", eventoPublicoCatalogoFacade.obtenerProvinciasPublicasOrdenadas());
        // Pre-cargar municipios de la provincia de este municipio
        model.addAttribute("municipiosProvincia",
            nombreProvinciaConsultaCanonico.isBlank() ? List.of() : eventoPublicoCatalogoFacade.obtenerMunicipiosPublicosPorProvincia(nombreProvinciaConsultaCanonico));
        model.addAttribute("artistasDisponibles", eventoPublicoCatalogoFacade.obtenerArtistasOrdenados(eventos));
        model.addAttribute("quickLinks", eventoPublicoCatalogoFacade.obtenerQuickLinksPublicos(nombreProvinciaCanonico, municipioTrim));
        model.addAttribute("fechaDesde", fechaDesde.toString());
        model.addAttribute("fechaHasta", fechaHasta.toString());
        model.addAttribute("fechaMaxFiltro", fechaHasta.toString());
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

        EventoPublicoDateWindow.DateRange dateRange = eventoPublicoDateWindow.effectiveUpcomingWindow(fechaDesde, fechaHasta);
        List<EventoPublicoDto> eventos = eventoPublicoService.obtenerEventosPublicosPorMunicipio(
            municipio.trim(), dateRange.fechaDesde(), dateRange.fechaHasta());
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
        String provinciaConsulta = eventoPublicoCatalogoFacade.normalizarProvinciaParaConsulta(provinciaTrim);
        log.info("API: Obteniendo municipios para provincia: {}", provinciaTrim);

        // Verify province exists (404 if not found)
        Optional<Provincia> provinciaOpt = localizacionService.findProvinciaByNombreUpperCase(provinciaConsulta);
        if (provinciaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Provincia no encontrada"));
        }

        // Get canonical province name from DB
        String nombreCanonico = provinciaOpt.get().getNombre();

        // Get municipalities
        List<CodigoNombreRecord> municipios = localizacionService.findMunicipiosByProvinciaNombre(provinciaConsulta);

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

    private Page<EventoPublicoDto> filtrarPaginaPorRango(Page<EventoPublicoDto> paginaEventos, LocalDate fechaDesde, LocalDate fechaHasta) {
        List<EventoPublicoDto> eventosFiltrados = paginaEventos.getContent().stream()
            .filter(evento -> {
                LocalDate fechaEvento = evento.getFecha().toLocalDate();
                return !fechaEvento.isBefore(fechaDesde) && !fechaEvento.isAfter(fechaHasta);
            })
            .toList();

        return new MetadataPreservingPage<>(eventosFiltrados, paginaEventos.getPageable(), paginaEventos.getTotalElements());
    }

    private ProvinciaSeoMetadata.SeoCopy obtenerSeoProvinciaCopy(String provincia, String year) {
        return ProvinciaSeoMetadata.seoCopyPara(provincia, year);
    }
}
