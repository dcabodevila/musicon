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

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionDto(t.id, t.fecha, t.artista.id, t.artista.nombre, cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, t.municipio.nombre, t.poblacion, t.matinal, t.soloMatinal,t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos) FROM Ocupacion t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo and t.ocupacionEstado.id!=4")
    List<OcupacionDto> findOcupacionesDtoByArtistaIdAndDates(long idArtista, LocalDateTime start, LocalDateTime end);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionDto(t.id, t.fecha, t.artista.id,  t.artista.nombre,cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, t.municipio.nombre, t.poblacion, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos) FROM Ocupacion t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo and t.ocupacionEstado.id!=4 and t.id != ?4")
    List<OcupacionDto> findOcupacionesDtoByArtistaIdAndDatesNotInId(long idArtista, LocalDateTime start, LocalDateTime end, Long id);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionEditDto(t.id, t.fecha, t.artista.id, cast(TRUNC(t.importe,0) as string), cast(TRUNC(t.porcentajeRepre,0) as string), cast(TRUNC(t.iva,0) as string), t.tipoOcupacion.id, t.tipoOcupacion.nombre, t.provincia.ccaa.id, t.provincia.id, t.provincia.nombre, t.municipio.id, t.municipio.nombre, t.poblacion, t.lugar, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.observaciones) FROM Ocupacion t WHERE t.id= ?1")
    OcupacionEditDto findOcupacionEditDtoByArtistaIdAndDates(long id);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionDto(t.id, t.fecha, t.artista.id,  t.artista.nombre, cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, t.municipio.nombre, t.poblacion, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos) FROM Ocupacion t WHERE t.artista.agencia.id in (:idsAgencia) and t.activo and (t.ocupacionEstado.id =3  or t.ocupacionEstado.id=2) order by t.id ")
    Optional<List<OcupacionDto>> findOcupacionesDtoByAgenciaPendientes(@Param("idsAgencia") Set<Long> idsAgencia);

//    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionListRecord(t.id, t.fecha, t.artista.id,  t.artista.nombre,cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, t.municipio.nombre, t.poblacion, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.usuario.id, t.usuario.nombre || ' ' || t.usuario.apellidos, uconf.id, uconf.nombre || ' ' || uconf.apellidos) FROM Ocupacion t LEFT JOIN t.usuarioConfirmacion uconf WHERE t.artista.id in (?1) AND t.fecha >= ?2 and t.activo order by t.id desc")
//    List<OcupacionListRecord> findOcupacionesByArtistasListAndDatesActivo(Set<Long> idsArtistas, LocalDateTime start);

}
