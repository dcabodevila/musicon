package es.musicalia.gestmusica.tarifa;

import es.musicalia.gestmusica.actividad.ActividadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    @Query(value ="select new es.musicalia.gestmusica.tarifa.TarifaDto(t.id, t.fecha, t.artista.id, cast(TRUNC(t.importe,0) as string), true) FROM Tarifa t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo")
    Optional<List<TarifaDto>> findTarifasDtoByArtistaIdAndDates(long idArtista, LocalDateTime start, LocalDateTime end);

    @Query(value ="select t FROM Tarifa t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo order by t.id desc")
    List<Tarifa> findTarifasByArtistaIdAndDates(long idArtista, LocalDateTime start, LocalDateTime end);


	@Query("select new es.musicalia.gestmusica.actividad.ActividadRecord(t.artista.id, t.artista.agencia.nombre, t.artista.nombre, " +
		   "GREATEST(max(t.fechaCreacion), max(t.fechaModificacion)), " +
		   "sum(case when GREATEST(t.fechaCreacion, coalesce(t.fechaModificacion, t.fechaCreacion)) >= :fechaLimite then 1 else 0 end)) " +
		   "from Tarifa t " +
		   "where t.artista.activo = true " +
		   "group by t.artista.id, t.artista.agencia.nombre,  t.artista.nombre " +
		   "order by GREATEST(max(t.fechaCreacion), max(t.fechaModificacion))")
	List<ActividadRecord> findActividadTarifasConConteo(@Param("fechaLimite") LocalDateTime fechaLimite);
}
