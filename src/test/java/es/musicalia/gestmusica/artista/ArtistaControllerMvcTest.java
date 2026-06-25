package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.AgenciaRecord;
import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.incremento.IncrementoService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.tarifa.TarifaService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistaController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.thymeleaf.prefix=file:src/main/resources/templates/")
class ArtistaControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private FileService fileService;
    @MockBean
    private ArtistaService artistaService;
    @MockBean
    private AgenciaService agenciaService;
    @MockBean
    private LocalizacionService localizacionService;
    @MockBean
    private MensajeService mensajeService;
    @MockBean
    private IncrementoService incrementoService;
    @MockBean
    private OcupacionService ocupacionService;
    @MockBean
    private SecurityService securityService;
    @MockBean
    private TarifaService tarifaService;
    @MockBean
    private FunctionalEventTracker functionalEventTracker;

    @Test
    void editArtista_muestraSeccionEventosDelArtistaConPublicacionYCalendario() throws Exception {
        ArtistaDto artistaDto = new ArtistaDto();
        artistaDto.setId(15L);
        artistaDto.setNombre("Panorama Test");
        artistaDto.setIdUsuario(3L);
        artistaDto.setIdCcaa(4L);
        artistaDto.setIdAgencia(5L);
        artistaDto.setIdsTipoArtista(List.of(6L));
        artistaDto.setIdsComunidadesTrabajo(List.of(4L));
        artistaDto.setPermitirSuscripcionCalendario(true);
        artistaDto.setCalendarSubscriptionUrl("https://festia.es/eventos/artista/15/calendar/token-actual.ics");

        when(artistaService.findArtistaDtoById(15L)).thenReturn(artistaDto);
        when(userService.findAllUsuarioRecordsNotAdmin()).thenReturn(List.of());
        when(agenciaService.findAgenciaRecordById(5L)).thenReturn(new AgenciaRecord(5L, "Agencia test", null, null, null, null, null, null, null, null));
        when(artistaService.listaTipoArtista()).thenReturn(List.of());
        when(artistaService.listaTipoEscenario()).thenReturn(List.of());
        when(localizacionService.findAllComunidades()).thenReturn(List.of());
        when(localizacionService.findAllProvincias()).thenReturn(List.of());
        when(ocupacionService.listarTiposOcupacion(15L)).thenReturn(List.of());
        when(incrementoService.listTipoIncremento()).thenReturn(List.of());

        mockMvc.perform(get("/artista/edit/15"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Publicar eventos")))
                .andExpect(content().string(containsString("Suscripción al calendario")))
                .andExpect(content().string(containsString("Copiar URL al calendario")))
                .andExpect(content().string(containsString("Suscribirse al calendario")))
                .andExpect(content().string(containsString("https://festia.es/eventos/artista/15/calendar/token-actual.ics")));
    }

    @Test
    void regenerarTokenCalendario_devuelveRespuestaOk() throws Exception {
        when(artistaService.regenerarTokenSuscripcionCalendario(15L)).thenReturn(DefaultResponseBody.builder()
                .success(true)
                .messageType("success")
                .message("Token regenerado correctamente")
                .build());

        mockMvc.perform(post("/artista/15/calendar-subscription/regenerate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token regenerado correctamente"));
    }

    @Test
    void revocarTokenCalendario_devuelveRespuestaOk() throws Exception {
        when(artistaService.revocarTokenSuscripcionCalendario(15L)).thenReturn(DefaultResponseBody.builder()
                .success(true)
                .messageType("success")
                .message("Suscripción deshabilitada y token revocado")
                .build());

        mockMvc.perform(post("/artista/15/calendar-subscription/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Suscripción deshabilitada y token revocado"));
    }
}
