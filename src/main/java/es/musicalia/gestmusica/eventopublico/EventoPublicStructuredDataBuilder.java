package es.musicalia.gestmusica.eventopublico;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface EventoPublicStructuredDataBuilder {

    String buildEventJsonLd(EventoPublicoDto evento, String canonicalUrl, String imageUrl);

    String buildItemListJsonLd(List<EventoPublicoDto> eventos, String baseUrl, String listName, String listUrl);

    String buildArtistaJsonLd(List<EventoPublicoDto> eventos, String baseUrl, String artistaUrl,
                              String listName, String listDescription, String imageUrl);

    String buildBreadcrumbEventoJsonLd(String baseUrl, EventoPublicoDto evento);

    String buildBreadcrumbProvinciaJsonLd(String baseUrl, String provincia);

    String buildBreadcrumbMunicipioJsonLd(String baseUrl, String municipio, String provincia);

    String buildBreadcrumbHoyJsonLd(String baseUrl);
}

@Component
class DefaultEventoPublicStructuredDataBuilder implements EventoPublicStructuredDataBuilder {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ZoneId EVENT_TIME_ZONE = ZoneId.of("Europe/Madrid");
    private static final Locale SPANISH_LOCALE = new Locale("es", "ES");
    private static final String EVENT_IMAGE_URL =
        "https://res.cloudinary.com/hseoceuyz/image/upload/v1760835633/landing-festia_epbr7a.png";
    private static final String ORGANIZER_NAME_FALLBACK = "festia.es";

    @Override
    public String buildEventJsonLd(EventoPublicoDto evento, String canonicalUrl, String imageUrl) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@type", "Event");
        root.put("@id", canonicalUrl);
        root.put("url", canonicalUrl);
        root.put("name", buildEventName(evento));
        root.put("startDate", buildStartDate(evento));
        root.put("location", buildLocation(evento));
        root.put("description", buildDescription(evento));
        root.put("performer", Map.of("@type", "MusicGroup", "name", evento.getNombreArtista()));
        root.put("eventStatus", "https://schema.org/EventScheduled");
        root.put("eventAttendanceMode", "https://schema.org/OfflineEventAttendanceMode");

        if (imageUrl != null && !imageUrl.isBlank()) {
            root.put("image", EventoPublicoDto.normalizeImageUrl(imageUrl));
        }

        return serialize(root, "Event", String.valueOf(evento.getId()));
    }

    @Override
    public String buildItemListJsonLd(List<EventoPublicoDto> eventos, String baseUrl, String listName, String listUrl) {
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
        return serialize(root, "ItemList", listUrl);
    }

    @Override
    public String buildArtistaJsonLd(List<EventoPublicoDto> eventos, String baseUrl, String artistaUrl,
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

        String itemListJson = buildItemListJsonLd(eventos.subList(0, Math.min(eventos.size(), 50)), baseUrl, listName, artistaUrl);

        try {
            return serialize(List.of(musicGroup, OBJECT_MAPPER.readValue(itemListJson, Map.class)), "Artist", artistaUrl);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar JSON-LD Artist para " + artistaUrl, ex);
        }
    }

    @Override
    public String buildBreadcrumbEventoJsonLd(String baseUrl, EventoPublicoDto evento) {
        List<Map<String, Object>> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(breadcrumbItem(1, "Festia", baseUrl));
        breadcrumbs.add(breadcrumbItem(2, "Eventos", baseUrl + "/eventos"));
        breadcrumbs.add(breadcrumbItem(3, evento.getNombreArtista(), baseUrl + "/eventos/artista/" + evento.getIdArtista()));

        Map<String, Object> currentItem = new LinkedHashMap<>();
        currentItem.put("@type", "ListItem");
        currentItem.put("position", 4);
        currentItem.put("name", evento.getTituloEvento());
        breadcrumbs.add(currentItem);

        return serializeBreadcrumbs(breadcrumbs);
    }

    @Override
    public String buildBreadcrumbProvinciaJsonLd(String baseUrl, String provincia) {
        return serializeBreadcrumbs(List.of(
            breadcrumbItem(1, "Festia", baseUrl),
            breadcrumbItem(2, "Eventos", baseUrl + "/eventos"),
            breadcrumbItem(3, provincia, baseUrl + "/eventos/provincia/" + UriUtils.encodePath(provincia, StandardCharsets.UTF_8))
        ));
    }

    @Override
    public String buildBreadcrumbMunicipioJsonLd(String baseUrl, String municipio, String provincia) {
        List<Map<String, Object>> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(breadcrumbItem(1, "Festia", baseUrl));
        breadcrumbs.add(breadcrumbItem(2, "Eventos", baseUrl + "/eventos"));
        if (provincia != null && !provincia.isBlank()) {
            breadcrumbs.add(breadcrumbItem(3, provincia, baseUrl + "/eventos/provincia/" + UriUtils.encodePath(provincia, StandardCharsets.UTF_8)));
        }
        breadcrumbs.add(breadcrumbItem(provincia == null || provincia.isBlank() ? 3 : 4,
            municipio,
            baseUrl + "/eventos/municipio/" + UriUtils.encodePath(municipio, StandardCharsets.UTF_8)));
        return serializeBreadcrumbs(breadcrumbs);
    }

    @Override
    public String buildBreadcrumbHoyJsonLd(String baseUrl) {
        return serializeBreadcrumbs(List.of(
            breadcrumbItem(1, "Festia", baseUrl),
            breadcrumbItem(2, "Eventos", baseUrl + "/eventos"),
            breadcrumbItem(3, "Eventos de hoy", baseUrl + "/eventos/hoy")
        ));
    }

    private String buildEventName(EventoPublicoDto evento) {
        String fecha = evento.getFecha().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", SPANISH_LOCALE));
        return evento.getNombreArtista() + " en " + evento.getMunicipio() + " - " + fecha;
    }

    private String buildStartDate(EventoPublicoDto evento) {
        LocalDate fecha = evento.getFecha().toLocalDate();
        if (evento.getHoraActuacion() == null) {
            return fecha.toString();
        }
        LocalDateTime startDateTime = fecha.atTime(evento.getHoraActuacion());
        return startDateTime.atZone(EVENT_TIME_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private Map<String, Object> buildLocation(EventoPublicoDto evento) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("@type", "PostalAddress");
        address.put("addressLocality", evento.getMunicipio());
        address.put("addressRegion", evento.getProvincia());
        address.put("addressCountry", "ES");

        Map<String, Object> location = new LinkedHashMap<>();
        location.put("@type", "Place");
        location.put("name", resolveLocationName(evento));
        location.put("address", address);
        return location;
    }

    private String resolveLocationName(EventoPublicoDto evento) {
        if (evento.getLugarDisplay() != null && !evento.getLugarDisplay().isBlank()) {
            return evento.getLugarDisplay();
        }
        if (evento.getLugarParaMapa() != null && !evento.getLugarParaMapa().isBlank()) {
            return evento.getLugarParaMapa();
        }
        if (evento.getMunicipio() != null && !evento.getMunicipio().isBlank()) {
            return evento.getMunicipio();
        }
        return evento.getProvincia();
    }

    private String buildDescription(EventoPublicoDto evento) {
        if (evento.getInformacionAdicional() != null && !evento.getInformacionAdicional().isBlank()) {
            return evento.getInformacionAdicional();
        }
        return evento.getDescripcionSeo();
    }

    private Map<String, Object> breadcrumbItem(int position, String name, String item) {
        Map<String, Object> breadcrumb = new LinkedHashMap<>();
        breadcrumb.put("@type", "ListItem");
        breadcrumb.put("position", position);
        breadcrumb.put("name", name);
        breadcrumb.put("item", item);
        return breadcrumb;
    }

    private String serializeBreadcrumbs(List<Map<String, Object>> breadcrumbs) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@type", "BreadcrumbList");
        root.put("itemListElement", breadcrumbs);
        return serialize(root, "BreadcrumbList", "breadcrumbs");
    }

    private String serialize(Object data, String type, String reference) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data).replace("</", "<\\/");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar JSON-LD " + type + " para " + reference, ex);
        }
    }
}
