package es.musicalia.gestmusica.listado;

import es.musicalia.gestmusica.info.InfoCcaaMetricRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ListadoRepository extends JpaRepository<Listado, Long>, JpaSpecificationExecutor<Listado> {

    long countByActivoTrueAndFechaCreacionGreaterThanEqual(LocalDateTime desde);

    @Query("""
        SELECT new es.musicalia.gestmusica.info.InfoCcaaMetricRecord(c.nombre, 0, COUNT(l))
        FROM Listado l
        JOIN l.provincia p
        JOIN p.ccaa c
        WHERE l.activo = true
        AND l.fechaCreacion >= :desde
        GROUP BY c.nombre
        """)
    List<InfoCcaaMetricRecord> countPresupuestosActivosPorCcaaDesde(@Param("desde") LocalDateTime desde);

}
