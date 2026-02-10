package es.musicalia.gestmusica.orquestasdegalicia;

import es.musicalia.gestmusica.ocupacion.OrquestasDeGaliciaException;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class OrquestasDeGaliciaServiceImpl implements OrquestasDeGaliciaService {

    private final RestTemplate restTemplate;

    public OrquestasDeGaliciaServiceImpl(@Qualifier("orquestasRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    @Value("${orquestas.galicia.auth.url:https://oauth.orquestasdegalicia.es}")
    private String authUrl;

    @Value("${orquestas.galicia.api.url:https://ext-api.orquestasdegalicia.es/v1/actuaciones-externas}")
    private String apiUrl;

    @Value("${orquestas.galicia.api.enabled}")
    private boolean apiEnabled;

    @Value("${orquestas.api.username}")
    private String username;

    @Value("${orquestas.api.password}")
    private String password;

    private String cachedToken;

    private String getToken() {
        if (isTokenValido()) {
            return cachedToken;
        }

        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        try {
            AuthResponse response = restTemplate.postForObject(authUrl + "/token", body, AuthResponse.class);
            if (response != null) {
                cachedToken = response.getAccessToken();
                return cachedToken;
            }
        } catch (RestClientResponseException e) {
            log.error("Error de autenticación con Orquestas de Galicia: [{}] {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Fallo al autenticar: " + e.getMessage());
        }

        throw new RuntimeException("No se pudo obtener el token de Orquestas de Galicia");
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private boolean isTokenValido() {
        if (cachedToken == null) return false;

        try {
            // Un JWT tiene 3 partes: Header.Payload.Signature
            String[] parts = cachedToken.split("\\.");
            if (parts.length < 2) return false;

            // El payload es la segunda parte
            String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            com.fasterxml.jackson.databind.JsonNode payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson);

            if (payload.has("exp")) {
                long expTimestamp = payload.get("exp").asLong();
                long ahora = java.time.Instant.now().getEpochSecond();

                // Consideramos el token inválido si falta menos de 1 minuto para que caduque
                return expTimestamp > (ahora + 60);
            }
        } catch (Exception e) {
            log.error("Error al verificar caducidad del token JWT", e);
        }
        return false;
    }

    @Transactional
    @Override
    public Optional<ActuacionExterna> obtenerActuacion(Integer idActuacionExterno) throws OrquestasDeGaliciaException {
        if (!apiEnabled) {
            log.warn("La API de Orquestas de Galicia está deshabilitada. No se realizó la llamada para obtener la actuación con ID: {}", idActuacionExterno);
            return Optional.empty();
        }

        // Construir la URL del endpoint
        String url = apiUrl + "/"  + idActuacionExterno;
        
        try {
            // Crear headers e incluir el token de autenticación
            HttpHeaders headers = getHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            // Realizar la solicitud GET
            ResponseEntity<ActuacionExterna> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ActuacionExterna.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new OrquestasDeGaliciaException("Error en la llamada a Orquestas de Galicia: " + response.getStatusCode());
            }

            // Registrar y devolver la respuesta
            ActuacionExterna actuacionExterna = response.getBody();
            log.info("Actuación obtenida correctamente: {}", actuacionExterna);
            return Optional.ofNullable(actuacionExterna);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("Actuación no encontrada con idActuacionExterno: {}", idActuacionExterno);
                return Optional.empty();
            }
            log.error("Error al obtener actuación {}: [{}]", idActuacionExterno, e.getStatusCode());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                cachedToken = null;
                log.error("Consulta no autorizada para idActuacionExterno: {}", idActuacionExterno);
                return Optional.empty();
            }
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al obtener actuación con ID {}: {}", idActuacionExterno, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @Override
    public DefaultResponseBody crearActuacion(ActuacionExterna actuacion) {
        if (!apiEnabled) {
            log.warn("La API de Orquestas de Galicia está deshabilitada. No se realizó la llamada para crear la actuación.");
            return DefaultResponseBody.builder().success(false).messageType("danger").message("API deshabilitada").build();
        }

        HttpHeaders headers = getHeaders();
        HttpEntity<ActuacionExterna> entity = new HttpEntity<>(actuacion, headers);


        try {
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl ,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()){
                log.info("Actuación creada correctamente: {}", response.getBody());
                return DefaultResponseBody.builder().success(true).messageType("success").message("Actuación creada correctamente en OrquestasDeGalicia").build();
            }
            else {
                log.error("No se ha podido crear la actuación: {}, {}", actuacion.getIdActuacionExterno(), response.getStatusCode());
                return DefaultResponseBody.builder().success(false).messageType("error").message("No se ha podido crear la actuación en OrquestasDeGalicia").build();
            }


        } catch (RestClientResponseException e) {
            log.error("Error al crear actuación: {}", e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) { cachedToken = null; }
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                return DefaultResponseBody.builder().success(false).messageType("error").message("La actuación ya ha sido creada previamente en OrquestasDeGalicia").build();
            }

            return DefaultResponseBody.builder().success(false).messageType("error").message("No se ha podido crear la actuación en OrquestasDeGalicia. Comprueba si ya existía anteriormente.").build();

        }
    }

    @Transactional
    @Override
    public DefaultResponseBody modificarActuacion(Integer idActuacionExterno, ActuacionExterna actuacion) {
        if (!apiEnabled) {
            log.warn("La API de Orquestas de Galicia está deshabilitada. No se realizó la llamada para crear la actuación.");
            return DefaultResponseBody.builder().success(false).messageType("danger").message("API deshabilitada").build();
        }

        HttpHeaders headers = getHeaders();
        HttpEntity<ActuacionExterna> entity = new HttpEntity<>(actuacion, headers);

        String url = apiUrl + "/"  + idActuacionExterno;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                String.class
            );
            if (response.getStatusCode().is2xxSuccessful()){
                log.info("Actuación actualizada correctamente: {}", response.getBody());
                return DefaultResponseBody.builder().success(true).messageType("success").message("Actuación actualizada correctamente en OrquestasDeGalicia").build();
            }
            else {
                log.error("No se ha podido crear la actuación: {}, {}", actuacion.getIdActuacionExterno(), response.getStatusCode());
                return DefaultResponseBody.builder().success(false).messageType("error").message("No se ha podido actualizar la actuación en OrquestasDeGalicia").build();
            }
        } catch (RestClientResponseException e) {
            log.error("Error al modificar actuación: {}", e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) { cachedToken = null; }
            return DefaultResponseBody.builder().success(false).messageType("error").message("Excepción al actualizar la actuación en OrquestasDeGalicia").build();

        }
    }

    @Transactional
    @Override
    public DefaultResponseBody eliminarActuacion(Integer idActuacionExterno) {
        if (!apiEnabled) {
            log.warn("La API de Orquestas de Galicia está deshabilitada. No se realizó la llamada para crear la actuación.");
            return DefaultResponseBody.builder().success(false).messageType("danger").message("API deshabilitada").build();
        }

        HttpHeaders headers = getHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = apiUrl + "/"  + idActuacionExterno;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()){
                log.info("Actuación eliminada correctamente: {}", response.getBody());
                return DefaultResponseBody.builder().success(true).messageType("success").message("Actuación eliminada correctamente de OrquestasDeGalicia").build();
            }
            else {
                log.error("No se ha podido eliminar la actuación: {}, {}", idActuacionExterno, response.getStatusCode());
                return DefaultResponseBody.builder().success(false).messageType("error").message("No se ha podido eliminar la actuación de OrquestasDeGalicia").build();
            }


        } catch (RestClientResponseException e) {
            log.error("Error al eliminar actuación: {}", e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) { cachedToken = null; }
            return DefaultResponseBody.builder().success(false).messageType("error").message("Excepción al actualizar la actuación de OrquestasDeGalicia").build();
        }
    }

//    @Transactional
//    @Override
//    public void enviarActuacionOrquestasDeGalicia(boolean isCreacion, ActuacionExterna actuacionExterna, String nombreEstadoOcupacion) throws OrquestasDeGaliciaException {
//        if (actuacionExterna == null || nombreEstadoOcupacion == null) {
//            throw new IllegalArgumentException("Parámetros obligatorios no pueden ser nulos");
//        }
//
//        DefaultResponseBody response = null;
//        try {
//            if (OcupacionEstadoEnum.OCUPADO.getDescripcion().equals(nombreEstadoOcupacion)) {
//                response = (isCreacion)
//                        ? crearActuacion(actuacionExterna)
//                        : procesarModificacion(actuacionExterna);
//            } else {
//                response = (isCreacion)
//                        ? ResponseEntity.ok("No es necesario actualizar")
//                        : eliminarActuacion(actuacionExterna.getIdActuacionExterno());
//            }
//
//            if (response != null && response.getStatusCode().is2xxSuccessful()) {
//                log.info("Actuación enviada correctamente: {}", actuacionExterna.getIdActuacionExterno());
//            } else {
//                String status = (response != null) ? response.getStatusCode().toString() : "SIN_RESPUESTA";
//                throw new OrquestasDeGaliciaException("Error en la operación externa. Status: " + status);
//            }
//        } catch (Exception e) {
//            throw new OrquestasDeGaliciaException("Fallo inesperado al procesar la actuación: " + e.getMessage());
//        }
//    }
//
//
//
//    private ResponseEntity<String> procesarModificacion(ActuacionExterna actuacionExterna) {
//        ActuacionExterna actuacionExternaResponse =
//                obtenerActuacion(actuacionExterna.getIdActuacionExterno());
//        if (actuacionExternaResponse != null) {
//            return modificarActuacion(actuacionExterna.getIdActuacionExterno(), actuacionExterna);
//        } else {
//            return crearActuacion(actuacionExterna);
//        }
//    }



}