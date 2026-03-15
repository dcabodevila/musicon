package es.musicalia.gestmusica.eventopublico;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Data
@Builder
public class EventoPublicoDto {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ZoneId EVENT_TIME_ZONE = ZoneId.of("Europe/Madrid");
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^a-z0-9]+");

    private Long id;
    private Long idArtista;
    private String nombreArtista;
    private String nombreAgencia;
    private String urlOrganizador;
    private String logoArtista;
    private LocalDateTime fecha;
    private LocalTime horaActuacion;
    private String lugar;
    private String municipio;
    private String provincia;
    private boolean matinal;
    private boolean tarde;
    private boolean noche;
    private String informacionAdicional;
    private LocalDateTime fechaActualizacion;

    /**
     * Genera JSON-LD de Schema.org para MusicEvent sin concatenaciones manuales.
     */
    public String toJsonLd(String baseUrl, String organizerName, String organizerUrl, String imageUrl) {
        String eventoUrl = baseUrl + getPathPublico();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@type", "MusicEvent");
        root.put("@id", eventoUrl);
        root.put("url", eventoUrl);
        root.put("name", getTituloEvento());
        root.put("startDate", fecha.atZone(EVENT_TIME_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        Map<String, Object> location = new LinkedHashMap<>();
        location.put("@type", "Place");
        location.put("name", lugar);

        Map<String, Object> address = new LinkedHashMap<>();
        address.put("@type", "PostalAddress");
        address.put("addressLocality", municipio);
        address.put("addressRegion", provincia);
        address.put("addressCountry", "ES");
        location.put("address", address);
        root.put("location", location);

        Map<String, Object> performer = new LinkedHashMap<>();
        performer.put("@type", "MusicGroup");
        performer.put("name", nombreArtista);
        root.put("performer", performer);

        Map<String, Object> organizer = new LinkedHashMap<>();
        organizer.put("@type", "Organization");
        organizer.put("name", organizerName);
        if (organizerUrl != null && !organizerUrl.isBlank()) {
            organizer.put("url", organizerUrl);
        }
        root.put("organizer", organizer);

        root.put("eventStatus", "https://schema.org/EventScheduled");
        root.put("eventAttendanceMode", "https://schema.org/OfflineEventAttendanceMode");
        root.put("description", getDescripcionJsonLd());

        if (imageUrl != null && !imageUrl.isBlank()) {
            root.put("image", imageUrl);
        }

        try {
            // Evita cierre accidental de <script> en HTML.
            return OBJECT_MAPPER.writeValueAsString(root).replace("</", "<\\/");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar JSON-LD del evento " + id, ex);
        }
    }

    /**
     * Titulo SEO para la pagina del evento.
     */
    public String getTituloSeo() {
        return nombreArtista + " en " + municipio + " (" + provincia + ") | " +
            fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " | Festia";
    }

    /**
     * Descripcion SEO para la pagina del evento.
     */
    public String getDescripcionSeo() {
        StringBuilder desc = new StringBuilder();
        desc.append("Concierto de ").append(nombreArtista);
        desc.append(" en ").append(municipio).append(" (").append(provincia).append(")");
        desc.append(" el ").append(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        if (horaActuacion != null) {
            desc.append(" a las ").append(horaActuacion.format(DateTimeFormatter.ofPattern("HH:mm"))).append(" h");
        }
        if (lugar != null && !lugar.isBlank()) {
            desc.append(", en ").append(lugar);
        }
        desc.append(". Consulta todos los detalles en Festia.");
        return desc.toString();
    }

    public String getPathPublico() {
        return "/eventos/evento/" + id + "-" + getSlug();
    }

    public String getPathPublicoLegacy() {
        return "/eventos/evento/" + id;
    }

    public String getSlug() {
        String raw = nombreArtista + "-" + municipio + "-" +
            fecha.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String normalized = Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        normalized = NON_ALNUM_PATTERN.matcher(normalized).replaceAll("-");
        normalized = normalized.replaceAll("^-+", "").replaceAll("-+$", "");
        return normalized.isBlank() ? "evento" : normalized;
    }

    public String getTituloEvento() {
        return "Actuación de " + nombreArtista + " en " + municipio;
    }

    public String getEncabezadoPrincipal() {
        return nombreArtista + " en " + municipio + " - " +
            fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public LocalDate getLastModDate() {
        if (fechaActualizacion != null) {
            return fechaActualizacion.toLocalDate();
        }
        return fecha.toLocalDate();
    }

    private String getDescripcionJsonLd() {
        if (informacionAdicional != null && !informacionAdicional.isBlank()) {
            return informacionAdicional;
        }
        return getDescripcionSeo();
    }
}
