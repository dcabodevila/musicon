package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;

import java.util.Collections;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = UsuarioController.class)
@Import(WebSecurityConfig.class)
class UsuarioControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private AccesoService accesoService;
    @MockBean
    private LocalizacionService localizacionService;
    @MockBean
    private EmailService emailService;

    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private RateLimitingFilter rateLimitingFilter;
    @MockBean
    private CustomPermissionEvaluator customPermissionEvaluator;
    @MockBean
    private MensajeService mensajeService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());

        when(userDetailsService.loadUserByUsername(any())).thenReturn(
            User.withUsername("test")
                .password("{noop}password")
                .authorities("USER")
                .build()
        );
    }

    @Test
    void guardarUsuario_debeVolverAFormularioConErrorInlineCuandoEmailEstaDuplicado() throws Exception {
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());
        when(accesoService.findAllAccesosDetailRecordByIdUsuario(1L)).thenReturn(Collections.emptyList());
        when(userService.guardarUsuario(any(), any())).thenThrow(new EmailYaExisteException("El email ya está registrado por otro usuario"));

        mockMvc.perform(multipart("/usuarios/guardar")
                .with(csrf())
                .with(user("auth").roles("USER"))
                .param("id", "1")
                .param("username", "usuario")
                .param("nombre", "Nombre")
                .param("apellidos", "Apellido")
                .param("email", "duplicado@correo.com")
                .param("telefono", "600000000")
                .param("nombreComercial", "Comercial")
                .param("idProvincia", "10"))
            .andExpect(status().isOk())
            .andExpect(view().name("usuario-detail-edit"))
            .andExpect(model().attributeHasFieldErrors("usuarioEdicionDTO", "email"))
            .andExpect(model().attribute("usuarioEdicionDTO", hasProperty("id", is(1L))))
            .andExpect(model().attribute("usuarioEdicionDTO", hasProperty("username", is("usuario"))))
            .andExpect(model().attribute("usuarioEdicionDTO", hasProperty("nombre", is("Nombre"))))
            .andExpect(model().attribute("usuarioEdicionDTO", hasProperty("apellidos", is("Apellido"))))
            .andExpect(model().attribute("usuarioEdicionDTO", hasProperty("telefono", is("600000000"))))
            .andExpect(model().attribute("usuarioEdicionDTO", hasProperty("nombreComercial", is("Comercial"))))
            .andExpect(model().attribute("usuarioEdicionDTO", hasProperty("idProvincia", is(10L))))
            .andExpect(model().attributeExists("listaAccesos"))
            .andExpect(model().attributeExists("listaProvincias"));
    }

    @Test
    void guardarUsuario_debeMostrarErrorFuncionalAnteConflictoConcurrente() throws Exception {
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());
        when(accesoService.findAllAccesosDetailRecordByIdUsuario(1L)).thenReturn(Collections.emptyList());
        when(userService.guardarUsuario(any(), any())).thenThrow(new EmailYaExisteException("Conflicto tardío de unicidad"));

        mockMvc.perform(multipart("/usuarios/guardar")
                .with(csrf())
                .with(user("auth").roles("USER"))
                .param("id", "1")
                .param("username", "usuario")
                .param("nombre", "Nombre")
                .param("apellidos", "Apellido")
                .param("email", "colision@correo.com")
                .param("telefono", "600000000")
                .param("nombreComercial", "Comercial")
                .param("idProvincia", "10"))
            .andExpect(status().isOk())
            .andExpect(view().name("usuario-detail-edit"))
            .andExpect(model().attributeHasFieldErrors("usuarioEdicionDTO", "email"));
    }

    @Test
    void guardarUsuario_debeRedirigirEnFlujoExitoso() throws Exception {
        when(userService.guardarUsuario(any(), any())).thenReturn(new Usuario());

        mockMvc.perform(multipart("/usuarios/guardar")
                .with(csrf())
                .with(user("auth").roles("USER"))
                .param("id", "1")
                .param("username", "usuario")
                .param("nombre", "Nombre")
                .param("apellidos", "Apellido")
                .param("email", "ok@correo.com")
                .param("telefono", "600000000")
                .param("nombreComercial", "Comercial")
                .param("idProvincia", "10"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/usuarios/editar/1"))
            .andExpect(flash().attribute("alertClass", "success"))
            .andExpect(flash().attribute("message", "Usuario actualizado correctamente"));
    }
}
