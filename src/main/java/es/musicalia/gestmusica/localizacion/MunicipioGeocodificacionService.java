package es.musicalia.gestmusica.localizacion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class MunicipioGeocodificacionService {

    private static final Logger log = LoggerFactory.getLogger(MunicipioGeocodificacionService.class);
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    private final RestTemplate restTemplate;
    private final MunicipioRepository municipioRepository;

    public MunicipioGeocodificacionService(@Qualifier("nominatimRestTemplate") RestTemplate restTemplate,
                                            MunicipioRepository municipioRepository) {
        this.restTemplate = restTemplate;
        this.municipioRepository = municipioRepository;
    }

    @Transactional
    public GeocodificacionResultado geocodificarPendientes(int limite) {
        List<Municipio> pendientes = municipioRepository.findByLatitudIsNullAndLongitudIsNull(PageRequest.of(0, limite));
        return geocodificarLista(pendientes);
    }

    @Transactional
    public GeocodificacionResultado geocodificarPendientesPorProvincia(Long idProvincia, int limite) {
        List<Municipio> pendientes = municipioRepository.findByProvinciaIdAndLatitudIsNullAndLongitudIsNull(idProvincia, PageRequest.of(0, limite));
        return geocodificarLista(pendientes);
    }

    private GeocodificacionResultado geocodificarLista(List<Municipio> pendientes) {
        int total = pendientes.size();
        int exitosos = 0;
        int fallidos = 0;

        for (Municipio municipio : pendientes) {
            try {
                boolean ok = geocodificarMunicipio(municipio);
                if (ok) {
                    exitosos++;
                } else {
                    fallidos++;
                }
                // Nominatim pide max 1 req/segundo para uso gratuito
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Geocodificación interrumpida");
                break;
            } catch (Exception e) {
                log.error("Error geocodificando municipio {}: {}", municipio.getNombre(), e.getMessage());
                fallidos++;
            }
        }

        return new GeocodificacionResultado(total, exitosos, fallidos);
    }

    private boolean geocodificarMunicipio(Municipio municipio) {
        String provincia = municipio.getProvincia() != null ? municipio.getProvincia().getNombre() : "";
        String query = String.format("%s, %s, España", municipio.getNombre(), provincia);

        URI uri = UriComponentsBuilder.fromUriString(NOMINATIM_URL)
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        log.info("Geocodificando: {}", query);

        try {
            NominatimResponse[] respuesta = restTemplate.getForObject(uri, NominatimResponse[].class);
            if (respuesta != null && respuesta.length > 0) {
                NominatimResponse resultado = respuesta[0];
                municipio.setLatitud(new BigDecimal(resultado.lat));
                municipio.setLongitud(new BigDecimal(resultado.lon));
                municipioRepository.save(municipio);
                log.info("OK -> lat={}, lon={} para {}", resultado.lat, resultado.lon, query);
                return true;
            } else {
                log.warn("Sin resultados para: {}", query);
            }
        } catch (Exception e) {
            log.error("Fallo en petición Nominatim para {}: {}", query, e.getMessage());
        }
        return false;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NominatimResponse {
        public String lat;
        public String lon;
    }

    public record GeocodificacionResultado(int total, int exitosos, int fallidos) {}
}
