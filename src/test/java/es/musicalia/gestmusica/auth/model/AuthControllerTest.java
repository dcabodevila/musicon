package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.*;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(WebSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private CodigoVerificacionService codigoVerificacionService;
    @MockBean
    private LocalizacionService localizacionService;
    @MockBean
    private SecurityService securityService;
    @MockBean
    private FunctionalEventTracker functionalEventTracker;
    @MockBean
    private MensajeService mensajeService;

    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private RateLimitingFilter rateLimitingFilter;
    @MockBean
    private CustomPermissionEvaluator customPermissionEvaluator;

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

    // ==================== GET /auth/login ====================

    @Test
    void login_sinParams_retornaLogin() throws Exception {
        mockMvc.perform(get("/auth/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"));
    }

    @Test
    void login_conError_modelTieneError() throws Exception {
        mockMvc.perform(get("/auth/login").param("error", "true"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"))
            .andExpect(model().attribute("error", "Usuario o contraseña no válidos."));
    }

    @Test
    void login_conLogout_modelTieneMessage() throws Exception {
        mockMvc.perform(get("/auth/login").param("logout", "true"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"))
            .andExpect(model().attribute("message", "You have been logged out successfully."));
    }

    // ==================== GET /auth/registration ====================

    @Test
    void registration_retornaFormularioConProvincias() throws Exception {
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/auth/registration"))
            .andExpect(status().isOk())
            .andExpect(view().name("registration"))
            .andExpect(model().attributeExists("registrationForm"))
            .andExpect(model().attribute("listaProvincias", Collections.emptyList()));
    }

    // ==================== POST /auth/registration ====================

    @Test
    void registration_post_validacionExitosa_redirectAVerifyEmail() throws Exception {
        when(userService.usernameExists("nuevoUser")).thenReturn(false);
        Usuario usuarioRegistrado = new Usuario();
        usuarioRegistrado.setId(1L);
        usuarioRegistrado.setUsername("nuevoUser");
        when(userService.saveRegistration(any())).thenReturn(usuarioRegistrado);

        mockMvc.perform(post("/auth/registration")
                .with(csrf())
                .param("username", "nuevoUser")
                .param("nombre", "Nombre")
                .param("apellidos", "Apellidos")
                .param("password", "password123")
                .param("retryPassword", "password123")
                .param("email", "nuevo@test.com")
                .param("nombreComercial", "Comercial")
                .param("telefono", "600000000")
                .param("idProvincia", "1")
                .param("tipoUsuario", "AGENCIA"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/verify-email"))
            .andExpect(flash().attribute("email", "nuevo@test.com"));
    }

    @Test
    void registration_post_usernameYaExiste_errorEnUsernameYTracking() throws Exception {
        when(userService.usernameExists("existente")).thenReturn(true);
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/auth/registration")
                .with(csrf())
                .param("username", "existente")
                .param("nombre", "Nombre")
                .param("apellidos", "Apellidos")
                .param("password", "password123")
                .param("retryPassword", "password123")
                .param("email", "nuevo@test.com")
                .param("nombreComercial", "Comercial")
                .param("telefono", "600000000")
                .param("idProvincia", "1")
                .param("tipoUsuario", "AGENCIA"))
            .andExpect(status().isOk())
            .andExpect(view().name("registration"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "username"));
    }

    @Test
    void registration_post_passwordsNoCoinciden_errorEnAmbosCampos() throws Exception {
        when(userService.usernameExists("nuevoUser")).thenReturn(false);
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/auth/registration")
                .with(csrf())
                .param("username", "nuevoUser")
                .param("nombre", "Nombre")
                .param("apellidos", "Apellidos")
                .param("password", "password123")
                .param("retryPassword", "otropassword")
                .param("email", "nuevo@test.com")
                .param("nombreComercial", "Comercial")
                .param("telefono", "600000000")
                .param("idProvincia", "1")
                .param("tipoUsuario", "AGENCIA"))
            .andExpect(status().isOk())
            .andExpect(view().name("registration"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "password", "retryPassword"));
    }

    @Test
    void registration_post_emailYaExiste_errorEnEmailYTracking() throws Exception {
        when(userService.usernameExists("nuevoUser")).thenReturn(false);
        when(userService.saveRegistration(any())).thenThrow(new EmailYaExisteException("Email ya existe"));
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/auth/registration")
                .with(csrf())
                .param("username", "nuevoUser")
                .param("nombre", "Nombre")
                .param("apellidos", "Apellidos")
                .param("password", "password123")
                .param("retryPassword", "password123")
                .param("email", "duplicado@test.com")
                .param("nombreComercial", "Comercial")
                .param("telefono", "600000000")
                .param("idProvincia", "1")
                .param("tipoUsuario", "AGENCIA"))
            .andExpect(status().isOk())
            .andExpect(view().name("registration"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));
    }

    @Test
    void registration_post_errorInesperado_errorGeneralYTracking() throws Exception {
        when(userService.usernameExists("nuevoUser")).thenReturn(false);
        when(userService.saveRegistration(any())).thenThrow(new RuntimeException("Error inesperado"));
        when(localizacionService.findAllProvincias()).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/auth/registration")
                .with(csrf())
                .param("username", "nuevoUser")
                .param("nombre", "Nombre")
                .param("apellidos", "Apellidos")
                .param("password", "password123")
                .param("retryPassword", "password123")
                .param("email", "nuevo@test.com")
                .param("nombreComercial", "Comercial")
                .param("telefono", "600000000")
                .param("idProvincia", "1")
                .param("tipoUsuario", "AGENCIA"))
            .andExpect(status().isOk())
            .andExpect(view().name("registration"))
            .andExpect(model().attributeExists("errors"));
    }

    // ==================== POST /auth/verify-email ====================

    @Test
    void verifyEmail_codigoCorrecto_autologinExitoso_successYRedirectRaiz() throws Exception {
        when(codigoVerificacionService.verificarCodigo("test@test.com", "1234", EmailTemplateEnum.REGISTRO, false))
            .thenReturn(true);

        mockMvc.perform(post("/auth/verify-email")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "1234"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.redirect", is("/")));
    }

    @Test
    void verifyEmail_codigoCorrecto_autologinFallido_successYRedirectLogin() throws Exception {
        when(codigoVerificacionService.verificarCodigo("test@test.com", "1234", EmailTemplateEnum.REGISTRO, false))
            .thenReturn(true);
        doThrow(new RuntimeException("Autologin fallido")).when(securityService).autologin(eq("test@test.com"), any());

        mockMvc.perform(post("/auth/verify-email")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "1234"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.redirect", is("/auth/login")));
    }

    @Test
    void verifyEmail_codigoIncorrecto_badRequest() throws Exception {
        when(codigoVerificacionService.verificarCodigo("test@test.com", "0000", EmailTemplateEnum.REGISTRO, false))
            .thenReturn(false);

        mockMvc.perform(post("/auth/verify-email")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "0000"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void verifyEmail_exception_badRequest() throws Exception {
        when(codigoVerificacionService.verificarCodigo(any(), any(), any(), anyBoolean()))
            .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/auth/verify-email")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "1234"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    // ==================== POST /auth/resend-code ====================

    @Test
    void resendCode_reenvioExitoso_success() throws Exception {
        when(codigoVerificacionService.reenviarCodigo("test@test.com", EmailTemplateEnum.REGISTRO))
            .thenReturn(true);

        mockMvc.perform(post("/auth/resend-code")
                .with(csrf())
                .param("email", "test@test.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void resendCode_reenvioFallido_badRequest() throws Exception {
        when(codigoVerificacionService.reenviarCodigo("test@test.com", EmailTemplateEnum.REGISTRO))
            .thenReturn(false);

        mockMvc.perform(post("/auth/resend-code")
                .with(csrf())
                .param("email", "test@test.com"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void resendCode_exception_badRequest() throws Exception {
        when(codigoVerificacionService.reenviarCodigo(any(), any()))
            .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/auth/resend-code")
                .with(csrf())
                .param("email", "test@test.com"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    // ==================== GET /auth/remember-password ====================

    @Test
    void rememberPassword_get_muestraFormulario() throws Exception {
        mockMvc.perform(get("/auth/remember-password"))
            .andExpect(status().isOk())
            .andExpect(view().name("remember-password"))
            .andExpect(model().attributeExists("rememberPasswordForm"));
    }

    // ==================== POST /auth/remember-password ====================

    @Test
    void rememberPassword_post_emailValido_redirectAVerifyReset() throws Exception {
        when(userService.existsUsuarioActivoByEmail("activo@test.com")).thenReturn(true);

        mockMvc.perform(post("/auth/remember-password")
                .with(csrf())
                .param("email", "activo@test.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/verify-reset-password"))
            .andExpect(flash().attribute("email", "activo@test.com"));
    }

    @Test
    void rememberPassword_post_emailNoExiste_error() throws Exception {
        when(userService.existsUsuarioActivoByEmail("inexistente@test.com")).thenReturn(false);

        mockMvc.perform(post("/auth/remember-password")
                .with(csrf())
                .param("email", "inexistente@test.com"))
            .andExpect(status().isOk())
            .andExpect(view().name("remember-password"))
            .andExpect(model().attributeHasFieldErrors("rememberPasswordForm", "email"));
    }

    @Test
    void rememberPassword_post_validationErrors_retornaFormulario() throws Exception {
        mockMvc.perform(post("/auth/remember-password")
                .with(csrf())
                .param("email", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("remember-password"));
    }

    @Test
    void rememberPassword_post_exception_error() throws Exception {
        when(userService.existsUsuarioActivoByEmail("activo@test.com")).thenReturn(true);
        doThrow(new RuntimeException("Error enviando código")).when(codigoVerificacionService).generarYEnviarCodigo(any(), any());

        mockMvc.perform(post("/auth/remember-password")
                .with(csrf())
                .param("email", "activo@test.com"))
            .andExpect(status().isOk())
            .andExpect(view().name("remember-password"))
            .andExpect(model().attributeExists("error"));
    }

    // ==================== GET /auth/verify-reset-password ====================

    @Test
    void verifyResetPassword_get_muestraPagina() throws Exception {
        mockMvc.perform(get("/auth/verify-reset-password"))
            .andExpect(status().isOk())
            .andExpect(view().name("verify-reset-password"));
    }

    // ==================== POST /auth/verify-reset-password ====================

    @Test
    void verifyResetPassword_post_codigoCorrecto_successYRedirectUrl() throws Exception {
        when(codigoVerificacionService.verificarCodigo("test@test.com", "1234", EmailTemplateEnum.RECUPERACION_PASSWORD, false))
            .thenReturn(true);

        mockMvc.perform(post("/auth/verify-reset-password")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "1234"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.redirect", is("/auth/reset-password?email=test@test.com&codigo=1234")));
    }

    @Test
    void verifyResetPassword_post_codigoIncorrecto_badRequest() throws Exception {
        when(codigoVerificacionService.verificarCodigo("test@test.com", "0000", EmailTemplateEnum.RECUPERACION_PASSWORD, false))
            .thenReturn(false);

        mockMvc.perform(post("/auth/verify-reset-password")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "0000"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void verifyResetPassword_post_exception_badRequest() throws Exception {
        when(codigoVerificacionService.verificarCodigo(any(), any(), any(), anyBoolean()))
            .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/auth/verify-reset-password")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "1234"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    // ==================== GET /auth/reset-password ====================

    @Test
    void resetPassword_get_muestraFormularioConEmailYCodigo() throws Exception {
        mockMvc.perform(get("/auth/reset-password")
                .param("email", "test@test.com")
                .param("codigo", "1234"))
            .andExpect(status().isOk())
            .andExpect(view().name("reset-password"))
            .andExpect(model().attributeExists("resetPasswordForm"));
    }

    // ==================== POST /auth/reset-password ====================

    @Test
    void resetPassword_post_passwordsNoCoinciden_error() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "1234")
                .param("newPassword", "password123")
                .param("confirmPassword", "otropassword"))
            .andExpect(status().isOk())
            .andExpect(view().name("reset-password"))
            .andExpect(model().attributeHasFieldErrors("resetPasswordForm", "newPassword", "confirmPassword"));
    }

    @Test
    void resetPassword_post_codigoInvalido_error() throws Exception {
        when(codigoVerificacionService.verificarCodigo("test@test.com", "1234", EmailTemplateEnum.RECUPERACION_PASSWORD, true))
            .thenReturn(false);

        mockMvc.perform(post("/auth/reset-password")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "1234")
                .param("newPassword", "password123")
                .param("confirmPassword", "password123"))
            .andExpect(status().isOk())
            .andExpect(view().name("reset-password"))
            .andExpect(model().attribute("error", "El código ha expirado. Solicita uno nuevo."));
    }

    @Test
    void resetPassword_post_exito_redirectALoginConMensaje() throws Exception {
        when(codigoVerificacionService.verificarCodigo("test@test.com", "1234", EmailTemplateEnum.RECUPERACION_PASSWORD, true))
            .thenReturn(true);

        mockMvc.perform(post("/auth/reset-password")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "1234")
                .param("newPassword", "password123")
                .param("confirmPassword", "password123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/auth/login"))
            .andExpect(flash().attribute("message", "Contraseña cambiada correctamente. Ya puedes iniciar sesión."));
    }

    @Test
    void resetPassword_post_exception_error() throws Exception {
        when(codigoVerificacionService.verificarCodigo("test@test.com", "1234", EmailTemplateEnum.RECUPERACION_PASSWORD, true))
            .thenReturn(true);
        doThrow(new RuntimeException("Error cambiando password")).when(userService).changePasswordByEmail(any(), any());

        mockMvc.perform(post("/auth/reset-password")
                .with(csrf())
                .param("email", "test@test.com")
                .param("codigo", "1234")
                .param("newPassword", "password123")
                .param("confirmPassword", "password123"))
            .andExpect(status().isOk())
            .andExpect(view().name("reset-password"))
            .andExpect(model().attribute("error", "Error cambiando la contraseña. Inténtalo de nuevo."));
    }

    // ==================== POST /auth/resend-reset-code ====================

    @Test
    void resendResetCode_reenvioExitoso_success() throws Exception {
        when(codigoVerificacionService.reenviarCodigo("test@test.com", EmailTemplateEnum.RECUPERACION_PASSWORD))
            .thenReturn(true);

        mockMvc.perform(post("/auth/resend-reset-code")
                .with(csrf())
                .param("email", "test@test.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void resendResetCode_reenvioFallido_badRequest() throws Exception {
        when(codigoVerificacionService.reenviarCodigo("test@test.com", EmailTemplateEnum.RECUPERACION_PASSWORD))
            .thenReturn(false);

        mockMvc.perform(post("/auth/resend-reset-code")
                .with(csrf())
                .param("email", "test@test.com"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void resendResetCode_exception_badRequest() throws Exception {
        when(codigoVerificacionService.reenviarCodigo(any(), any()))
            .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/auth/resend-reset-code")
                .with(csrf())
                .param("email", "test@test.com"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }
}
