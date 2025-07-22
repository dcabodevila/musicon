package es.musicalia.gestmusica.ocupacion;

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

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionRecord(t.id, t.fecha, t.artista.id, t.artista.nombre, cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, t.municipio.nombre, t.poblacion, t.matinal, t.soloMatinal,t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos) FROM Ocupacion t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo and t.ocupacionEstado.id!=4")
    List<OcupacionRecord> findOcupacionesDtoByArtistaIdAndDates(long idArtista, LocalDateTime start, LocalDateTime end);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionRecord(t.id, t.fecha, t.artista.id,  t.artista.nombre,cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, t.municipio.nombre, t.poblacion, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos) FROM Ocupacion t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo and t.ocupacionEstado.id!=4 and t.id != ?4")
    List<OcupacionRecord> findOcupacionesDtoByArtistaIdAndDatesNotInId(long idArtista, LocalDateTime start, LocalDateTime end, Long id);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionEditDto(t.id, t.fecha, t.artista.id, cast(TRUNC(t.importe,0) as string), cast(TRUNC(t.porcentajeRepre,0) as string), cast(TRUNC(t.iva,0) as string), t.tipoOcupacion.id, t.tipoOcupacion.nombre, t.provincia.ccaa.id, t.provincia.id, t.provincia.nombre, t.municipio.id, t.municipio.nombre, t.poblacion, t.lugar, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.observaciones, t.provisional) FROM Ocupacion t WHERE t.id= ?1")
    OcupacionEditDto findOcupacionEditDtoByArtistaIdAndDates(long id);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionRecord(t.id, t.fecha, t.artista.id,  t.artista.nombre, cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, t.municipio.nombre, t.poblacion, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos) FROM Ocupacion t WHERE t.artista.agencia.id in (:idsAgencia) and t.activo and (t.ocupacionEstado.id =3  or t.ocupacionEstado.id=2) order by t.id ")
    Optional<List<OcupacionRecord>> findOcupacionesDtoByAgenciaPendientes(@Param("idsAgencia") Set<Long> idsAgencia);

    @Query(value = "select o FROM Ocupacion o WHERE o.artista.id= ?1 AND o.fecha >= ?2 AND o.fecha<= ?3 and o.activo and o.ocupacionEstado.id!=4")
    Optional<List<Ocupacion>> findOcupacionesDtoByArtistaIdAndDatesCualquiera(long idArtista, LocalDateTime start, LocalDateTime end);

//    @Query("SELECT o FROM Ocupacion o WHERE o.provincia.id = :provinciaId AND o.poblacion = :poblacion AND o.lugar = :lugar")
//    Optional<Ocupacion> findOcupacionByProvinciaIdAndPoblacionAndLugar(@Param("provinciaId") Long provinciaId, @Param("poblacion") String poblacion, @Param("lugar") String lugar);

}
