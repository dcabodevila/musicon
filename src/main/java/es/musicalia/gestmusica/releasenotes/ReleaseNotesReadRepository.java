package es.musicalia.gestmusica.releasenotes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReleaseNotesReadRepository extends JpaRepository<ReleaseNotesRead, Long> {
    
    boolean existsByUsuarioIdAndVersion(Long usuarioId, String version);
    
    Optional<ReleaseNotesRead> findByUsuarioIdAndVersion(Long usuarioId, String version);
}
