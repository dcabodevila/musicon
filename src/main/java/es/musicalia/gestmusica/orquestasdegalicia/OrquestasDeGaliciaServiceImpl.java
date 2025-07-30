package es.musicalia.gestmusica.orquestasdegalicia;

import es.musicalia.gestmusica.ocupacion.OcupacionEstadoEnum;
import es.musicalia.gestmusica.ocupacion.OrquestasDeGaliciaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
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

    @Value("${orquestas.galicia.api.enabled}")
    private boolean apiEnabled;

    @Transactional(readOnly = true)
    @Override
    public ActuacionExterna obtenerActuacion(Integer idActuacionExterno) {
        if (!apiEnabled) {
            log.warn("La API de Orquestas de Galicia está deshabilitada. No se realizó la llamada para obtener la actuación con ID: {}", idActuacionExterno);
            return null;
        }

        // Construir la URL del endpoint
        String url = apiUrl + "/v1/actuaciones-externas/" + idActuacionExterno;
        
        try {
            // Crear headers e incluir el token de autenticación
            HttpHeaders headers = crearHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            // Realizar la solicitud GET
            ResponseEntity<ActuacionExterna> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ActuacionExterna.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                return null;
            }

            // Registrar y devolver la respuesta
            ActuacionExterna actuacionExterna = response.getBody();
            log.info("Actuación obtenida correctamente: {}", actuacionExterna);
            return actuacionExterna;
        } catch (RestClientResponseException e) {
            // Actualización: usar getStatusCode().value() en lugar de getRawStatusCode()
            log.error("Error al obtener actuación con ID {}: [{}] {}", idActuacionExterno, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            // Manejar cualquier otro error desconocido
            log.error("Error inesperado al obtener actuación con ID {}: {}", idActuacionExterno, e.getMessage(), e);
        }

        return null; // Devuelve null si ocurre algún error
    }

    @Transactional
    @Override
    public ResponseEntity<String> crearActuacion(ActuacionExterna actuacion) {
        if (!apiEnabled) {
            log.warn("La API de Orquestas de Galicia está deshabilitada. No se realizó la llamada para crear la actuación.");
            return ResponseEntity.status(HttpStatus.OK)
                    .body("API deshabilitada");
        }

        HttpHeaders headers = crearHeaders();
        HttpEntity<ActuacionExterna> entity = new HttpEntity<>(actuacion, headers);

        String url = apiUrl + "/v1/actuaciones-externas";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
            log.info("Actuación creada correctamente: {}", response.getBody());
            return response;
        } catch (RestClientResponseException e) {
            log.error("Error al crear actuación: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode().value())
                    .body(e.getResponseBodyAsString());
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> modificarActuacion(Integer idActuacionExterno, ActuacionExterna actuacion) {
        if (!apiEnabled) {
            log.warn("La API de Orquestas de Galicia está deshabilitada. No se realizó la llamada para modificar la actuación.");
            return ResponseEntity.status(HttpStatus.OK)
                    .body("API deshabilitada");
        }

        HttpHeaders headers = crearHeaders();
        HttpEntity<ActuacionExterna> entity = new HttpEntity<>(actuacion, headers);

        String url = apiUrl + "/v1/actuaciones-externas/" + idActuacionExterno;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                String.class
            );
            log.info("Actuación modificada correctamente. ID: {}", idActuacionExterno);
            return ResponseEntity.noContent().build();
        } catch (RestClientResponseException e) {
            log.error("Error al modificar actuación: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode().value())
                    .body(e.getResponseBodyAsString());
        }
    }

    @Transactional
    @Override
    public ResponseEntity<String> eliminarActuacion(Integer idActuacionExterno) {
        if (!apiEnabled) {
            log.warn("La API de Orquestas de Galicia está deshabilitada. No se realizó la llamada para eliminar la actuación.");
            return ResponseEntity.status(HttpStatus.OK)
                    .body("API deshabilitada");
        }

        HttpHeaders headers = crearHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = apiUrl + "/v1/actuaciones-externas/" + idActuacionExterno;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                String.class
            );
            log.info("Actuación eliminada correctamente. ID: {}", idActuacionExterno);
            return ResponseEntity.noContent().build();
        } catch (RestClientResponseException e) {
            log.error("Error al eliminar actuación: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode().value())
                    .body(e.getResponseBodyAsString());
        }
    }

    @Transactional
    @Override
    public void enviarActuacionOrquestasDeGalicia(boolean isCreacion, ActuacionExterna actuacionExterna, String nombreEstadoOcupacion) throws OrquestasDeGaliciaException {
        if (actuacionExterna == null || nombreEstadoOcupacion == null) {
            throw new IllegalArgumentException("Parámetros obligatorios no pueden ser nulos");
        }

        ResponseEntity<String> response = null;
        try {
            if (OcupacionEstadoEnum.OCUPADO.getDescripcion().equals(nombreEstadoOcupacion)) {
                response = (isCreacion)
                        ? crearActuacion(actuacionExterna)
                        : procesarModificacion(actuacionExterna);
            } else {
                response = (isCreacion)
                        ? ResponseEntity.ok("No es necesario actualizar")
                        : eliminarActuacion(actuacionExterna.getIdActuacionExterno());
            }

            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                log.info("Actuación enviada correctamente: {}", actuacionExterna.getIdActuacionExterno());
            } else {
                throw new OrquestasDeGaliciaException("Error en la operación. Respuesta: " + response != null ? response.getStatusCode().toString() : "null");
            }
        } catch (Exception e) {
            throw new OrquestasDeGaliciaException("Fallo inesperado al procesar la actuación: " + e.getMessage());
        }
    }

    private ResponseEntity<String> procesarModificacion(ActuacionExterna actuacionExterna) {
        ActuacionExterna actuacionExternaResponse =
                obtenerActuacion(actuacionExterna.getIdActuacionExterno());
        if (actuacionExternaResponse != null) {
            return modificarActuacion(actuacionExterna.getIdActuacionExterno(), actuacionExterna);
        } else {
            return crearActuacion(actuacionExterna);
        }
    }


    private HttpHeaders crearHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken);
        return headers;
    }

}