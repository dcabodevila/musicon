package es.musicalia.gestmusica.eventopublico;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public interface EventoPublicStructuredDataBuilder {

    String buildEventJsonLd(EventoPublicoDto evento, String canonicalUrl, String imageUrl);
}

@Component
class DefaultEventoPublicStructuredDataBuilder implements EventoPublicStructuredDataBuilder {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ZoneId EVENT_TIME_ZONE = ZoneId.of("Europe/Madrid");
    private static final Locale SPANISH_LOCALE = new Locale("es", "ES");

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

        // SEO contract: omitir organizer/offers/endDate hasta tener fuente fiable verificable.
        try {
            return OBJECT_MAPPER.writeValueAsString(root).replace("</", "<\\/");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar JSON-LD Event para evento " + evento.getId(), ex);
        }
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
}
