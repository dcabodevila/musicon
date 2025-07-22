package es.musicalia.gestmusica.orquestasdegalicia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Transactional(readOnly = true)
public class OrquestasDeGaliciaServiceImpl implements OrquestasDeGaliciaService {

    private final RestTemplate restTemplate;


    public OrquestasDeGaliciaServiceImpl(@Qualifier("orquestasRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${orquestas.galicia.api.url}")
    private String apiUrl;

    @Value("${orquestas.galicia.api.token}")
    private String apiToken;

    @Transactional(readOnly = true)
    @Override
    public ActuacionExterna obtenerActuacion(Integer idActuacionExterno) {
        HttpHeaders headers = crearHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = apiUrl + "/v1/actuaciones-externas/" + idActuacionExterno;
        
        ResponseEntity<ActuacionExterna> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            ActuacionExterna.class
        );

        return response.getBody();
    }

    @Transactional
    @Override
    public void crearActuacion(ActuacionExterna actuacion) {
        HttpHeaders headers = crearHeaders();
        HttpEntity<ActuacionExterna> entity = new HttpEntity<>(actuacion, headers);

        String url = apiUrl + "/v1/actuaciones-externas";

        restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            Void.class
        );
    }

    @Transactional
    @Override
    public void modificarActuacion(Integer idActuacionExterno, ActuacionExterna actuacion) {
        HttpHeaders headers = crearHeaders();
        HttpEntity<ActuacionExterna> entity = new HttpEntity<>(actuacion, headers);

        String url = apiUrl + "/v1/actuaciones-externas/" + idActuacionExterno;

        restTemplate.exchange(
            url,
            HttpMethod.PUT,
            entity,
            Void.class
        );
    }

    @Transactional
    @Override
    public void eliminarActuacion(Integer idActuacionExterno) {
        HttpHeaders headers = crearHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = apiUrl + "/v1/actuaciones-externas/" + idActuacionExterno;

        restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            entity,
            Void.class
        );
    }

    private HttpHeaders crearHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken);
        return headers;
    }
}