package es.musicalia.gestmusica.orquestasdegalicia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OdgSincronizacionTrackingRepository extends JpaRepository<OdgSincronizacionTracking, Long> {

    @Query("SELECT MAX(t.fechaEjecucion) FROM OdgSincronizacionTracking t " +
           "WHERE t.ocupacionId = :ocupacionId AND t.resultado = 'ERROR' AND t.messageType = 'danger'")
    Optional<LocalDateTime> findUltimoErrorValidacion(@Param("ocupacionId") Long ocupacionId);
}
