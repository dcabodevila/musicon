package es.musicalia.gestmusica.ajustes;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.localizacion.Ccaa;
import es.musicalia.gestmusica.localizacion.CcaaRepository;
import es.musicalia.gestmusica.tipoartista.TipoArtista;
import es.musicalia.gestmusica.tipoartista.TipoArtistaRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AjustesServiceImplTest {

    @Mock
    private AjustesRepository ajustesRepository;
    @Mock
    private TipoArtistaRepository tipoArtistaRepository;
    @Mock
    private AgenciaRepository agenciaRepository;
    @Mock
    private CcaaRepository ccaaRepository;

    @InjectMocks
    private AjustesServiceImpl ajustesService;

    @Test
    @DisplayName("saveAjustesDto limpia relaciones many-to-many cuando el formulario las vacía")
    void saveAjustesDtoLimpiaColeccionesCuandoNoHaySeleccion() {
        long ajusteId = 11L;

        Ajustes ajustesExistentes = new Ajustes();
        ajustesExistentes.setId(ajusteId);
        ajustesExistentes.setTipoArtistas(new HashSet<>(Set.of(crearTipoArtista(1L))));
        ajustesExistentes.setAgencias(new HashSet<>(Set.of(crearAgencia(2L))));
        ajustesExistentes.setCcaa(new HashSet<>(Set.of(crearCcaa(3L))));

        AjustesDto dto = new AjustesDto();
        dto.setId(ajusteId);
        dto.setNombre("Ajuste test");
        dto.setIdsTipoArtista(List.of());
        dto.setIdsAgencias(List.of());
        dto.setIdsComunidades(List.of());

        Usuario usuario = new Usuario();
        usuario.setId(99L);

        when(ajustesRepository.findById(ajusteId)).thenReturn(Optional.of(ajustesExistentes));
        when(ajustesRepository.save(any(Ajustes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ajustes ajustesGuardados = ajustesService.saveAjustesDto(dto, usuario);

        assertThat(ajustesGuardados.getTipoArtistas()).isEmpty();
        assertThat(ajustesGuardados.getAgencias()).isEmpty();
        assertThat(ajustesGuardados.getCcaa()).isEmpty();
    }

    @Test
    @DisplayName("guardarOpcionesListado conserva nombre y predeterminado y actualiza solo relaciones")
    void guardarOpcionesListadoConservaMetadatosYActualizaColecciones() {
        long ajusteId = 11L;
        long usuarioId = 99L;

        Ajustes ajustesExistentes = new Ajustes();
        ajustesExistentes.setId(ajusteId);
        ajustesExistentes.setNombre("Ajuste original");
        ajustesExistentes.setPredeterminado(true);
        ajustesExistentes.setTipoArtistas(new HashSet<>(Set.of(crearTipoArtista(1L))));
        ajustesExistentes.setAgencias(new HashSet<>(Set.of(crearAgencia(2L))));
        ajustesExistentes.setCcaa(new HashSet<>(Set.of(crearCcaa(3L))));

        TipoArtista tipoArtistaNuevo = crearTipoArtista(10L);
        Agencia agenciaNueva = crearAgencia(20L);
        Ccaa ccaaNueva = crearCcaa(30L);

        AjustesDto dto = new AjustesDto();
        dto.setNombre("Nombre que no debe aplicarse");
        dto.setPredeterminado(false);
        dto.setIdsTipoArtista(List.of(tipoArtistaNuevo.getId()));
        dto.setIdsAgencias(List.of(agenciaNueva.getId()));
        dto.setIdsComunidades(List.of(ccaaNueva.getId()));

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(ajustesRepository.findByIdAndUsuarioId(ajusteId, usuarioId)).thenReturn(Optional.of(ajustesExistentes));
        when(tipoArtistaRepository.findAllById(dto.getIdsTipoArtista())).thenReturn(List.of(tipoArtistaNuevo));
        when(agenciaRepository.findAllById(dto.getIdsAgencias())).thenReturn(List.of(agenciaNueva));
        when(ccaaRepository.findAllById(dto.getIdsComunidades())).thenReturn(List.of(ccaaNueva));
        when(ajustesRepository.save(any(Ajustes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ajustes ajustesGuardados = ajustesService.guardarOpcionesListado(ajusteId, dto, usuario);

        assertThat(ajustesGuardados.getNombre()).isEqualTo("Ajuste original");
        assertThat(ajustesGuardados.isPredeterminado()).isTrue();
        assertThat(ajustesGuardados.getTipoArtistas()).extracting(TipoArtista::getId).containsExactly(tipoArtistaNuevo.getId());
        assertThat(ajustesGuardados.getAgencias()).extracting(Agencia::getId).containsExactly(agenciaNueva.getId());
        assertThat(ajustesGuardados.getCcaa()).extracting(Ccaa::getId).containsExactly(ccaaNueva.getId());
    }

    private TipoArtista crearTipoArtista(Long id) {
        TipoArtista tipoArtista = new TipoArtista();
        tipoArtista.setId(id);
        return tipoArtista;
    }

    private Agencia crearAgencia(Long id) {
        Agencia agencia = new Agencia();
        agencia.setId(id);
        return agencia;
    }

    private Ccaa crearCcaa(Long id) {
        Ccaa ccaa = new Ccaa();
        ccaa.setId(id);
        return ccaa;
    }
}
