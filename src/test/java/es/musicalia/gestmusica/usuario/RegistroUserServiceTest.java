package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.auth.model.RegistrationForm;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.mensaje.Mensaje;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.rol.Rol;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.rol.RolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Characterization tests of user registration in {@link UserServiceImpl}: duplicate e-mail,
 * the persisted user state, the verification code and the administrator notifications.
 */
@ExtendWith(MockitoExtension.class)
class RegistroUserServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CodigoVerificacionService codigoVerificacionService;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private UsuarioMapper usuarioMapper;
    @Mock
    private FileService fileService;
    @Mock
    private EmailService emailService;
    @Mock
    private ProvinciaRepository provinciaRepository;
    @Mock
    private MensajeService mensajeService;

    private UserServiceImpl userService;
    private Provincia provincia;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                usuarioRepository,
                passwordEncoder,
                codigoVerificacionService,
                rolRepository,
                usuarioMapper,
                fileService,
                emailService,
                provinciaRepository,
                mensajeService
        );
        provincia = new Provincia();
        provincia.setId(15L);
        provincia.setNombre("A Coruña");
    }

    private static RegistrationForm form(TipoUsuarioEnum tipo) {
        RegistrationForm form = new RegistrationForm();
        form.setUsername("  nuevoUser  ");
        form.setNombre("  Nombre  ");
        form.setApellidos("Apellidos");
        form.setApodo("Apodo");
        form.setPassword("password123");
        form.setRetryPassword("password123");
        form.setEmail("nuevo@test.com");
        form.setNombreComercial("Comercial");
        form.setTelefono("600000000");
        form.setIdProvincia(15L);
        form.setTipoUsuario(tipo);
        return form;
    }

    private static Usuario admin(long id, String email) {
        Usuario admin = new Usuario();
        admin.setId(id);
        admin.setEmail(email);
        return admin;
    }

    private void givenRegistrableEmail() {
        when(usuarioRepository.existsUsuarioByEmail("nuevo@test.com")).thenReturn(false);
        when(provinciaRepository.findById(15L)).thenReturn(Optional.of(provincia));
        when(passwordEncoder.encode("password123")).thenReturn("{bcrypt}encoded");
    }

    private Usuario savedUser() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void saveRegistration_emailAlreadyExists_throwsAndHasNoSideEffects() {
        when(usuarioRepository.existsUsuarioByEmail("nuevo@test.com")).thenReturn(true);

        EmailYaExisteException ex = assertThrows(EmailYaExisteException.class,
                () -> userService.saveRegistration(form(TipoUsuarioEnum.ARTISTA)));

        assertThat(ex.getMessage()).isEqualTo("El email ya está registrado");
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, codigoVerificacionService, emailService, mensajeService, rolRepository);
    }

    @Test
    void saveRegistration_nonAgency_persistsPendingUserWithEncodedPasswordAndSendsCode() throws Exception {
        givenRegistrableEmail();
        when(usuarioRepository.findUsuariosByRolGeneralCodigo(RolEnum.ROL_ADMINISTRADOR.getCodigo())).thenReturn(List.of());

        Usuario returned = userService.saveRegistration(form(TipoUsuarioEnum.ARTISTA));

        Usuario saved = savedUser();
        assertThat(returned).isSameAs(saved);
        assertThat(saved.getUsername()).isEqualTo("nuevoUser");
        assertThat(saved.getNombre()).isEqualTo("Nombre");
        assertThat(saved.getApellidos()).isEqualTo("Apellidos");
        assertThat(saved.getPassword()).isEqualTo("{bcrypt}encoded");
        assertThat(saved.getEmail()).isEqualTo("nuevo@test.com");
        assertThat(saved.getNombreComercial()).isEqualTo("Comercial");
        assertThat(saved.getTelefono()).isEqualTo("600000000");
        assertThat(saved.getProvincia()).isSameAs(provincia);
        assertThat(saved.getTipoUsuario()).isEqualTo(TipoUsuarioEnum.ARTISTA);
        assertThat(saved.isActivo()).isTrue();
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.isValidado()).isFalse();
        assertThat(saved.getRolGeneral()).isNull();
        assertThat(saved.getFechaRegistro()).isNotNull();
        assertThat(UUID.fromString(saved.getActivateKey())).isNotNull();
        // The optional nickname of the form is not stored on registration
        assertThat(saved.getApodo()).isNull();

        verify(codigoVerificacionService).generarYEnviarCodigo("nuevo@test.com", EmailTemplateEnum.REGISTRO);
        verifyNoInteractions(rolRepository, emailService, mensajeService);
    }

    @Test
    void saveRegistration_agency_isValidatedWithAgentRole() throws Exception {
        givenRegistrableEmail();
        Rol rolAgente = new Rol();
        rolAgente.setCodigo(RolEnum.ROL_AGENTE.getCodigo());
        when(rolRepository.findRolByCodigo(RolEnum.ROL_AGENTE.getCodigo())).thenReturn(rolAgente);
        when(usuarioRepository.findUsuariosByRolGeneralCodigo(RolEnum.ROL_ADMINISTRADOR.getCodigo())).thenReturn(List.of());

        userService.saveRegistration(form(TipoUsuarioEnum.AGENCIA));

        Usuario saved = savedUser();
        assertThat(saved.isValidado()).isTrue();
        assertThat(saved.getRolGeneral()).isSameAs(rolAgente);
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getTipoUsuario()).isEqualTo(TipoUsuarioEnum.AGENCIA);
    }

    @Test
    void saveRegistration_representante_isNotValidatedNorGivenAgentRole() throws Exception {
        givenRegistrableEmail();
        when(usuarioRepository.findUsuariosByRolGeneralCodigo(RolEnum.ROL_ADMINISTRADOR.getCodigo())).thenReturn(List.of());

        userService.saveRegistration(form(TipoUsuarioEnum.REPRESENTANTE));

        Usuario saved = savedUser();
        assertThat(saved.isValidado()).isFalse();
        assertThat(saved.getRolGeneral()).isNull();
        verifyNoInteractions(rolRepository);
    }

    @Test
    void saveRegistration_notifiesEveryAdminByInternalMessageAndEmail() throws Exception {
        givenRegistrableEmail();
        Usuario admin1 = admin(1L, "admin1@test.com");
        Usuario admin2 = admin(2L, "admin2@test.com");
        when(usuarioRepository.findUsuariosByRolGeneralCodigo(RolEnum.ROL_ADMINISTRADOR.getCodigo()))
                .thenReturn(List.of(admin1, admin2));

        Usuario registrado = userService.saveRegistration(form(TipoUsuarioEnum.ARTISTA));

        ArgumentCaptor<Mensaje> mensajes = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeService).enviarMensaje(mensajes.capture(), eq(1L));
        verify(mensajeService).enviarMensaje(mensajes.capture(), eq(2L));
        assertThat(mensajes.getAllValues()).allSatisfy(mensaje -> {
            assertThat(mensaje.getUsuarioRemite()).isSameAs(registrado);
            assertThat(mensaje.getAsunto()).isEqualTo("Nuevo usuario");
            assertThat(mensaje.getMensaje()).isEqualTo("Nuevo usuario registrado Nombre Apellidos");
            assertThat(mensaje.getImagen()).isEqualTo("fa-users text-success");
        });
        assertThat(mensajes.getAllValues()).extracting(Mensaje::getUsuarioReceptor).containsExactly(admin1, admin2);
        verify(emailService).enviarMensajePorEmail("admin1@test.com", EmailTemplateEnum.VALIDAR_USUARIO);
        verify(emailService).enviarMensajePorEmail("admin2@test.com", EmailTemplateEnum.VALIDAR_USUARIO);
    }

    @Test
    void saveRegistration_verificationCodeEmailFails_userIsStillRegisteredAndAdminsNotified() throws Exception {
        givenRegistrableEmail();
        doThrow(new EnvioEmailException("mailgun down"))
                .when(codigoVerificacionService).generarYEnviarCodigo("nuevo@test.com", EmailTemplateEnum.REGISTRO);
        Usuario admin1 = admin(1L, "admin1@test.com");
        when(usuarioRepository.findUsuariosByRolGeneralCodigo(RolEnum.ROL_ADMINISTRADOR.getCodigo())).thenReturn(List.of(admin1));

        Usuario registrado = userService.saveRegistration(form(TipoUsuarioEnum.ARTISTA));

        assertThat(registrado).isSameAs(savedUser());
        verify(mensajeService).enviarMensaje(any(Mensaje.class), eq(1L));
        verify(emailService).enviarMensajePorEmail("admin1@test.com", EmailTemplateEnum.VALIDAR_USUARIO);
    }

    @Test
    void saveRegistration_adminEmailFails_continuesWithRemainingAdmins() throws Exception {
        givenRegistrableEmail();
        Usuario admin1 = admin(1L, "admin1@test.com");
        Usuario admin2 = admin(2L, "admin2@test.com");
        when(usuarioRepository.findUsuariosByRolGeneralCodigo(RolEnum.ROL_ADMINISTRADOR.getCodigo()))
                .thenReturn(List.of(admin1, admin2));
        doThrow(new EnvioEmailException("bounce"))
                .when(emailService).enviarMensajePorEmail("admin1@test.com", EmailTemplateEnum.VALIDAR_USUARIO);

        Usuario registrado = userService.saveRegistration(form(TipoUsuarioEnum.ARTISTA));

        assertThat(registrado).isSameAs(savedUser());
        verify(mensajeService).enviarMensaje(any(Mensaje.class), eq(2L));
        verify(emailService).enviarMensajePorEmail("admin2@test.com", EmailTemplateEnum.VALIDAR_USUARIO);
    }

    @Test
    void saveRegistration_unknownProvincia_failsWithoutSavingNorSendingCode() {
        when(usuarioRepository.existsUsuarioByEmail("nuevo@test.com")).thenReturn(false);
        when(provinciaRepository.findById(15L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.saveRegistration(form(TipoUsuarioEnum.ARTISTA)));

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(codigoVerificacionService, emailService, mensajeService);
    }

    @Test
    void usernameExists_onlyConsidersActiveUsers() {
        when(usuarioRepository.existsUsuarioActivoByUsername("nuevoUser")).thenReturn(true);
        when(usuarioRepository.existsUsuarioActivoByUsername("libre")).thenReturn(false);

        assertThat(userService.usernameExists("nuevoUser")).isTrue();
        assertThat(userService.usernameExists("libre")).isFalse();
        verify(usuarioRepository, never()).findByUsername(anyString());
    }

    @Test
    void activateUserByEmail_marksUserActiveAndEmailVerified() throws Exception {
        Usuario pendiente = new Usuario();
        pendiente.setEmail("nuevo@test.com");
        pendiente.setActivo(false);
        pendiente.setEmailVerified(false);
        when(usuarioRepository.findUsuarioByMail("nuevo@test.com")).thenReturn(Optional.of(pendiente));
        when(usuarioRepository.save(pendiente)).thenReturn(pendiente);

        Usuario activado = userService.activateUserByEmail("nuevo@test.com");

        assertThat(activado).isSameAs(pendiente);
        assertThat(pendiente.isActivo()).isTrue();
        assertThat(pendiente.isEmailVerified()).isTrue();
        verify(usuarioRepository).save(pendiente);
    }

    @Test
    void activateUserByEmail_unknownEmail_throwsWithoutSaving() {
        when(usuarioRepository.findUsuarioByMail("nadie@test.com")).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> userService.activateUserByEmail("nadie@test.com"));

        verify(usuarioRepository, never()).save(any());
    }
}
