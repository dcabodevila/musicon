package es.musicalia.gestmusica.orquestasdegalicia;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.musicalia.gestmusica.config.RestTemplateConfig; // Asegúrate de que el paquete coincida
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(OrquestasDeGaliciaServiceImpl.class)
@Import(RestTemplateConfig.class) // Importamos la configuración de tus beans RestTemplate
@TestPropertySource(properties = {
    "mailgun.api-key=dummy", // Necesario porque RestTemplateConfig lo requiere
    "orquestas.galicia.api.token=dummy",
    "orquestas.galicia.api.enabled=true",
    "orquestas.galicia.auth.url=https://oauth.test",
    "orquestas.galicia.api.url=https://api.test/v1/actuaciones-externas",
    "orquestas.api.username=test-user",
    "orquestas.api.password=test-pass"
})
class OrquestasDeGaliciaServiceTest {

    @Autowired
    private OrquestasDeGaliciaServiceImpl service;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    @Qualifier("orquestasRestTemplate") // Inyectamos el mismo bean que usa el servicio
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final String AUTH_URL = "https://oauth.test/token";
    private final String API_URL = "https://api.test/v1/actuaciones-externas";
    private String validJwtToken;

    private String createValidJwtToken() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        long exp = Instant.now().plusSeconds(3600).getEpochSecond();
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"exp\":" + exp + "}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    @BeforeEach
    void setUp() {
        // Vinculamos manualmente el MockServer al RestTemplate específico
        mockServer = MockRestServiceServer.createServer(restTemplate);
        ReflectionTestUtils.setField(service, "apiEnabled", true);
        ReflectionTestUtils.setField(service, "cachedToken", null);
        validJwtToken = createValidJwtToken();
    }

    private void mockAuthSuccess() throws Exception {
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(validJwtToken);
        
        mockServer.expect(requestTo(AUTH_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"username\":\"test-user\",\"password\":\"test-pass\"}"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(authResponse), MediaType.APPLICATION_JSON));
    }

    @Test
    void testObtenerActuacion_Success() throws Exception {
        // GIVEN
        mockAuthSuccess();
        ActuacionExterna mockActuacion = new ActuacionExterna();
        mockActuacion.setIdActuacionExterno(123);
        mockActuacion.setLugar("Santiago");

        mockServer.expect(requestTo(API_URL + "/123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + validJwtToken))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockActuacion), MediaType.APPLICATION_JSON));

        // WHEN
        Optional<ActuacionExterna> result = service.obtenerActuacion(123);

        // THEN
        assertNotNull(result);
        assertEquals("Santiago", result.get().getLugar());
        mockServer.verify();
    }

    @Test
    void testCrearActuacion_Success() throws Exception {
        // GIVEN
        mockAuthSuccess();
        ActuacionExterna actuacion = new ActuacionExterna();
        actuacion.setIdActuacionExterno(500);
        actuacion.setFecha(LocalDate.now());

        mockServer.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + validJwtToken))
                .andRespond(withStatus(HttpStatus.CREATED).body("Created OK"));

        // WHEN
        var response = service.crearActuacion(actuacion);

        // THEN
        assertTrue(response.isSuccess());
        mockServer.verify();
    }

    @Test
    void testAuthFailure_ThrowsException() throws Exception {
        // GIVEN
        mockServer.expect(requestTo(AUTH_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // WHEN & THEN
        assertThrows(es.musicalia.gestmusica.ocupacion.OrquestasDeGaliciaException.class, () -> service.obtenerActuacion(123));
        mockServer.verify();
    }

    @Test
    void testCrearActuacion_AuthFailure_ReturnsErrorResponse() {
        // GIVEN
        mockServer.expect(requestTo(AUTH_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ActuacionExterna actuacion = new ActuacionExterna();
        actuacion.setIdActuacionExterno(500);

        // WHEN
        var response = service.crearActuacion(actuacion);

        // THEN
        assertFalse(response.isSuccess());
        assertEquals("error", response.getMessageType());
        assertEquals("No se ha podido autenticar con OrquestasDeGalicia", response.getMessage());
        mockServer.verify();
    }

    @Test
    void testModificarActuacion_AuthFailure_ReturnsErrorResponse() {
        // GIVEN
        mockServer.expect(requestTo(AUTH_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ActuacionExterna actuacion = new ActuacionExterna();
        actuacion.setIdActuacionExterno(500);

        // WHEN
        var response = service.modificarActuacion(500, actuacion);

        // THEN
        assertFalse(response.isSuccess());
        assertEquals("error", response.getMessageType());
        assertEquals("No se ha podido autenticar con OrquestasDeGalicia", response.getMessage());
        mockServer.verify();
    }

    @Test
    void testObtenerActuacion_NotFound() throws Exception {
        // GIVEN
        mockAuthSuccess();
        mockServer.expect(requestTo(API_URL + "/999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // WHEN
        Optional<ActuacionExterna> result = service.obtenerActuacion(999);

        // THEN
        assertTrue(result.isEmpty());
        mockServer.verify();
    }

    @Test
    void testTokenCleanupOnUnauthorized() throws Exception {
        // 1) Primera llamada con token cacheado que devuelve 401
        ReflectionTestUtils.setField(service, "cachedToken", validJwtToken);
        mockServer.expect(requestTo(API_URL + "/1"))
                .andExpect(header("Authorization", "Bearer " + validJwtToken))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // 2) Segunda auth + segunda llamada exitosa (solo tras limpiar el token anterior)
        mockAuthSuccess();
        mockServer.expect(requestTo(API_URL + "/1"))
                .andExpect(header("Authorization", "Bearer " + validJwtToken))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // WHEN
        assertThrows(es.musicalia.gestmusica.ocupacion.OrquestasDeGaliciaException.class, () -> service.obtenerActuacion(1));
        Optional<ActuacionExterna> result = service.obtenerActuacion(1);

        // THEN
        assertTrue(result.isPresent());
        mockServer.verify();
    }

    @Test
    void testObtenerActuacion_ApiDeshabilitada_ThrowsException() {
        ReflectionTestUtils.setField(service, "apiEnabled", false);

        assertThrows(es.musicalia.gestmusica.ocupacion.OrquestasDeGaliciaException.class, () -> service.obtenerActuacion(123));
    }

    @Test
    void testCrearActuacion_ApiDeshabilitada_ReturnsDanger() {
        ReflectionTestUtils.setField(service, "apiEnabled", false);

        ActuacionExterna actuacion = new ActuacionExterna();
        actuacion.setIdActuacionExterno(500);

        var response = service.crearActuacion(actuacion);

        assertFalse(response.isSuccess());
        assertEquals("danger", response.getMessageType());
        assertEquals("API deshabilitada", response.getMessage());
    }

    @Test
    void testModificarActuacion_ApiDeshabilitada_ReturnsDanger() {
        ReflectionTestUtils.setField(service, "apiEnabled", false);

        ActuacionExterna actuacion = new ActuacionExterna();
        actuacion.setIdActuacionExterno(500);

        var response = service.modificarActuacion(500, actuacion);

        assertFalse(response.isSuccess());
        assertEquals("danger", response.getMessageType());
        assertEquals("API deshabilitada", response.getMessage());
    }

    @Test
    void testEliminarActuacion_ApiDeshabilitada_ReturnsDanger() {
        ReflectionTestUtils.setField(service, "apiEnabled", false);

        var response = service.eliminarActuacion(500);

        assertFalse(response.isSuccess());
        assertEquals("danger", response.getMessageType());
        assertEquals("API deshabilitada", response.getMessage());
    }

    @Test
    void testObtenerActuacion_BodyNulo_ThrowsException() throws Exception {
        mockAuthSuccess();
        mockServer.expect(requestTo(API_URL + "/123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + validJwtToken))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThrows(es.musicalia.gestmusica.ocupacion.OrquestasDeGaliciaException.class, () -> service.obtenerActuacion(123));
        mockServer.verify();
    }

    @Test
    void testEliminarActuacion_AuthFailure_ReturnsErrorResponse() {
        mockServer.expect(requestTo(AUTH_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        var response = service.eliminarActuacion(500);

        assertFalse(response.isSuccess());
        assertEquals("error", response.getMessageType());
        assertEquals("No se ha podido autenticar con OrquestasDeGalicia", response.getMessage());
        mockServer.verify();
    }
}
