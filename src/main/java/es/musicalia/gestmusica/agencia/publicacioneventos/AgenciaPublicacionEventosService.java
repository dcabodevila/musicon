package es.musicalia.gestmusica.agencia.publicacioneventos;

import java.util.List;

public interface AgenciaPublicacionEventosService {

    boolean debeMostrarModal(Long usuarioId);

    List<AgenciaPublicacionEventosModalItem> findAgenciasPendientesModal(Long usuarioId);

    void activarPublicacionEventos(Long usuarioId);

    void rechazarPublicacionEventos(Long usuarioId);

    void desactivarPublicacionEventos(Long usuarioId);

    AgenciaDecision findDecisionByAgenciaId(Long agenciaId);
}
