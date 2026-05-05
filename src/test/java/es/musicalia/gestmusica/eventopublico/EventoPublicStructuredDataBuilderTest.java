package es.musicalia.gestmusica.eventopublico;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventoPublicStructuredDataBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventoPublicStructuredDataBuilder builder = new DefaultEventoPublicStructuredDataBuilder();

    @Test
    void buildEventJsonLd_debeGenerarEventConCanonicalYOmitirCamposInventados() throws Exception {
        EventoPublicoDto evento = baseEvento();
        evento.setHoraActuacion(null);

        String canonicalUrl = "https://festia.es/eventos/evento/10-los-saturnos-en-lugo-2026-08-15";
        String json = builder.buildEventJsonLd(evento, canonicalUrl, "http://cdn.festia.es/logo.png");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("@context").asText()).isEqualTo("https://schema.org");
        assertThat(root.path("@type").asText()).isEqualTo("Event");
        assertThat(root.path("@id").asText()).isEqualTo(canonicalUrl);
        assertThat(root.path("url").asText()).isEqualTo(canonicalUrl);
        assertThat(root.path("performer").path("@type").asText()).isEqualTo("MusicGroup");
        assertThat(root.path("startDate").asText()).isEqualTo("2026-08-15");
        assertThat(root.has("organizer")).isFalse();
        assertThat(root.has("offers")).isFalse();
        assertThat(root.has("endDate")).isFalse();
        assertThat(root.path("image").asText()).isEqualTo("https://cdn.festia.es/logo.png");
    }

    @Test
    void buildEventJsonLd_debePriorizarLugarYMapearConHora() throws Exception {
        EventoPublicoDto evento = baseEvento();
        evento.setLugar("SANTA-CRUZ-LUGO");
        evento.setHoraActuacion(LocalTime.of(23, 30));

        String json = builder.buildEventJsonLd(evento,
            "https://festia.es/eventos/evento/10-los-saturnos-en-lugo-2026-08-15",
            "https://cdn.festia.es/logo.png");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("name").asText()).isEqualTo("Los Satélites en Lugo - 15 agosto 2026");
        assertThat(root.path("startDate").asText()).isEqualTo("2026-08-15T23:30:00+02:00");
        assertThat(root.path("location").path("name").asText()).isEqualTo("Santa");
        assertThat(root.path("location").path("address").path("addressCountry").asText()).isEqualTo("ES");
    }

    private EventoPublicoDto baseEvento() {
        return EventoPublicoDto.builder()
            .id(10L)
            .idArtista(20L)
            .nombreArtista("Los Satélites")
            .municipio("Lugo")
            .provincia("Lugo")
            .lugar("Praza Maior")
            .informacionAdicional("Actuación principal de fiestas patronales")
            .fecha(LocalDateTime.of(2026, 8, 15, 0, 0))
            .build();
    }
}
