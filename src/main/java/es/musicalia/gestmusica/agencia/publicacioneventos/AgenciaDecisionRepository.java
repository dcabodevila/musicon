package es.musicalia.gestmusica.agencia.publicacioneventos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgenciaDecisionRepository extends JpaRepository<AgenciaDecision, Long> {

    boolean existsByAgenciaIdAndCodigoDecision(Long agenciaId, AgenciaDecisionCodigo codigoDecision);

    Optional<AgenciaDecision> findByAgenciaIdAndCodigoDecision(Long agenciaId, AgenciaDecisionCodigo codigoDecision);
}
