package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.SecurityService;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.incremento.IncrementoService;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.tarifa.TarifaService;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistaControllerAccesoAgenciaTest {

    @Mock
    private UserService userService;
    @Mock
    private ArtistaService artistaService;
    @Mock
    private FileService fileService;
    @Mock
    private AgenciaService agenciaService;
    @Mock
    private LocalizacionService localizacionService;
    @Mock
    private IncrementoService incrementoService;
    @Mock
    private OcupacionService ocupacionService;
    @Mock
    private SecurityService securityService;
    @Mock
    private TarifaService tarifaService;
    @Mock
    private FunctionalEventTracker functionalEventTracker;

    @InjectMocks
    private ArtistaController controller;

    private CustomAuthenticatedUser userConAccesoAgencia10;
    private CustomAuthenticatedUser userSinAccesoAgencia;

    @BeforeEach
    void setUp() {
        userConAccesoAgencia10 = authenticatedUser(Map.of(10L, Set.of("VER_OCUPACIONES")));
        userSinAccesoAgencia = authenticatedUser(Map.of(20L, Set.of("VER_OCUPACIONES")));
    }

    @Test
    void obtenerArtistasPorAgencia_agenciaNoAccesible_lanzaAccessDenied() {
        assertThatThrownBy(() -> controller.obtenerArtistasPorAgencia(userSinAccesoAgencia, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerArtistasPorAgencia_agenciaAccesible_invocaServicio() {
        when(artistaService.findArtistasRecordByIdAgencia(10L)).thenReturn(List.of());

        ResponseEntity<List<ArtistaRecord>> response = controller.obtenerArtistasPorAgencia(userConAccesoAgencia10, 10L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(artistaService).findArtistasRecordByIdAgencia(10L);
    }

    private CustomAuthenticatedUser authenticatedUser(Map<Long, Set<String>> mapPermisosAgencia) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Usuario");
        usuario.setApellidos("Prueba");
        usuario.setPassword("secret");

        return new CustomAuthenticatedUser(usuario, true, true, true, true, List.of(), Map.of(), mapPermisosAgencia);
    }
}
