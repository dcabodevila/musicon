package es.musicalia.gestmusica.ajustes;

import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AjustesControllerTest {

    @Mock
    private LocalizacionService localizacionService;
    @Mock
    private AgenciaService agenciaService;
    @Mock
    private ArtistaService artistaService;
    @Mock
    private AjustesService ajustesService;

    @InjectMocks
    private AjustesController ajustesController;

    private CustomAuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombre("Test");
        usuario.setApellidos("User");
        usuario.setPassword("secret");
        usuario.setValidado(true);

        authenticatedUser = new CustomAuthenticatedUser(
                usuario,
                true,
                true,
                true,
                true,
                List.of(),
                java.util.Map.of(),
                java.util.Map.of()
        );
    }

    @Test
    void guardarOpcionesListado_devuelveExito() {
        ResponseEntity<?> response = ajustesController.guardarOpcionesListado(authenticatedUser, 7L, new AjustesDto());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isInstanceOf(es.musicalia.gestmusica.util.DefaultResponseBody.class);
        es.musicalia.gestmusica.util.DefaultResponseBody body =
                (es.musicalia.gestmusica.util.DefaultResponseBody) response.getBody();
        assertThat(body.isSuccess()).isTrue();
        assertThat(body.getMessage()).isEqualTo("Configuración guardada correctamente");
        verify(ajustesService).guardarOpcionesListado(eq(7L), any(AjustesDto.class), eq(authenticatedUser.getUsuario()));
    }

    @Test
    void guardarOpcionesListado_devuelveErrorSiFallaElServicio() {
        doThrow(new IllegalStateException("boom"))
                .when(ajustesService)
                .guardarOpcionesListado(eq(8L), any(AjustesDto.class), eq(authenticatedUser.getUsuario()));

        ResponseEntity<?> response = ajustesController.guardarOpcionesListado(authenticatedUser, 8L, new AjustesDto());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        es.musicalia.gestmusica.util.DefaultResponseBody body =
                (es.musicalia.gestmusica.util.DefaultResponseBody) response.getBody();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getMessage()).isEqualTo("Error guardando configuración");
    }
}
