package es.musicalia.gestmusica.eventopublico;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

    @Test
    void buildItemListJsonLd_debeGenerarItemListConEventosOrdenadosYUrlsCanonicas() throws Exception {
        EventoPublicoDto primero = baseEvento();
        EventoPublicoDto segundo = EventoPublicoDto.builder()
            .id(11L)
            .idArtista(21L)
            .nombreArtista("Orquesta Marbella")
            .nombreAgencia("Agencia Norte")
            .municipio("Sarria")
            .provincia("Lugo")
            .fecha(LocalDateTime.of(2026, 8, 16, 0, 0))
            .build();

        String json = builder.buildItemListJsonLd(
            List.of(primero, segundo),
            "https://festia.es",
            "Fiestas en Lugo",
            "https://festia.es/eventos/provincia/Lugo"
        );

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("@type").asText()).isEqualTo("ItemList");
        assertThat(root.path("numberOfItems").asInt()).isEqualTo(2);
        assertThat(root.path("itemListElement").get(0).path("position").asInt()).isEqualTo(1);
        assertThat(root.path("itemListElement").get(0).path("item").path("url").asText())
            .isEqualTo("https://festia.es/eventos/evento/10-los-satelites-lugo-2026-08-15");
        assertThat(root.path("itemListElement").get(1).path("item").path("organizer").path("name").asText())
            .isEqualTo("Agencia Norte");
    }

    @Test
    void buildArtistaJsonLd_debeCombinarMusicGroupEItemList() throws Exception {
        EventoPublicoDto primero = baseEvento();
        EventoPublicoDto segundo = EventoPublicoDto.builder()
            .id(11L)
            .idArtista(20L)
            .nombreArtista("Los Satélites")
            .municipio("Sarria")
            .provincia("Lugo")
            .fecha(LocalDateTime.of(2026, 8, 16, 0, 0))
            .build();

        String json = builder.buildArtistaJsonLd(
            List.of(primero, segundo),
            "https://festia.es",
            "https://festia.es/eventos/artista/20",
            "Conciertos y Fechas de Los Satélites | Festia",
            "Próximas actuaciones públicas del artista",
            "https://cdn.festia.es/logo.png"
        );

        JsonNode root = objectMapper.readTree(json);
        assertThat(root).hasSize(2);
        assertThat(root.get(0).path("@type").asText()).isEqualTo("MusicGroup");
        assertThat(root.get(0).path("name").asText()).isEqualTo("Los Satélites");
        assertThat(root.get(1).path("@type").asText()).isEqualTo("ItemList");
        assertThat(root.get(1).path("numberOfItems").asInt()).isEqualTo(2);
    }

    @Test
    void breadcrumbs_debenMantenerJerarquiaPublicaParaEventoProvinciaMunicipioYHoy() throws Exception {
        EventoPublicoDto evento = baseEvento();

        JsonNode breadcrumbEvento = objectMapper.readTree(builder.buildBreadcrumbEventoJsonLd("https://festia.es", evento));
        JsonNode breadcrumbProvincia = objectMapper.readTree(builder.buildBreadcrumbProvinciaJsonLd("https://festia.es", "Coruña"));
        JsonNode breadcrumbMunicipio = objectMapper.readTree(builder.buildBreadcrumbMunicipioJsonLd("https://festia.es", "Santiago de Compostela", "Coruña"));
        JsonNode breadcrumbHoy = objectMapper.readTree(builder.buildBreadcrumbHoyJsonLd("https://festia.es"));

        assertThat(breadcrumbEvento.path("itemListElement")).hasSize(4);
        assertThat(breadcrumbEvento.path("itemListElement").get(2).path("item").asText())
            .isEqualTo("https://festia.es/eventos/artista/20");
        assertThat(breadcrumbProvincia.path("itemListElement").get(2).path("item").asText())
            .isEqualTo("https://festia.es/eventos/provincia/Coru%C3%B1a");
        assertThat(breadcrumbMunicipio.path("itemListElement").get(3).path("item").asText())
            .isEqualTo("https://festia.es/eventos/municipio/Santiago%20de%20Compostela");
        assertThat(breadcrumbHoy.path("itemListElement").get(2).path("item").asText())
            .isEqualTo("https://festia.es/eventos/hoy");
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
