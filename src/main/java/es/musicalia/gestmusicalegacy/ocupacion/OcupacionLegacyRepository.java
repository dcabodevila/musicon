package es.musicalia.gestmusicalegacy.ocupacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface OcupacionLegacyRepository extends JpaRepository<OcupacionLegacy, Integer> {

    /**
     * Obtiene todas las ocupaciones a partir de una fecha específica (inclusive)
     * Ordenadas por fecha ascendente
     */
    @Query("SELECT o FROM OcupacionLegacy o WHERE o.fecha >= :fechaDesde AND o.fechaModificacion >= :fechaModificacionDesde ")
    Optional<List<OcupacionLegacy>> findOcupacionesModificadasFromDate(@Param("fechaDesde") LocalDate fechaDesde, @Param("fechaModificacionDesde") LocalDateTime fechaModificacionDesde);

    @Query("SELECT o FROM OcupacionLegacy o WHERE o.fecha >= :fechaDesde AND o.idArtista = :idArtistaLegacy AND o.fechaModificacion >= :fechaModificacionDesde ")
    Optional<List<OcupacionLegacy>> findOcupacionesArtistaModificadasFromDate(@Param("fechaDesde") LocalDate fechaDesde, @Param("fechaModificacionDesde") LocalDateTime fechaModificacionDesde, @Param("idArtistaLegacy") Integer idARtistaLegacy);

    @Query("SELECT o.id FROM OcupacionLegacy o WHERE o.fecha >= :fechaDesde ")
    Optional<Set<Integer>> findIdsOcupacionesFromDate(@Param("fechaDesde") LocalDate fechaDesde);

}