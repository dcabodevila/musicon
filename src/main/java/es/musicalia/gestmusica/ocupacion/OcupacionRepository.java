package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.actividad.ActividadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface OcupacionRepository extends JpaRepository<Ocupacion, Long>, JpaSpecificationExecutor<Ocupacion>
{

    long countByActivoTrueAndFechaCreacionGreaterThanEqual(LocalDateTime desde);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionRecord(t.id, t.fecha, t.artista.id, t.artista.nombre, cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, COALESCE(t.municipio.nombre, 'Sin municipio'), t.poblacion, t.matinal, t.soloMatinal,t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos) FROM Ocupacion t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo and t.ocupacionEstado.id!=4")
    List<OcupacionRecord> findOcupacionesDtoByArtistaIdAndDates(long idArtista, LocalDateTime start, LocalDateTime end);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionRecord(t.id, t.fecha, t.artista.id,  t.artista.nombre,cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, COALESCE(t.municipio.nombre, 'Sin municipio'), t.poblacion, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos) FROM Ocupacion t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo and t.ocupacionEstado.id!=4 and t.id != ?4")
    List<OcupacionRecord> findOcupacionesDtoByArtistaIdAndDatesNotInId(long idArtista, LocalDateTime start, LocalDateTime end, Long id);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionEditDto(t.id, t.fecha, t.artista.id, cast(TRUNC(t.importe,0) as string), cast(TRUNC(t.porcentajeRepre,0) as string), cast(TRUNC(t.iva,0) as string), t.tipoOcupacion.id, t.tipoOcupacion.nombre, t.provincia.ccaa.id, t.provincia.id, t.provincia.nombre, COALESCE(t.municipio.id, 8117L), COALESCE(t.municipio.nombre, 'Sin municipio'), t.poblacion, t.lugar, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.observaciones, t.provisional, t.textoOrquestasDeGalicia, t.usuario.id, t.publicadoOdg, COALESCE(cast(t.horaActuacionDesde as string), ''), COALESCE(cast(t.horaActuacionHasta as string), ''), t.eventoVisible) FROM Ocupacion t WHERE t.id= ?1")
    OcupacionEditDto findOcupacionEditDtoByArtistaIdAndDates(long id);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionRecord(t.id, t.fecha, t.artista.id,  t.artista.nombre, cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, COALESCE(t.municipio.nombre, 'Sin municipio'), t.poblacion, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos) FROM Ocupacion t WHERE t.artista.agencia.id in (:idsAgencia) and t.activo and (t.ocupacionEstado.id =3  or t.ocupacionEstado.id=2) and t.fecha >= CURRENT_DATE order by t.fecha ")
    Optional<List<OcupacionRecord>> findOcupacionesDtoByAgenciaPendientes(@Param("idsAgencia") Set<Long> idsAgencia);

    @Query(value = "select o FROM Ocupacion o WHERE o.idOcupacionLegacy=?1")
    Optional<Ocupacion> findOcupacionByIdOcupacionLegacy(Integer idOcupacionLegacy);

    @Query("SELECT o FROM Ocupacion o WHERE o.fecha>= :fechaDesde AND o.activo AND o.idOcupacionLegacy NOT IN :ids")
    Optional<List<Ocupacion>> findByIdOcupacionLegacyNotIn(@Param("fechaDesde") LocalDateTime fechaDesde, @Param("ids") Set<Integer> ids);

    @Query("""
            SELECT o
            FROM Ocupacion o
            WHERE o.artista.activo = true
            AND o.artista.isSincronizarOdg = true
            AND o.activo = true
            AND o.tipoOcupacion.id = 1
            AND o.ocupacionEstado.id = :estadoOcupadoId
            AND o.fecha >= :fechaDesde
            AND o.fecha <= :fechaHasta
            AND o.publicadoOdg = false
            AND o.excluirSincronizacionOdg = false
            ORDER BY o.fecha ASC
            """)
    List<Ocupacion> findPendientesPublicarOdg(@Param("fechaDesde") LocalDateTime fechaDesde,
                                              @Param("fechaHasta") LocalDateTime fechaHasta,
                                              @Param("estadoOcupadoId") Long estadoOcupadoId);

    @Query("""
            SELECT o
            FROM Ocupacion o
            WHERE o.artista.activo = true
            AND o.artista.isSincronizarOdg = true
            AND o.activo = true
            AND o.tipoOcupacion.id = 1
            AND o.ocupacionEstado.id = :estadoOcupadoId
            AND o.fecha >= :fechaDesde
            AND o.fecha <= :fechaHasta
            AND o.publicadoOdg = true
            AND o.fechaPublicacionOdg IS NOT NULL
            AND o.fechaModificacion IS NOT NULL
            AND o.fechaModificacion > o.fechaPublicacionOdg
            AND o.excluirSincronizacionOdg = false
            ORDER BY o.fecha ASC
            """)
    List<Ocupacion> findPendientesActualizarOdg(@Param("fechaDesde") LocalDateTime fechaDesde,
                                                @Param("fechaHasta") LocalDateTime fechaHasta,
                                                @Param("estadoOcupadoId") Long estadoOcupadoId);

    @Query("""
            SELECT o
            FROM Ocupacion o
            WHERE o.artista.activo = true
            AND o.artista.isSincronizarOdg = true
            AND o.fecha >= :fechaDesde
            AND o.fecha <= :fechaHasta
            AND o.publicadoOdg = true
            AND o.fechaPublicacionOdg IS NOT NULL
            AND o.fechaModificacion IS NOT NULL
            AND o.fechaModificacion > o.fechaPublicacionOdg
            AND (o.ocupacionEstado.id = :estadoAnuladoId OR o.activo = false)
            ORDER BY o.fecha ASC
            """)
    List<Ocupacion> findPendientesEliminarOdg(@Param("fechaDesde") LocalDateTime fechaDesde,
                                              @Param("fechaHasta") LocalDateTime fechaHasta,
                                              @Param("estadoAnuladoId") Long estadoAnuladoId);


	@Query("select new es.musicalia.gestmusica.actividad.ActividadRecord(o.artista.id, o.artista.agencia.nombre, o.artista.nombre, " +
		   "GREATEST(max(o.fechaCreacion), max(o.fechaModificacion)), " +
		   "sum(case when GREATEST(o.fechaCreacion, coalesce(o.fechaModificacion, o.fechaCreacion)) >= :fechaLimite then 1 else 0 end)) " +
		   "from Ocupacion o " +
		   "where o.artista.activo = true " +
		   "group by o.artista.id, o.artista.agencia.nombre,  o.artista.nombre " +
		   "order by GREATEST(max(o.fechaCreacion), max(o.fechaModificacion)) desc")
	List<ActividadRecord> findActividadOcupacionesConConteo(@Param("fechaLimite") LocalDateTime fechaLimite);

	@Query("SELECT COUNT(o) FROM Ocupacion o WHERE o.tarifa.id = :tarifaId AND o.activo = true")
	long countActivasByTarifaId(@Param("tarifaId") Long tarifaId);
}
