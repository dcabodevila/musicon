package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.agencia.AgenciaService;
import es.musicalia.gestmusica.artista.ArtistaService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import es.musicalia.gestmusica.observabilidad.FunctionalEventTracker;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcupacionControllerAccesoAgenciaTest {

    @Mock
    private OcupacionService ocupacionService;
    @Mock
    private AgenciaService agenciaService;
    @Mock
    private ArtistaService artistaService;
    @Mock
    private LocalizacionService localizacionService;
    @Mock
    private UserService userService;
    @Mock
    private FunctionalEventTracker functionalEventTracker;

    @InjectMocks
    private OcupacionController controller;

    private CustomAuthenticatedUser userConAccesoAgencia10;
    private CustomAuthenticatedUser userSinAccesoAgencia;

    @BeforeEach
    void setUp() {
        userConAccesoAgencia10 = authenticatedUser(Map.of(10L, Set.of("VER_OCUPACIONES")));
        userSinAccesoAgencia = authenticatedUser(Map.of(20L, Set.of("VER_OCUPACIONES")));
    }

    @Test
    void getListadoOcupacionesData_agenciaNoAccesible_lanzaAccessDenied() {
        assertThatThrownBy(() -> controller.getListadoOcupacionesData(
                userSinAccesoAgencia, 1, 0, 10, "10", null, null, null, null, 1, "desc", false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getListadoOcupacionesData_agenciaAccesible_invocaServicio() {
        lenient().when(ocupacionService.findOcupacionesByArtistasListAndDatesActivoPaginado(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Map<String, Object> response = controller.getListadoOcupacionesData(
                userConAccesoAgencia10, 1, 0, 10, "10", null, null, null, null, 1, "desc", false);

        assertThat(response.get("error")).isNull();
        verify(ocupacionService, org.mockito.Mockito.times(2)).findOcupacionesByArtistasListAndDatesActivoPaginado(any(), any(), any(), any());
    }

    @Test
    void exportarOcupacionesExcel_agenciaNoAccesible_lanzaAccessDenied() {
        OcupacionListFilterDto filtro = OcupacionListFilterDto.builder().idAgencia(10L).build();

        assertThatThrownBy(() -> controller.exportarOcupacionesExcel(userSinAccesoAgencia, filtro))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void exportarOcupacionesPDF_agenciaNoAccesible_lanzaAccessDenied() {
        OcupacionListFilterDto filtro = OcupacionListFilterDto.builder().idAgencia(10L).build();

        assertThatThrownBy(() -> controller.exportarOcupacionesPDF(userSinAccesoAgencia, filtro))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getListadoOcupaciones_flashFilterAgenciaNoAccesible_lanzaAccessDenied() {
        Model model = new BindingAwareModelMap();
        model.addAttribute("ocupacionListFilterDto", OcupacionListFilterDto.builder().idAgencia(10L).build());

        assertThatThrownBy(() -> controller.getListadoOcupaciones(userSinAccesoAgencia, model))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getListadoOcupaciones_flashFilterAgenciaAccesible_noLanza() {
        Model model = new BindingAwareModelMap();
        model.addAttribute("ocupacionListFilterDto", OcupacionListFilterDto.builder().idAgencia(10L).build());

        String view = controller.getListadoOcupaciones(userConAccesoAgencia10, model);

        assertThat(view).isEqualTo("ocupaciones");
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
