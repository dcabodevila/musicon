package es.musicalia.gestmusica.agencia.publicacioneventos;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgenciaPublicacionEventosServiceImplTest {

    @Mock
    private AgenciaRepository agenciaRepository;
    @Mock
    private AgenciaDecisionRepository decisionRepository;
    @Mock
    private AgenciaPublicacionEventosArtistaRepository artistaRepository;

    @InjectMocks
    private AgenciaPublicacionEventosServiceImpl service;

    @Test
    void debeMostrarModal_cuandoUsuarioEsTitularAgenciaYNoTieneDecision() {
        when(agenciaRepository.findAllByUsuarioIdAndActivoTrue(10L)).thenReturn(List.of(agencia(7L)));
        when(decisionRepository.existsByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(false);
        when(artistaRepository.existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(7L)).thenReturn(false);

        boolean mostrar = service.debeMostrarModal(10L);

        assertThat(mostrar).isTrue();
    }

    @Test
    void debeMostrarModal_noMuestraSiYaHayArtistaConPublicarEventosActivo() {
        when(agenciaRepository.findAllByUsuarioIdAndActivoTrue(10L)).thenReturn(List.of(agencia(7L)));
        when(decisionRepository.existsByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(false);
        when(artistaRepository.existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(7L)).thenReturn(true);

        boolean mostrar = service.debeMostrarModal(10L);

        assertThat(mostrar).isFalse();
    }

    @Test
    void debeMostrarModal_noMuestraSiNoHayAgencia() {
        when(agenciaRepository.findAllByUsuarioIdAndActivoTrue(10L)).thenReturn(List.of());

        boolean mostrar = service.debeMostrarModal(10L);

        assertThat(mostrar).isFalse();
    }

    @Test
    void activarPublicacionEventos_activaArtistasYGuardaDecision() {
        Agencia agencia = agencia(7L);
        AgenciaDecision decision = new AgenciaDecision();
        when(agenciaRepository.findAllByUsuarioIdAndActivoTrue(10L)).thenReturn(List.of(agencia));
        when(decisionRepository.findByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(Optional.of(decision));
        when(decisionRepository.existsByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(false);
        when(artistaRepository.existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(7L)).thenReturn(false);

        service.activarPublicacionEventos(10L);

        verify(artistaRepository).activarPublicacionEventosPorAgencia(7L);
        verify(decisionRepository).save(decision);
        assertThat(decision.getEstado()).isEqualTo(AgenciaPublicacionEventosEstado.ACTIVADO);
        assertThat(decision.getCodigoDecision()).isEqualTo(AgenciaDecisionCodigo.PUBLICACION_EVENTOS);
        assertThat(decision.getFechaActivacion()).isNotNull();
        assertThat(decision.getFechaRechazo()).isNull();
    }

    @Test
    void rechazarPublicacionEventos_guardaFechaRechazo() {
        Agencia agencia = agencia(7L);
        AgenciaDecision decision = new AgenciaDecision();
        decision.setFechaActivacion(LocalDateTime.now().minusDays(1));

        when(agenciaRepository.findAllByUsuarioIdAndActivoTrue(10L)).thenReturn(List.of(agencia));
        when(decisionRepository.findByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(Optional.of(decision));
        when(decisionRepository.existsByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(false);
        when(artistaRepository.existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(7L)).thenReturn(false);

        service.rechazarPublicacionEventos(10L);

        verify(decisionRepository).save(decision);
        assertThat(decision.getEstado()).isEqualTo(AgenciaPublicacionEventosEstado.RECHAZADO);
        assertThat(decision.getCodigoDecision()).isEqualTo(AgenciaDecisionCodigo.PUBLICACION_EVENTOS);
        assertThat(decision.getFechaRechazo()).isNotNull();
        assertThat(decision.getFechaActivacion()).isNull();
    }

    @Test
    void desactivarPublicacionEventos_desactivaArtistasYGuardaEstadoRechazado() {
        Agencia agencia = agencia(7L);
        AgenciaDecision decision = new AgenciaDecision();
        decision.setEstado(AgenciaPublicacionEventosEstado.ACTIVADO);
        decision.setFechaActivacion(LocalDateTime.now().minusDays(2));

        when(agenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(agencia));
        when(decisionRepository.findByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(Optional.of(decision));

        service.desactivarPublicacionEventos(10L);

        verify(artistaRepository).desactivarPublicacionEventosPorAgencia(7L);
        verify(decisionRepository).save(decision);
        assertThat(decision.getEstado()).isEqualTo(AgenciaPublicacionEventosEstado.RECHAZADO);
        assertThat(decision.getCodigoDecision()).isEqualTo(AgenciaDecisionCodigo.PUBLICACION_EVENTOS);
        assertThat(decision.getFechaRechazo()).isNotNull();
        assertThat(decision.getFechaActivacion()).isNull();
    }

    @Test
    void activarPublicacionEventos_aplicaSobreTodasLasAgenciasPendientes() {
        Agencia agenciaUno = agencia(7L);
        Agencia agenciaDos = agencia(8L);
        AgenciaDecision decisionUno = new AgenciaDecision();
        AgenciaDecision decisionDos = new AgenciaDecision();

        when(agenciaRepository.findAllByUsuarioIdAndActivoTrue(10L)).thenReturn(List.of(agenciaUno, agenciaDos));
        when(decisionRepository.existsByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(false);
        when(artistaRepository.existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(7L)).thenReturn(false);
        when(decisionRepository.existsByAgenciaIdAndCodigoDecision(8L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(false);
        when(artistaRepository.existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(8L)).thenReturn(false);
        when(decisionRepository.findByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(Optional.of(decisionUno));
        when(decisionRepository.findByAgenciaIdAndCodigoDecision(8L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(Optional.of(decisionDos));

        service.activarPublicacionEventos(10L);

        verify(artistaRepository).activarPublicacionEventosPorAgencia(7L);
        verify(artistaRepository).activarPublicacionEventosPorAgencia(8L);
        verify(decisionRepository).save(decisionUno);
        verify(decisionRepository).save(decisionDos);
    }

    @Test
    void listarAgenciasModal_debeIncluirSoloAgenciasPendientes() {
        Agencia agenciaPendiente = agencia(7L);
        agenciaPendiente.setNombre("Agencia Norte");
        Agencia agenciaConDecision = agencia(8L);
        Agencia agenciaConArtistaActivo = agencia(9L);

        when(agenciaRepository.findAllByUsuarioIdAndActivoTrue(10L)).thenReturn(List.of(agenciaPendiente, agenciaConDecision, agenciaConArtistaActivo));
        when(decisionRepository.existsByAgenciaIdAndCodigoDecision(7L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(false);
        when(artistaRepository.existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(7L)).thenReturn(false);
        when(decisionRepository.existsByAgenciaIdAndCodigoDecision(8L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(true);
        when(decisionRepository.existsByAgenciaIdAndCodigoDecision(9L, AgenciaDecisionCodigo.PUBLICACION_EVENTOS)).thenReturn(false);
        when(artistaRepository.existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(9L)).thenReturn(true);

        List<AgenciaPublicacionEventosModalItem> items = service.findAgenciasPendientesModal(10L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).idAgencia()).isEqualTo(7L);
        assertThat(items.get(0).nombreAgencia()).isEqualTo("Agencia Norte");
    }

    private Agencia agencia(Long id) {
        Agencia agencia = new Agencia();
        agencia.setId(id);
        return agencia;
    }
}
