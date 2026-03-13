package es.musicalia.gestmusica.orquestasdegalicia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OdgSincronizacionTrackingRepository extends JpaRepository<OdgSincronizacionTracking, Long> {
}
