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

	/**
	 * Consulta nativa para obtener datos de tarifa anual con ocupación en formato horizontal
	 * Replica la consulta del reporte JasperReports tarifa_anual_horizontal_ocupacion.jrxml
	 */
	@Query(value = """
		WITH cal AS (
		  SELECT gs AS fecha,
		         EXTRACT(MONTH FROM gs) AS mes,
		         EXTRACT(DAY   FROM gs) AS dia,
		         EXTRACT(DOW   FROM gs) AS dow
		  FROM generate_series(
		         make_date(?2, 1, 1),
		         make_date(?2, 12, 31),
		         interval '1 day'
		       ) AS gs
		),
		art AS (
		  SELECT a.id, a.nombre, ag.nombre AS agencia
		  FROM gestmusica.artista a
		  JOIN gestmusica.agencia ag ON ag.id = a.agencia_id
		  WHERE a.id = ?1
		),
		t AS (
		  SELECT t.artista_id, t.fecha AS f, t.importe, t.activo
		  FROM gestmusica.tarifa t
		  WHERE t.artista_id = ?1
		    AND t.activo
		),
		o AS (
		  SELECT o.artista_id, o.fecha AS f, o.estado_id, o.provincia_id, o.activo
		  FROM gestmusica.ocupacion o
		  WHERE o.artista_id = ?1
		    AND o.activo
		    AND o.estado_id IN (1,2,3)
		),
		ip AS (
		  SELECT ip.artista_id, ip.provincia_id, ip.tipo_incremento_id, ip.incremento
		  FROM gestmusica.incremento_provincial ip
		  WHERE ip.artista_id = ?1
		    AND ip.provincia_id = CAST(?3 AS bigint)
		)
		SELECT
		  art.nombre  AS nombre,
		  ag.nombre AS agencia,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=1  THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=1  THEN to_char(cal.fecha,'DD') END) AS enero,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=1  AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=1  AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=1  AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=1  THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS enero_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=2  THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=2  THEN to_char(cal.fecha,'DD') END) AS febrero,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=2  AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=2  AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=2  AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=2  THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS febrero_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=3  THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=3  THEN to_char(cal.fecha,'DD') END) AS marzo,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=3  AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=3  AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=3  AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=3  THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS marzo_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=4  THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=4  THEN to_char(cal.fecha,'DD') END) AS abril,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=4  AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=4  AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=4  AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=4  THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS abril_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=5  THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=5  THEN to_char(cal.fecha,'DD') END) AS mayo,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=5  AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=5  AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=5  AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=5  THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS mayo_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=6  THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=6  THEN to_char(cal.fecha,'DD') END) AS junio,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=6  AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=6  AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=6  AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=6  THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS junio_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=7  THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=7  THEN to_char(cal.fecha,'DD') END) AS julio,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=7  AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=7  AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=7  AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=7  THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS julio_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=8  THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=8  THEN to_char(cal.fecha,'DD') END) AS agosto,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=8  AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=8  AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=8  AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=8  THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS agosto_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=9  THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=9  THEN to_char(cal.fecha,'DD') END) AS septiembre,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=9  AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=9  AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=9  AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=9  THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS septiembre_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=10 THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=10 THEN to_char(cal.fecha,'DD') END) AS octubre,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=10 AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=10 AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=10 AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=10 THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS octubre_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=11 THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=11 THEN to_char(cal.fecha,'DD') END) AS noviembre,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=11 AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=11 AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=11 AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=11 THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS noviembre_valor,
		  (ARRAY['D','L','M','X','J','V','S'])[ (MAX(CASE WHEN cal.mes=12 THEN cal.dow END) + 1) ]
		    || MAX(CASE WHEN cal.mes=12 THEN to_char(cal.fecha,'DD') END) AS diciembre,
		  COALESCE(
		    MAX(CASE WHEN cal.mes=12 AND ?4 AND o.estado_id=2 THEN 'RESER' END),
		    MAX(CASE WHEN cal.mes=12 AND ?4 AND o.estado_id=3 THEN 'PEND'  END),
		    MAX(CASE WHEN cal.mes=12 AND ?4 THEN p.abreviatura END),
		    MAX(CASE WHEN cal.mes=12 THEN
		      CASE
		        WHEN ip.tipo_incremento_id=1 AND t.importe>0 THEN to_char(t.importe + COALESCE(ip.incremento,0), 'FM999999')
		        WHEN ip.tipo_incremento_id=2 AND t.importe>0 THEN to_char(t.importe + (t.importe*ip.incremento)/100.0, 'FM999999')
		        ELSE to_char(t.importe, 'FM999999')
		      END
		    END)
		  ) AS diciembre_valor
		FROM gestmusica.artista art
		INNER JOIN gestmusica.agencia ag ON art.agencia_id = ag.id
		CROSS JOIN cal
		LEFT JOIN t ON t.artista_id = art.id AND t.activo AND t.f = cal.fecha
		LEFT JOIN o ON ?4 AND o.activo AND o.artista_id = art.id AND o.f = cal.fecha AND o.estado_id != 4
		LEFT JOIN gestmusica.provincia p ON p.id = o.provincia_id
		LEFT JOIN ip ON ip.artista_id = art.id AND ip.provincia_id = CAST(?3 AS bigint)
		WHERE art.id = ?1
		GROUP BY art.id, art.nombre, ag.nombre, cal.dia
		ORDER BY art.nombre, ag.nombre, cal.dia
		""", nativeQuery = true)
	List<Object[]> findTarifaAnualData(
		Long idArtista,
		Integer ano,
		Integer idProvincia,
		Boolean conOcupacion
	);

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

	@Query("select new es.musicalia.gestmusica.tarifa.TarifaArtistaCcaaDto(t.artista.id, t.artista.nombre, t.importe) " +
		   "from Tarifa t " +
		   "join t.artista a " +
		   "where " +
		   " t.fecha >= :fechaInicio " +
		   "and t.fecha <= :fechaFin " +
		   "and t.activo = true " +
            "and a.componentes <= :numeroComponentes +2 and a.componentes >= :numeroComponentes -2"+
		   "and t.importe > 0 " +
		   "and a.id != :idArtistaExcluir " +
		   "order by t.importe desc")
	List<TarifaArtistaCcaaDto> findTarifasByFechaAndNumeroComponentes(
		@Param("numeroComponentes") int numeroComponentes,
		@Param("fechaInicio") LocalDateTime fechaInicio,
		@Param("fechaFin") LocalDateTime fechaFin,
		@Param("idArtistaExcluir") Long idArtistaExcluir
	);

	boolean existsByArtistaAgenciaIdAndActivoTrue(Long agenciaId);

	@Query(value = "SELECT COUNT(*) FROM gestmusica.tarifa WHERE artista_id = :idArtista AND activo = true AND EXTRACT(YEAR FROM fecha) = :ano", nativeQuery = true)
	int countTarifasActivasByArtistaAndAnio(@Param("idArtista") Long idArtista, @Param("ano") int ano);
}
