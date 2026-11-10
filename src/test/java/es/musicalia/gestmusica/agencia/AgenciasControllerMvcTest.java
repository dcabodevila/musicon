package es.musicalia.gestmusica.agencia;

import es.musicalia.gestmusica.agencia.publicacioneventos.AgenciaPublicacionEventosService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.tarifa.TarifaService;
import es.musicalia.gestmusica.usuario.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AgenciasController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.thymeleaf.prefix=file:src/main/resources/templates/")
@WithMockUser
class AgenciasControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private LocalizacionService localizacionService;
    @MockitoBean private AgenciaService agenciaService;
    @MockitoBean private FileService fileService;
    @MockitoBean private ArtistaService artistaService;
    @MockitoBean private SecurityService securityService;
    @MockitoBean private TarifaService tarifaService;
    @MockitoBean private AgenciaPublicacionEventosService agenciaPublicacionEventosService;
    @MockitoBean private FunctionalEventTracker functionalEventTracker;
    @MockitoBean private MensajeService mensajeService;

    @BeforeEach
    void setUp() {
        when(localizacionService.findAllProvincias()).thenReturn(List.of());
        when(userService.findAllUsuarioRecordsNotAdmin()).thenReturn(List.of());
    }

    @Test
    void guardarAgencia_conErroresDeValidacionVuelveAlFormularioDeEdicion() throws Exception {
        mockMvc.perform(guardarAgenciaRequest().param("nombre", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("agencia-detail-edit"))
                .andExpect(model().attributeHasFieldErrors("agenciaDto", "nombre"))
                .andExpect(model().attributeExists("listaProvincias", "listaUsuarios"));

        verify(agenciaService, never()).saveAgencia(any());
    }

    @Test
    void guardarAgencia_conErrorAlGuardarVuelveAlFormularioDeEdicionConAviso() throws Exception {
        when(agenciaService.saveAgencia(any())).thenThrow(new IllegalStateException("fallo de base de datos"));

        mockMvc.perform(guardarAgenciaRequest().param("nombre", "Agencia test"))
                .andExpect(status().isOk())
                .andExpect(view().name("agencia-detail-edit"))
                .andExpect(model().attribute("message", "Error guardando agencia"))
                .andExpect(model().attributeExists("listaProvincias", "listaUsuarios"));
    }

    @Test
    void guardarAgencia_validaRedirigeAlDetalleConMensaje() throws Exception {
        Agencia agencia = new Agencia();
        agencia.setId(96L);
        when(agenciaService.saveAgencia(any())).thenReturn(agencia);

        mockMvc.perform(guardarAgenciaRequest().param("nombre", "Agencia test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/agencia/96"))
                .andExpect(flash().attribute("message", "Agencia guardada correctamente"));
    }

    private static MockHttpServletRequestBuilder guardarAgenciaRequest() {
        return multipart("/agencia/guardar")
                .param("id", "96")
                .param("idUsuario", "7");
    }
}
