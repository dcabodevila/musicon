package es.musicalia.gestmusicalegacy.ocupacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OcupacionLegacyRepository extends JpaRepository<OcupacionLegacy, Integer> {

    /**
     * Obtiene todas las ocupaciones a partir de una fecha específica (inclusive)
     * Ordenadas por fecha ascendente
     */
    @Query("SELECT o FROM OcupacionLegacy o WHERE o.fecha >= :fechaDesde ORDER BY o.fecha ASC")
    Optional<List<OcupacionLegacy>> findAllFromDate(@Param("fechaDesde") LocalDate fechaDesde);

}