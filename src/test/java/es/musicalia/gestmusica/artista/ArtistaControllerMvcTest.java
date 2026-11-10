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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ArtistaController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.thymeleaf.prefix=file:src/main/resources/templates/")
class ArtistaControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private ArtistaService artistaService;
    @MockitoBean
    private AgenciaService agenciaService;
    @MockitoBean
    private LocalizacionService localizacionService;
    @MockitoBean
    private MensajeService mensajeService;
    @MockitoBean
    private IncrementoService incrementoService;
    @MockitoBean
    private OcupacionService ocupacionService;
    @MockitoBean
    private SecurityService securityService;
    @MockitoBean
    private TarifaService tarifaService;
    @MockitoBean
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
    void guardarArtista_conErroresDeValidacionVuelveAlFormularioDeEdicion() throws Exception {
        stubFormularioEdicion(15L);

        mockMvc.perform(guardarArtistaRequest().param("idsComunidadesTrabajo", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("artista-detail-edit"))
                .andExpect(model().attributeHasFieldErrors("artistaDto", "idsComunidadesTrabajo"));

        verify(artistaService, never()).saveArtista(any());
    }

    @Test
    void guardarArtista_conErrorAlGuardarVuelveAlFormularioDeEdicionConAviso() throws Exception {
        stubFormularioEdicion(15L);
        when(artistaService.saveArtista(any())).thenThrow(new IllegalStateException("fallo de base de datos"));

        mockMvc.perform(guardarArtistaRequest().param("idsComunidadesTrabajo", "4"))
                .andExpect(status().isOk())
                .andExpect(view().name("artista-detail-edit"))
                .andExpect(model().attribute("message", "Error guardando artista"))
                .andExpect(model().attribute("alertClass", "danger"));
    }

    @Test
    void guardarArtista_validoRedirigeAlDetalleConMensaje() throws Exception {
        Artista artista = new Artista();
        artista.setId(15L);
        when(artistaService.saveArtista(any())).thenReturn(artista);

        mockMvc.perform(guardarArtistaRequest().param("idsComunidadesTrabajo", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artista/15"))
                .andExpect(flash().attribute("message", "Artista guardado correctamente"));
    }

    private static MockHttpServletRequestBuilder guardarArtistaRequest() {
        return multipart("/artista/guardar")
                .param("id", "15")
                .param("nombre", "Panorama Test")
                .param("idUsuario", "3")
                .param("idCcaa", "4")
                .param("idAgencia", "5")
                .param("idsTipoArtista", "6");
    }

    private void stubFormularioEdicion(long idArtista) {
        when(userService.findAllUsuarioRecordsNotAdmin()).thenReturn(List.of());
        when(agenciaService.findAgenciaRecordById(5L)).thenReturn(new AgenciaRecord(5L, "Agencia test", null, null, null, null, null, null, null, null));
        when(artistaService.listaTipoArtista()).thenReturn(List.of());
        when(artistaService.listaTipoEscenario()).thenReturn(List.of());
        when(localizacionService.findAllComunidades()).thenReturn(List.of());
        when(localizacionService.findAllProvincias()).thenReturn(List.of());
        when(ocupacionService.listarTiposOcupacion(idArtista)).thenReturn(List.of());
        when(incrementoService.listTipoIncremento()).thenReturn(List.of());
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
