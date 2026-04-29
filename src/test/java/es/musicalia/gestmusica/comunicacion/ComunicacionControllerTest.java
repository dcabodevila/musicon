package es.musicalia.gestmusica.comunicacion;

import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.CcaaRepository;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.rol.RolRecord;
import es.musicalia.gestmusica.rol.RolRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitarios para ComunicacionController usando MockMvc standalone.
 *
 * NOTA: Los tests de integración con @WebMvcTest y @SpringBootTest fallan
 * debido a problemas de infraestructura de testing en el proyecto (beans
 * faltantes en el contexto de test). Estos tests unitarios verifican
 * el comportamiento del controller de forma aislada.
 */

/**
 * Tests unitarios para ComunicacionController usando MockMvc standalone.
 * 
 * NOTA: Los tests de integración con @WebMvcTest y @SpringBootTest fallan
 * debido a problemas de infraestructura de testing en el proyecto (beans
 * faltantes en el contexto de test). Estos tests unitarios verifican
 * el comportamiento del controller de forma aislada.
 */
@ExtendWith(MockitoExtension.class)
class ComunicacionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ComunicacionService comunicacionService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LocalizacionService localizacionService;

    @Mock
    private CcaaRepository ccaaRepository;

    @Mock
    private RolRepository rolRepository;

    @BeforeEach
    void setUp() {
        ComunicacionController controller = new ComunicacionController(
                comunicacionService, usuarioRepository, localizacionService, ccaaRepository, rolRepository);

        // Configurar view resolver para Thymeleaf
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void getComunicaciones_debeRetornarVistaConDropdowns() throws Exception {
        // Arrange
        List<CodigoNombreRecord> ccaaList = List.of(
                new CodigoNombreRecord(1L, "Galicia"),
                new CodigoNombreRecord(2L, "Asturias")
        );
        List<RolRecord> rolList = List.of(
                new RolRecord(1L, "Administrador", "Admin", "ADMIN"),
                new RolRecord(2L, "Usuario", "Usuario", "USER")
        );

        when(ccaaRepository.findAllCcaaOrderedByName()).thenReturn(ccaaList);
        when(rolRepository.findAllByCodigos(any())).thenReturn(rolList);
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/comunicaciones"))
                .andExpect(status().isOk())
                .andExpect(view().name("comunicaciones"))
                .andExpect(model().attribute("listaCcaa", ccaaList))
                .andExpect(model().attribute("listaRoles", rolList));

        verify(ccaaRepository).findAllCcaaOrderedByName();
        verify(rolRepository).findAllByCodigos(any());
    }

    @Test
    void postFiltrar_conFiltros_debeRetornarUsuariosFiltrados() throws Exception {
        // Arrange
        Usuario user1 = crearUsuario(1L, "User1", "Apellido1", "user1@test.com");
        Usuario user2 = crearUsuario(2L, "User2", "Apellido2", "user2@test.com");

        List<CodigoNombreRecord> ccaaList = List.of(new CodigoNombreRecord(1L, "Galicia"));
        List<RolRecord> rolList = List.of(new RolRecord(1L, "Usuario", "Usuario", "USER"));

        when(usuarioRepository.findUsuariosParaComunicacion(eq(1L), eq(2L), eq("USER")))
                .thenReturn(List.of(user1, user2));
        when(ccaaRepository.findAllCcaaOrderedByName()).thenReturn(ccaaList);
        when(rolRepository.findAllByCodigos(any())).thenReturn(rolList);
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post("/comunicaciones/filtrar")
                        .param("ccaaId", "1")
                        .param("provinciaId", "2")
                        .param("rolCodigo", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("comunicaciones"))
                .andExpect(model().attribute("listaUsuarios", hasSize(2)))
                .andExpect(model().attribute("ccaaIdSeleccionada", 1L))
                .andExpect(model().attribute("provinciaIdSeleccionada", 2L))
                .andExpect(model().attribute("rolCodigoSeleccionado", "USER"));

        verify(usuarioRepository).findUsuariosParaComunicacion(1L, 2L, "USER");
    }

    @Test
    void postFiltrar_sinFiltros_debeRetornarTodosLosUsuarios() throws Exception {
        // Arrange
        Usuario user1 = crearUsuario(1L, "User1", "Apellido1", "user1@test.com");

        when(usuarioRepository.findUsuariosParaComunicacion(isNull(), isNull(), isNull()))
                .thenReturn(List.of(user1));
        when(ccaaRepository.findAllCcaaOrderedByName()).thenReturn(Collections.emptyList());
        when(rolRepository.findAllByCodigos(any())).thenReturn(Collections.emptyList());
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post("/comunicaciones/filtrar"))
                .andExpect(status().isOk())
                .andExpect(view().name("comunicaciones"))
                .andExpect(model().attribute("listaUsuarios", hasSize(1)));

        verify(usuarioRepository).findUsuariosParaComunicacion(null, null, null);
    }

    @Test
    void getRedactar_conIds_debeRetornarVistaRedactar() throws Exception {
        // Arrange
        Usuario user1 = crearUsuario(1L, "User1", "Apellido1", "user1@test.com");
        Usuario user2 = crearUsuario(2L, "User2", "Apellido2", "user2@test.com");

        when(usuarioRepository.findAllByIdWithRelaciones(List.of(1L, 2L))).thenReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/comunicaciones/redactar")
                        .param("ids", "1,2"))
                .andExpect(status().isOk())
                .andExpect(view().name("comunicaciones-redactar"))
                .andExpect(model().attribute("usuariosSeleccionados", hasSize(2)))
                .andExpect(model().attribute("totalUsuarios", 2))
                .andExpect(model().attribute("ids", "1,2"));

        verify(usuarioRepository).findAllByIdWithRelaciones(List.of(1L, 2L));
    }

    @Test
    void getRedactar_sinIds_debeRetornarVistaConListaVacia() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/comunicaciones/redactar"))
                .andExpect(status().isOk())
                .andExpect(view().name("comunicaciones-redactar"))
                .andExpect(model().attribute("usuariosSeleccionados", empty()))
                .andExpect(model().attribute("totalUsuarios", 0))
                .andExpect(model().attribute("ids", ""));
    }

    @Test
    void postEnviar_conDatosValidos_debeEnviarYRedirigir() throws Exception {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        ComunicacionResult resultado = new ComunicacionResult(2, 0, 0);

        when(comunicacionService.enviarComunicacion(eq(ids), eq("Asunto test"), eq("<p>Mensaje</p>"), eq("Mensaje")))
                .thenReturn(resultado);

        // Act & Assert
        mockMvc.perform(post("/comunicaciones/enviar")
                        .param("ids", "1,2")
                        .param("asunto", "Asunto test")
                        .param("htmlBody", "<p>Mensaje</p>")
                        .param("textoPlano", "Mensaje"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/comunicaciones"))
                .andExpect(flash().attribute("alertClass", "success"))
                .andExpect(flash().attribute("message", "Comunicación enviada: 2 exitosos, 0 fallidos, 0 excluidos por baja"));

        verify(comunicacionService).enviarComunicacion(ids, "Asunto test", "<p>Mensaje</p>", "Mensaje");
    }

    @Test
    void postEnviar_conFallos_debeEnviarYRedirigirConMensajeWarning() throws Exception {
        // Arrange
        List<Long> ids = List.of(1L, 2L, 3L);
        ComunicacionResult resultado = new ComunicacionResult(1, 1, 1);

        when(comunicacionService.enviarComunicacion(eq(ids), any(), any(), any()))
                .thenReturn(resultado);

        // Act & Assert
        mockMvc.perform(post("/comunicaciones/enviar")
                        .param("ids", "1,2,3")
                        .param("asunto", "Asunto")
                        .param("htmlBody", "<p>Body</p>")
                        .param("textoPlano", "Body"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/comunicaciones"))
                .andExpect(flash().attribute("alertClass", "warning"))
                .andExpect(flash().attribute("message", "Comunicación enviada: 1 exitosos, 1 fallidos, 1 excluidos por baja"));
    }

    @Test
    void postEnviar_conListaVacia_debeRedirigirConError() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/comunicaciones/enviar")
                        .param("ids", "")
                        .param("asunto", "Asunto")
                        .param("htmlBody", "<p>Body</p>")
                        .param("textoPlano", "Body"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/comunicaciones"))
                .andExpect(flash().attribute("alertClass", "danger"))
                .andExpect(flash().attribute("message", "Debe seleccionar al menos un destinatario"));

        verifyNoInteractions(comunicacionService);
    }

    private Usuario crearUsuario(Long id, String nombre, String apellidos, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos);
        usuario.setEmail(email);
        usuario.setActivo(true);
        return usuario;
    }
}
