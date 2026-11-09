package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.config.CustomPermissionEvaluator;
import es.musicalia.gestmusica.config.RateLimitingFilter;
import es.musicalia.gestmusica.config.WebSecurityConfig;
import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventNames;
import es.musicalia.gestmusica.observabilidad.FunctionalEventOutcome;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.*;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Characterization tests of the public registration flow in {@link AuthController}:
 * form rendering, server-side validation, duplicate username/email, successful submission
 * and e-mail verification. Complements {@link AuthControllerTest} with the side effects
 * (nothing saved on rejected input, functional tracking, activation and autologin).
 */
@WebMvcTest(AuthController.class)
@Import(WebSecurityConfig.class)
class RegistroControllerTest {

    private static final List<CodigoNombreRecord> PROVINCIAS = List.of(
            new CodigoNombreRecord(15L, "A Coruña"),
            new CodigoNombreRecord(36L, "Pontevedra"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private CodigoVerificacionService codigoVerificacionService;
    @MockitoBean
    private LocalizacionService localizacionService;
    @MockitoBean
    private SecurityService securityService;
    @MockitoBean
    private FunctionalEventTracker functionalEventTracker;
    @MockitoBean
    private MensajeService mensajeService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;
    @MockitoBean
    private CustomPermissionEvaluator customPermissionEvaluator;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
        when(localizacionService.findAllProvincias()).thenReturn(PROVINCIAS);
    }

    private static MockHttpServletRequestBuilder validRegistration() {
        return post("/auth/registration")
                .with(csrf())
                .param("username", "nuevoUser")
                .param("nombre", "Nombre")
                .param("apellidos", "Apellidos")
                .param("password", "password123")
                .param("retryPassword", "password123")
                .param("email", "nuevo@test.com")
                .param("nombreComercial", "Comercial")
                .param("telefono", "600000000")
                .param("idProvincia", "15")
                .param("tipoUsuario", "ARTISTA");
    }

    private void verifyRegistrationFailureTracked(String username, String reason) {
        verify(functionalEventTracker).track(
                eq(FunctionalEventNames.AUTH_REGISTRATION_SUBMITTED),
                eq(FunctionalEventOutcome.FAILURE),
                isNull(),
                eq(username),
                eq(Map.of("reason", reason)));
    }

    // ==================== GET /auth/registration ====================

    @Test
    void registration_get_rendersEmptyFormWithProvinciasAndTiposUsuario() throws Exception {
        mockMvc.perform(get("/auth/registration"))
                .andExpect(status().isOk())
                .andExpect(view().name("registration"))
                .andExpect(model().attribute("listaProvincias", PROVINCIAS))
                .andExpect(model().attribute("registrationForm", hasProperty("username", nullValue())))
                .andExpect(content().string(containsString("action=\"/auth/registration\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"retryPassword\"")))
                .andExpect(content().string(containsString("<option value=\"15\">A Coruña</option>")))
                .andExpect(content().string(containsString("<option value=\"36\">Pontevedra</option>")))
                .andExpect(content().string(containsString("value=\"AGENCIA\"")))
                .andExpect(content().string(containsString("value=\"REPRESENTANTE\"")))
                .andExpect(content().string(containsString("value=\"ARTISTA\"")))
                .andExpect(content().string(not(containsString("value=\"PUBLICO\""))));

        verifyNoInteractions(userService, functionalEventTracker);
    }

    // ==================== POST /auth/registration: rejected input ====================

    @Test
    void registration_post_withoutCsrf_isForbiddenAndSavesNothing() throws Exception {
        mockMvc.perform(post("/auth/registration")
                        .param("username", "nuevoUser")
                        .param("password", "password123")
                        .param("retryPassword", "password123"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService, functionalEventTracker);
    }

    @ParameterizedTest(name = "{0} = ''{1}'' -> {2}")
    @CsvSource({
            "nombre, '', NotEmpty",
            "apellidos, '', NotEmpty",
            "email, '', NotEmpty",
            "email, no-es-un-email, Email",
            "nombreComercial, '', NotEmpty",
            "telefono, '', NotEmpty",
            "idProvincia, '', NotNull",
            "tipoUsuario, '', NotNull",
            "tipoUsuario, DESCONOCIDO, typeMismatch",
            "idProvincia, abc, typeMismatch"
    })
    void registration_post_invalidField_reRendersFormWithFieldErrorAndSavesNothing(
            String field, String value, String errorCode) throws Exception {
        mockMvc.perform(validRegistration()
                        // param() would append a second value; replace the default one instead
                        .with(request -> {
                            request.setParameter(field, value);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(view().name("registration"))
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrorCode("registrationForm", field, errorCode))
                .andExpect(model().attribute("listaProvincias", PROVINCIAS));

        verify(userService, never()).saveRegistration(any());
        verifyRegistrationFailureTracked("nuevoUser", "validation");
    }

    @Test
    void registration_post_missingTipoUsuario_rendersCustomMessage() throws Exception {
        mockMvc.perform(validRegistration().with(request -> {
                    request.removeParameter("tipoUsuario");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("registration"))
                .andExpect(model().attributeHasFieldErrorCode("registrationForm", "tipoUsuario", "NotNull"))
                .andExpect(content().string(containsString("El tipo de usuario es obligatorio")));

        verify(userService, never()).saveRegistration(any());
    }

    @Test
    void registration_post_usernameExists_checksTrimmedUsernameRendersMessageAndSavesNothing() throws Exception {
        when(userService.usernameExists("existente")).thenReturn(true);

        mockMvc.perform(validRegistration().with(request -> {
                    request.setParameter("username", "  existente  ");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("registration"))
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrorCode("registrationForm", "username", "error.username-exists"))
                .andExpect(model().attribute("listaProvincias", PROVINCIAS))
                .andExpect(content().string(containsString("El nombre de usuario ya existe")));

        verify(userService).usernameExists("existente");
        verify(userService, never()).saveRegistration(any());
        verifyRegistrationFailureTracked("  existente  ", "validation");
    }

    @Test
    void registration_post_passwordsDoNotMatch_rejectsBothFieldsAndSavesNothing() throws Exception {
        mockMvc.perform(validRegistration().with(request -> {
                    request.setParameter("retryPassword", "Password123");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("registration"))
                .andExpect(model().errorCount(2))
                .andExpect(model().attributeHasFieldErrorCode("registrationForm", "password", "error.password-retry"))
                .andExpect(model().attributeHasFieldErrorCode("registrationForm", "retryPassword", "error.password-retry"))
                .andExpect(content().string(containsString("Las contraseñas no coinciden")));

        verify(userService, never()).saveRegistration(any());
        verifyRegistrationFailureTracked("nuevoUser", "validation");
    }

    @Test
    void registration_post_emptyMatchingPasswords_rejectsPasswordFieldsAsNotEmpty() throws Exception {
        mockMvc.perform(validRegistration().with(request -> {
                    request.setParameter("password", "");
                    request.setParameter("retryPassword", "");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("registration"))
                .andExpect(model().errorCount(2))
                .andExpect(model().attributeHasFieldErrorCode("registrationForm", "password", "NotEmpty"))
                .andExpect(model().attributeHasFieldErrorCode("registrationForm", "retryPassword", "NotEmpty"));

        verify(userService, never()).saveRegistration(any());
    }

    @Test
    void registration_post_emailAlreadyRegistered_rejectsEmailFieldAndTracksReason() throws Exception {
        when(userService.saveRegistration(any())).thenThrow(new EmailYaExisteException("El email ya está registrado"));

        mockMvc.perform(validRegistration())
                .andExpect(status().isOk())
                .andExpect(view().name("registration"))
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrorCode("registrationForm", "email", "error.email"))
                .andExpect(model().attribute("listaProvincias", PROVINCIAS))
                .andExpect(content().string(containsString("El email ya está registrado por otro usuario")));

        verify(userService).saveRegistration(any());
        verifyRegistrationFailureTracked("nuevoUser", "email_exists");
        verify(functionalEventTracker, never()).track(anyString(), eq(FunctionalEventOutcome.SUCCESS), any(), any(), any());
    }

    @Test
    void registration_post_unexpectedError_reRendersFormAndTracksReason() throws Exception {
        when(userService.saveRegistration(any())).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(validRegistration())
                .andExpect(status().isOk())
                .andExpect(view().name("registration"))
                .andExpect(model().attributeExists("errors"))
                .andExpect(model().attribute("listaProvincias", PROVINCIAS))
                .andExpect(flash().attributeCount(0));

        verifyRegistrationFailureTracked("nuevoUser", "unexpected_error");
    }

    // ==================== POST /auth/registration: success ====================

    @Test
    void registration_post_valid_savesFormDataTracksSuccessAndRedirectsWithEmail() throws Exception {
        Usuario registrado = new Usuario();
        registrado.setId(42L);
        registrado.setUsername("nuevoUser");
        when(userService.saveRegistration(any())).thenReturn(registrado);

        mockMvc.perform(validRegistration())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/verify-email"))
                .andExpect(flash().attribute("email", "nuevo@test.com"));

        ArgumentCaptor<RegistrationForm> captor = ArgumentCaptor.forClass(RegistrationForm.class);
        verify(userService).saveRegistration(captor.capture());
        RegistrationForm form = captor.getValue();
        assertThat(form.getUsername()).isEqualTo("nuevoUser");
        assertThat(form.getNombre()).isEqualTo("Nombre");
        assertThat(form.getApellidos()).isEqualTo("Apellidos");
        assertThat(form.getPassword()).isEqualTo("password123");
        assertThat(form.getEmail()).isEqualTo("nuevo@test.com");
        assertThat(form.getNombreComercial()).isEqualTo("Comercial");
        assertThat(form.getTelefono()).isEqualTo("600000000");
        assertThat(form.getIdProvincia()).isEqualTo(15L);
        assertThat(form.getTipoUsuario()).isEqualTo(TipoUsuarioEnum.ARTISTA);

        verify(functionalEventTracker).track(
                FunctionalEventNames.AUTH_REGISTRATION_SUBMITTED,
                FunctionalEventOutcome.SUCCESS,
                42L,
                "nuevoUser",
                Map.of("tipo_usuario", "ARTISTA"));
        verify(functionalEventTracker, never()).track(anyString(), eq(FunctionalEventOutcome.FAILURE), any(), any(), any());
    }

    @Test
    void registration_post_apodoIsOptional() throws Exception {
        Usuario registrado = new Usuario();
        registrado.setId(7L);
        registrado.setUsername("nuevoUser");
        when(userService.saveRegistration(any())).thenReturn(registrado);

        mockMvc.perform(validRegistration())
                .andExpect(status().is3xxRedirection())
                .andExpect(model().hasNoErrors());

        ArgumentCaptor<RegistrationForm> captor = ArgumentCaptor.forClass(RegistrationForm.class);
        verify(userService).saveRegistration(captor.capture());
        assertThat(captor.getValue().getApodo()).isNull();
    }

    // ==================== GET /auth/verify-email ====================

    @Test
    void verifyEmail_get_exposesFlashEmailToView() throws Exception {
        mockMvc.perform(get("/auth/verify-email").flashAttr("email", "nuevo@test.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("verify-email"))
                .andExpect(model().attribute("email", "nuevo@test.com"));
    }

    // ==================== POST /auth/verify-email ====================

    @Test
    void verifyEmail_post_validCode_activatesUserAndAutologins() throws Exception {
        when(codigoVerificacionService.verificarCodigo("nuevo@test.com", "1234", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(true);

        mockMvc.perform(post("/auth/verify-email").with(csrf())
                        .param("email", "nuevo@test.com")
                        .param("codigo", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Email verificado correctamente.")))
                .andExpect(jsonPath("$.redirect", is("/")));

        verify(userService).activateUserByEmail("nuevo@test.com");
        verify(securityService).autologin(eq("nuevo@test.com"), any());
    }

    @Test
    void verifyEmail_post_validCodeButAutologinFails_stillActivatesAndSendsToLogin() throws Exception {
        when(codigoVerificacionService.verificarCodigo("nuevo@test.com", "1234", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(true);
        doThrow(new IllegalStateException("no session")).when(securityService).autologin(eq("nuevo@test.com"), any());

        mockMvc.perform(post("/auth/verify-email").with(csrf())
                        .param("email", "nuevo@test.com")
                        .param("codigo", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Email verificado correctamente. Ya puedes iniciar sesión.")))
                .andExpect(jsonPath("$.redirect", is("/auth/login")));

        verify(userService).activateUserByEmail("nuevo@test.com");
    }

    @Test
    void verifyEmail_post_wrongOrExpiredCode_doesNotActivateNorLogin() throws Exception {
        when(codigoVerificacionService.verificarCodigo("nuevo@test.com", "9999", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(false);

        mockMvc.perform(post("/auth/verify-email").with(csrf())
                        .param("email", "nuevo@test.com")
                        .param("codigo", "9999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Código incorrecto o expirado.")))
                .andExpect(jsonPath("$.redirect").doesNotExist());

        verify(userService, never()).activateUserByEmail(anyString());
        verifyNoInteractions(securityService);
    }

    @Test
    void verifyEmail_post_userNotFoundOnActivation_returnsGenericErrorWithoutLogin() throws Exception {
        when(codigoVerificacionService.verificarCodigo("huerfano@test.com", "1234", EmailTemplateEnum.REGISTRO, false))
                .thenReturn(true);
        when(userService.activateUserByEmail("huerfano@test.com"))
                .thenThrow(new UsuarioNoEncontradoException("No se encontró usuario con email: huerfano@test.com"));

        mockMvc.perform(post("/auth/verify-email").with(csrf())
                        .param("email", "huerfano@test.com")
                        .param("codigo", "1234"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Error en la verificación. Inténtalo de nuevo más tarde.")));

        verifyNoInteractions(securityService);
    }

    // ==================== POST /auth/resend-code ====================

    @Test
    void resendCode_post_usesRegistroTemplateAndReturnsMessages() throws Exception {
        when(codigoVerificacionService.reenviarCodigo("nuevo@test.com", EmailTemplateEnum.REGISTRO)).thenReturn(true);
        when(codigoVerificacionService.reenviarCodigo("pronto@test.com", EmailTemplateEnum.REGISTRO)).thenReturn(false);

        mockMvc.perform(post("/auth/resend-code").with(csrf()).param("email", "nuevo@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Código reenviado correctamente.")));
        mockMvc.perform(post("/auth/resend-code").with(csrf()).param("email", "pronto@test.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Debe esperar antes de reenviar el código.")));

        verify(codigoVerificacionService, never()).reenviarCodigo(anyString(), eq(EmailTemplateEnum.RECUPERACION_PASSWORD));
    }
}
