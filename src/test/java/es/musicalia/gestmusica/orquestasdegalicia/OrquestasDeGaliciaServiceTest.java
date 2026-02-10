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

import java.time.LocalDate;
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

    @BeforeEach
    void setUp() {
        // Vinculamos manualmente el MockServer al RestTemplate específico
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    private void mockAuthSuccess() throws Exception {
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("mock-jwt-token");
        
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
                .andExpect(header("Authorization", "Bearer mock-jwt-token"))
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
                .andExpect(header("Authorization", "Bearer mock-jwt-token"))
                .andRespond(withStatus(HttpStatus.CREATED).body("Created OK"));

        // WHEN
        var response = service.crearActuacion(actuacion);

        // THEN
        assertTrue(response.isSuccess());
        mockServer.verify();
    }

    @Test
    void testAuthFailure_ThrowsException() {
        // GIVEN
        mockServer.expect(requestTo(AUTH_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> service.obtenerActuacion(123));
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
        assertNull(result);
        mockServer.verify();
    }

    @Test
    void testTokenCleanupOnUnauthorized() throws Exception {
        // 1. Simular primera auth
        mockAuthSuccess();

        // 2. Simular llamada a API que devuelve 401 (token caducado)
        mockServer.expect(requestTo(API_URL + "/1"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // WHEN
        service.obtenerActuacion(1);

        // THEN: Verificar que la próxima vez que se llame a getHeaders (vía obtenerActuacion),
        // se intentará obtener el token de nuevo porque el anterior se limpió
        mockAuthSuccess(); // Segunda petición de token esperada
        mockServer.expect(requestTo(API_URL + "/1"))
                .andRespond(withSuccess());

        service.obtenerActuacion(1);
        mockServer.verify();
    }
}
