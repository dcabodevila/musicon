package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.rol.RolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
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
    }

    @Test
    void guardarUsuario_debePermitirGuardarSiEmailDisponible() throws Exception {
        UsuarioEdicionDTO dto = crearDto("nuevo@correo.com");
        Usuario usuario = crearUsuario(1L, "actual@correo.com");
        Provincia provincia = crearProvincia(10L);

        when(usuarioRepository.existsByEmailAndIdNot("nuevo@correo.com", 1L)).thenReturn(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(provinciaRepository.findById(10L)).thenReturn(Optional.of(provincia));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario result = userService.guardarUsuario(dto, null);

        assertEquals("nuevo@correo.com", result.getEmail());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void guardarUsuario_debePermitirGuardarSiConservaSuMismoEmail() throws Exception {
        UsuarioEdicionDTO dto = crearDto("mismo@correo.com");
        Usuario usuario = crearUsuario(1L, "mismo@correo.com");
        Provincia provincia = crearProvincia(10L);

        when(usuarioRepository.existsByEmailAndIdNot("mismo@correo.com", 1L)).thenReturn(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(provinciaRepository.findById(10L)).thenReturn(Optional.of(provincia));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario result = userService.guardarUsuario(dto, null);

        assertEquals("mismo@correo.com", result.getEmail());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void guardarUsuario_debeRechazarSiEmailPerteneceAOtroUsuario() {
        UsuarioEdicionDTO dto = crearDto("duplicado@correo.com");

        when(usuarioRepository.existsByEmailAndIdNot("duplicado@correo.com", 1L)).thenReturn(true);

        assertThrows(EmailYaExisteException.class, () -> userService.guardarUsuario(dto, null));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void guardarUsuario_debeTraducirConflictoTardioDeUnicidad() {
        UsuarioEdicionDTO dto = crearDto("colision@correo.com");
        Usuario usuario = crearUsuario(1L, "actual@correo.com");
        Provincia provincia = crearProvincia(10L);

        when(usuarioRepository.existsByEmailAndIdNot("colision@correo.com", 1L)).thenReturn(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(provinciaRepository.findById(10L)).thenReturn(Optional.of(provincia));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenThrow(new DataIntegrityViolationException("constraint uk_usuario_email"));

        assertThrows(EmailYaExisteException.class, () -> userService.guardarUsuario(dto, null));
    }

    private UsuarioEdicionDTO crearDto(String email) {
        UsuarioEdicionDTO dto = new UsuarioEdicionDTO();
        dto.setId(1L);
        dto.setUsername("usuario");
        dto.setNombre("Nombre");
        dto.setApellidos("Apellido");
        dto.setEmail(email);
        dto.setTelefono("600000000");
        dto.setNombreComercial("Comercial");
        dto.setIdProvincia(10L);
        return dto;
    }

    private Usuario crearUsuario(Long id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        return usuario;
    }

    private Provincia crearProvincia(Long id) {
        Provincia provincia = new Provincia();
        provincia.setId(id);
        provincia.setNombre("A Coruña");
        return provincia;
    }
}
