package es.musicalia.gestmusica.comunicacion;

import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.usuario.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComunicacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ComunicacionChannel comunicacionChannel;

    @Mock
    private MensajeService mensajeService;

    @Mock
    private UserService userService;

    private ComunicacionService comunicacionService;

    @BeforeEach
    void setUp() {
        comunicacionService = new ComunicacionService(usuarioRepository, comunicacionChannel, mensajeService, userService);
    }

    @Test
    void enviarComunicacion_conUsuariosValidos_debeEnviarATodos() throws Exception {
        // Arrange
        Usuario user1 = crearUsuario(1L, "user1@test.com", false);
        Usuario user2 = crearUsuario(2L, "user2@test.com", false);
        when(usuarioRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(user1, user2));

        String asunto = "Asunto de prueba";
        String htmlBody = "<p>Mensaje</p>";
        String textoPlano = "Mensaje";

        // Act
        ComunicacionResult result = comunicacionService.enviarComunicacion(List.of(1L, 2L), asunto, htmlBody, textoPlano);

        // Assert
        assertThat(result.enviados()).isEqualTo(2);
        assertThat(result.fallidos()).isEqualTo(0);
        assertThat(result.excluidosBaja()).isEqualTo(0);
        verify(comunicacionChannel).enviar("user1@test.com", asunto, htmlBody);
        verify(comunicacionChannel).enviar("user2@test.com", asunto, htmlBody);
    }

    @Test
    void enviarComunicacion_conUsuarioDeBaja_debeExcluirYContar() throws Exception {
        // Arrange
        Usuario user1 = crearUsuario(1L, "user1@test.com", false);
        Usuario userBaja = crearUsuario(2L, "userbaja@test.com", true);
        when(usuarioRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(user1, userBaja));

        // Act
        ComunicacionResult result = comunicacionService.enviarComunicacion(List.of(1L, 2L), "Asunto", "<p>Msg</p>", "Msg");

        // Assert
        assertThat(result.enviados()).isEqualTo(1);
        assertThat(result.excluidosBaja()).isEqualTo(1);
        assertThat(result.fallidos()).isEqualTo(0);
        verify(comunicacionChannel).enviar("user1@test.com", "Asunto", "<p>Msg</p>");
        verify(comunicacionChannel, never()).enviar(eq("userbaja@test.com"), any(), any());
    }

    @Test
    void enviarComunicacion_cuandoCanalFalla_debeContarComoFallidoYContinuar() throws Exception {
        // Arrange
        Usuario user1 = crearUsuario(1L, "user1@test.com", false);
        Usuario user2 = crearUsuario(2L, "user2@test.com", false);
        when(usuarioRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(user1, user2));

        doThrow(new RuntimeException("Error de red"))
                .when(comunicacionChannel).enviar("user1@test.com", "Asunto", "<p>Msg</p>");

        // Act
        ComunicacionResult result = comunicacionService.enviarComunicacion(List.of(1L, 2L), "Asunto", "<p>Msg</p>", "Msg");

        // Assert
        assertThat(result.enviados()).isEqualTo(1);
        assertThat(result.fallidos()).isEqualTo(1);
        assertThat(result.excluidosBaja()).isEqualTo(0);
        verify(comunicacionChannel).enviar("user2@test.com", "Asunto", "<p>Msg</p>");
    }

    @Test
    void enviarComunicacion_conListaVacia_debeRetornarCeros() {
        // Act
        ComunicacionResult result = comunicacionService.enviarComunicacion(List.of(), "Asunto", "<p>Msg</p>", "Msg");

        // Assert
        assertThat(result.enviados()).isEqualTo(0);
        assertThat(result.fallidos()).isEqualTo(0);
        assertThat(result.excluidosBaja()).isEqualTo(0);
        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(comunicacionChannel);
    }

    @Test
    void enviarComunicacion_conUsuarioSinEmail_debeContarComoFallido() throws Exception {
        // Arrange
        Usuario userSinEmail = crearUsuario(1L, null, false);
        when(usuarioRepository.findAllById(List.of(1L))).thenReturn(List.of(userSinEmail));

        // Act
        ComunicacionResult result = comunicacionService.enviarComunicacion(List.of(1L), "Asunto", "<p>Msg</p>", "Msg");

        // Assert
        assertThat(result.enviados()).isEqualTo(0);
        assertThat(result.fallidos()).isEqualTo(1);
        verify(comunicacionChannel, never()).enviar(any(), any(), any());
    }

    @Test
    void enviarComunicacion_conMultiplesErrores_debeContarCorrectamente() throws Exception {
        // Arrange
        Usuario user1 = crearUsuario(1L, "user1@test.com", false);  // falla
        Usuario userBaja = crearUsuario(2L, "userbaja@test.com", true);  // excluido
        Usuario user2 = crearUsuario(3L, "user2@test.com", false);  // ok
        Usuario user3 = crearUsuario(4L, "user3@test.com", false);  // falla

        when(usuarioRepository.findAllById(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(user1, userBaja, user2, user3));

        // Stub cualquier llamada para lanzar excepción, excepto user2
        doThrow(new RuntimeException("Error"))
                .when(comunicacionChannel).enviar(org.mockito.ArgumentMatchers.argThat(
                        email -> !email.equals("user2@test.com")), eq("Asunto"), eq("<p>Msg</p>"));

        // Act
        ComunicacionResult result = comunicacionService.enviarComunicacion(
                List.of(1L, 2L, 3L, 4L), "Asunto", "<p>Msg</p>", "Msg");

        // Assert
        assertThat(result.enviados()).isEqualTo(1);
        assertThat(result.fallidos()).isEqualTo(2);
        assertThat(result.excluidosBaja()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(4);
    }

    private Usuario crearUsuario(Long id, String email, boolean emailBaja) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setEmailBaja(emailBaja);
        usuario.setActivo(true);
        return usuario;
    }
}
