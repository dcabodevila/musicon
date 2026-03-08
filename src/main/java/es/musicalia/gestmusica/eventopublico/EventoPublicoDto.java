package es.musicalia.gestmusica.eventopublico;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
public class EventoPublicoDto {
    private Long id;
    private Long idArtista;
    private String nombreArtista;
    private LocalDateTime fecha;
    private LocalTime horaActuacion;
    private String lugar;
    private String municipio;
    private String provincia;
    private boolean matinal;
    private boolean tarde;
    private boolean noche;
    private String informacionAdicional;

    /**
     * Genera el JSON-LD de Schema.org para MusicEvent
     * Compatible con Google Events y otros buscadores
     */
    public String toJsonLd(String baseUrl) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        String eventoUrl = baseUrl + "/eventos/evento/" + id;

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"@context\": \"https://schema.org\",\n");
        json.append("  \"@type\": \"MusicEvent\",\n");
        json.append("  \"@id\": \"").append(eventoUrl).append("\",\n");
        json.append("  \"name\": \"Actuación de ").append(escapeJson(nombreArtista)).append("\",\n");
        json.append("  \"startDate\": \"").append(fecha.format(formatter)).append("\",\n");
        json.append("  \"location\": {\n");
        json.append("    \"@type\": \"Place\",\n");
        json.append("    \"name\": \"").append(escapeJson(lugar)).append("\",\n");
        json.append("    \"address\": {\n");
        json.append("      \"@type\": \"PostalAddress\",\n");
        json.append("      \"addressLocality\": \"").append(escapeJson(municipio)).append("\",\n");
        json.append("      \"addressRegion\": \"").append(escapeJson(provincia)).append("\",\n");
        json.append("      \"addressCountry\": \"ES\"\n");
        json.append("    }\n");
        json.append("  },\n");
        json.append("  \"performer\": {\n");
        json.append("    \"@type\": \"MusicGroup\",\n");
        json.append("    \"name\": \"").append(escapeJson(nombreArtista)).append("\"\n");
        json.append("  },\n");
        json.append("  \"eventStatus\": \"https://schema.org/EventScheduled\",\n");
        json.append("  \"eventAttendanceMode\": \"https://schema.org/OfflineEventAttendanceMode\"");

        if (informacionAdicional != null && !informacionAdicional.isBlank()) {
            json.append(",\n  \"description\": \"").append(escapeJson(informacionAdicional)).append("\"");
        }

        json.append("\n}");
        return json.toString();
    }

    /**
     * Escapa caracteres especiales para JSON
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Genera el título SEO para la página del evento
     */
    public String getTituloSeo() {
        return nombreArtista + " en " + municipio + " - " +
               fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Genera la descripción SEO para la página del evento
     */
    public String getDescripcionSeo() {
        StringBuilder desc = new StringBuilder();
        desc.append("Actuación de ").append(nombreArtista);
        desc.append(" el ").append(fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        if (lugar != null && !lugar.isBlank()) {
            desc.append(" en ").append(lugar);
        }
        desc.append(", ").append(municipio).append(" (").append(provincia).append(")");
        return desc.toString();
    }
}
