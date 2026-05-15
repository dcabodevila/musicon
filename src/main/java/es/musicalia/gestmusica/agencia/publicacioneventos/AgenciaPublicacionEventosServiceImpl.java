package es.musicalia.gestmusica.agencia.publicacioneventos;

import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgenciaPublicacionEventosServiceImpl implements AgenciaPublicacionEventosService {

    private final AgenciaRepository agenciaRepository;
    private final AgenciaDecisionRepository decisionRepository;
    private final AgenciaPublicacionEventosArtistaRepository artistaRepository;

    public AgenciaPublicacionEventosServiceImpl(AgenciaRepository agenciaRepository,
                                                AgenciaDecisionRepository decisionRepository,
                                                AgenciaPublicacionEventosArtistaRepository artistaRepository) {
        this.agenciaRepository = agenciaRepository;
        this.decisionRepository = decisionRepository;
        this.artistaRepository = artistaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean debeMostrarModal(Long usuarioId) {
        return !findAgenciasPendientes(usuarioId).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgenciaPublicacionEventosModalItem> findAgenciasPendientesModal(Long usuarioId) {
        return findAgenciasPendientes(usuarioId).stream()
                .map(agencia -> new AgenciaPublicacionEventosModalItem(agencia.getId(), agencia.getNombre()))
                .toList();
    }

    @Override
    @Transactional
    public void activarPublicacionEventos(Long usuarioId) {
        List<Agencia> agenciasPendientes = findAgenciasPendientes(usuarioId);
        for (Agencia agencia : agenciasPendientes) {
            artistaRepository.activarPublicacionEventosPorAgencia(agencia.getId());

            AgenciaDecision decision = decisionRepository.findByAgenciaIdAndCodigoDecision(agencia.getId(), AgenciaDecisionCodigo.PUBLICACION_EVENTOS)
                    .orElseGet(AgenciaDecision::new);
            decision.setAgencia(agencia);
            decision.setCodigoDecision(AgenciaDecisionCodigo.PUBLICACION_EVENTOS);
            decision.setEstado(AgenciaPublicacionEventosEstado.ACTIVADO);
            decision.setFechaActivacion(LocalDateTime.now());
            decision.setFechaRechazo(null);
            decisionRepository.save(decision);
        }
    }

    @Override
    @Transactional
    public void rechazarPublicacionEventos(Long usuarioId) {
        List<Agencia> agenciasPendientes = findAgenciasPendientes(usuarioId);
        for (Agencia agencia : agenciasPendientes) {
            AgenciaDecision decision = decisionRepository.findByAgenciaIdAndCodigoDecision(agencia.getId(), AgenciaDecisionCodigo.PUBLICACION_EVENTOS)
                    .orElseGet(AgenciaDecision::new);
            decision.setAgencia(agencia);
            decision.setCodigoDecision(AgenciaDecisionCodigo.PUBLICACION_EVENTOS);
            decision.setEstado(AgenciaPublicacionEventosEstado.RECHAZADO);
            decision.setFechaRechazo(LocalDateTime.now());
            decision.setFechaActivacion(null);
            decisionRepository.save(decision);
        }
    }

    @Override
    @Transactional
    public void desactivarPublicacionEventos(Long usuarioId) {
        Agencia agencia = obtenerAgenciaPorUsuario(usuarioId);
        artistaRepository.desactivarPublicacionEventosPorAgencia(agencia.getId());

        AgenciaDecision decision = decisionRepository.findByAgenciaIdAndCodigoDecision(agencia.getId(), AgenciaDecisionCodigo.PUBLICACION_EVENTOS)
                .orElseGet(AgenciaDecision::new);
        decision.setAgencia(agencia);
        decision.setCodigoDecision(AgenciaDecisionCodigo.PUBLICACION_EVENTOS);
        decision.setEstado(AgenciaPublicacionEventosEstado.RECHAZADO);
        decision.setFechaRechazo(LocalDateTime.now());
        decision.setFechaActivacion(null);
        decisionRepository.save(decision);
    }

    @Override
    @Transactional(readOnly = true)
    public AgenciaDecision findDecisionByAgenciaId(Long agenciaId) {
        return decisionRepository.findByAgenciaIdAndCodigoDecision(agenciaId, AgenciaDecisionCodigo.PUBLICACION_EVENTOS).orElse(null);
    }

    private Agencia obtenerAgenciaPorUsuario(Long usuarioId) {
        return agenciaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("No existe agencia para el usuario autenticado"));
    }

    private List<Agencia> findAgenciasPendientes(Long usuarioId) {
        return agenciaRepository.findAllByUsuarioIdAndActivoTrue(usuarioId).stream()
                .filter(agencia -> !decisionRepository.existsByAgenciaIdAndCodigoDecision(agencia.getId(), AgenciaDecisionCodigo.PUBLICACION_EVENTOS))
                .filter(agencia -> !artistaRepository.existsByAgenciaIdAndActivoTrueAndPublicarEventosTrue(agencia.getId()))
                .toList();
    }
}
