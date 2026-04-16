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
import java.util.Arrays;
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
    private LocalTime horaActuacionHasta;
    private String lugar;
    private String municipio;
    private String provincia;
    private boolean matinal;
    private boolean tarde;
    private boolean noche;
    private String informacionAdicional;
    private LocalDateTime fechaActualizacion;

    /**
     * Genera el Map con los datos JSON-LD Schema.org MusicEvent (reutilizable en ItemList).
     */
    public Map<String, Object> toJsonLdMap(String baseUrl, String organizerName, String organizerUrl, String imageUrl) {
        String eventoUrl = baseUrl + getPathPublico();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("@context", "https://schema.org");
        root.put("@type", "MusicEvent");
        root.put("@id", eventoUrl);
        root.put("url", eventoUrl);
        root.put("name", getTituloEvento());
        LocalDate fechaDate = fecha.toLocalDate();
        if (horaActuacion != null) {
            LocalDateTime startDateTime = fechaDate.atTime(horaActuacion);
            LocalDateTime endDateTime;
            if (horaActuacionHasta != null) {
                LocalDate fechaHasta = (horaActuacion.getHour() >= 12 && horaActuacionHasta.getHour() < 12)
                        ? fechaDate.plusDays(1)
                        : fechaDate;
                endDateTime = fechaHasta.atTime(horaActuacionHasta);
            } else {
                endDateTime = startDateTime.plusHours(3);
            }
            root.put("startDate", startDateTime.atZone(EVENT_TIME_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            root.put("endDate", endDateTime.atZone(EVENT_TIME_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } else {
            root.put("startDate", fechaDate.toString());
        }

        Map<String, Object> location = new LinkedHashMap<>();
        location.put("@type", "Place");
        location.put("name", getLugarDisplay() != null ? getLugarDisplay() : lugar);

        Map<String, Object> address = new LinkedHashMap<>();
        address.put("@type", "PostalAddress");
        address.put("addressLocality", getMunicipioDisplay());
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
            root.put("image", normalizeImageUrl(imageUrl));
        }

        return root;
    }

    /**
     * Genera JSON-LD de Schema.org para MusicEvent como String (para incrustar en HTML).
     */
    public String toJsonLd(String baseUrl, String organizerName, String organizerUrl, String imageUrl) {
        try {
            // Evita cierre accidental de <script> en HTML.
            return OBJECT_MAPPER.writeValueAsString(toJsonLdMap(baseUrl, organizerName, organizerUrl, imageUrl))
                .replace("</", "<\\/");
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar JSON-LD del evento " + id, ex);
        }
    }

    /**
     * Titulo SEO para la pagina del evento.
     * Optimizado para no exceder ~60 caracteres y coincidir con queries de intención local.
     * Formato: "Artista — Orquesta en Municipio (Provincia) | Festia"
     */
    public String getTituloSeo() {
        String municipio = getMunicipioDisplay();
        // Formato compacto: "Artista en Municipio (Provincia) | Festia"
        // Ej: "Suavecito en Pontevedra | Festia" (37 chars)
        // Ej: "Costa Dorada en Carballeda de Valdeorras (Ourense) | Festia" (60 chars)
        String base = nombreArtista + " en " + municipio;
        if (base.length() <= 45) {
            return base + " (" + provincia + ") | Festia";
        }
        // Si es muy largo, omitir provincia del title (ya está en la description)
        return base + " | Festia";
    }

    /**
     * Descripcion SEO para la pagina del evento.
     * Optimizada para ~150 caracteres maximo, con keywords de intención y CTA.
     */
    public String getDescripcionSeo() {
        StringBuilder desc = new StringBuilder();
        String fechaFormateada = fecha.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
            new java.util.Locale("es", "ES")));

        desc.append(nombreArtista).append(" actúa en ").append(getMunicipioDisplay());
        desc.append(" (").append(provincia).append(") el ").append(fechaFormateada);

        if (horaActuacion != null) {
            desc.append(" a las ").append(horaActuacion.format(DateTimeFormatter.ofPattern("HH:mm"))).append(" h");
        }
        if (lugar != null && !lugar.isBlank()) {
            desc.append(" en ").append(lugar);
        }
        desc.append(". Toda la información en Festia.");
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
        return "Actuación de " + nombreArtista + " en " + getMunicipioDisplay();
    }

    public String getEncabezadoPrincipal() {
        return nombreArtista + " en " + getMunicipioDisplay() + " - " +
            fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public LocalDate getLastModDate() {
        if (fechaActualizacion != null) {
            return fechaActualizacion.toLocalDate();
        }
        return fecha.toLocalDate();
    }

    public String getMunicipioDisplay() {
        String localidad = getLugarParaMapa();
        return localidad != null ? localidad + ", " + municipio : municipio;
    }

    /**
     * Normaliza una URL de imagen para usar siempre HTTPS.
     * Cloudinary y otros CDNs sirven el mismo contenido por HTTPS.
     */
    public static String normalizeImageUrl(String url) {
        if (url == null || url.isBlank()) return url;
        if (url.startsWith("http://")) return url.replace("http://", "https://");
        return url;
    }

    /**
     * Un evento es indexable para JSON-LD si tiene datos geográficos reales.
     * Eventos con provincia o municipio "Provisional"/"Otras" producen
     * structured data sin sentido para Google y deben excluirse.
     */
    public boolean isIndexableForJsonLd() {
        if (provincia == null || provincia.isBlank()) return false;
        if (municipio == null || municipio.isBlank()) return false;
        String provLower = provincia.toLowerCase(Locale.ROOT);
        String munLower = municipio.toLowerCase(Locale.ROOT);
        if (provLower.contains("provisional") || provLower.equals("otras")) return false;
        if (munLower.contains("provisional")) return false;
        return true;
    }

    public String getLugarDisplay() {
        String localidad = getLugarParaMapa();
        if (localidad == null) return null;
        return Arrays.stream(localidad.split("\\s+"))
            .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.joining(" "));
    }

    /**
     * Extrae el nombre de la localidad del campo lugar para usar en búsquedas de mapa.
     * Si lugar contiene guiones (ej: "ROBRA-OUTEIRO DE REI-LUGO"), divide por guiones/comas,
     * descarta las partes que ya están contenidas en municipio o provincia, y devuelve
     * el primer elemento restante. Devuelve null si lugar es nulo o si todos los elementos
     * son redundantes con municipio/provincia.
     */

    public String getLugarParaMapa() {
        if (lugar == null || lugar.isBlank()) {
            return null;
        }
        String municipioNorm = normalizarTexto(municipio);
        String provinciaNorm = normalizarTexto(provincia);

        return Arrays.stream(lugar.replace('-', ',').split(","))
            .map(String::trim)
            .filter(p -> !p.isBlank())
            .filter(p -> {
                String pNorm = normalizarTexto(p);
                return !municipioNorm.contains(pNorm) && !provinciaNorm.contains(pNorm);
            })
            .findFirst()
            .orElse(null);
    }

    private String normalizarTexto(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(n).replaceAll("").trim();
    }

    private String getDescripcionJsonLd() {
        if (informacionAdicional != null && !informacionAdicional.isBlank()) {
            return informacionAdicional;
        }
        return getDescripcionSeo();
    }
}
