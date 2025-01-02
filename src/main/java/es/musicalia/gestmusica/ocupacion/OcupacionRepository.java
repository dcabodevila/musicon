package es.musicalia.gestmusica.ocupacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OcupacionRepository extends JpaRepository<Ocupacion, Long> {

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionDto(t.id, t.fecha, t.artista.id, cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, t.municipio.nombre, t.poblacion, t.matinal, t.soloMatinal,t.ocupacionEstado.nombre) FROM Ocupacion t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo and t.ocupacionEstado.id!=4")
    List<OcupacionDto> findOcupacionesDtoByArtistaIdAndDates(long idArtista, LocalDateTime start, LocalDateTime end);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionDto(t.id, t.fecha, t.artista.id, cast(TRUNC(t.importe,0) as string), true, t.tipoOcupacion.nombre, t.provincia.nombre, t.municipio.nombre, t.poblacion, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre) FROM Ocupacion t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo and t.ocupacionEstado.id!=4 and t.id != ?4")
    List<OcupacionDto> findOcupacionesDtoByArtistaIdAndDatesNotInId(long idArtista, LocalDateTime start, LocalDateTime end, Long id);

    @Query(value ="select new es.musicalia.gestmusica.ocupacion.OcupacionEditDto(t.id, t.fecha, t.artista.id, cast(TRUNC(t.importe,0) as string), t.tipoOcupacion.id, t.tipoOcupacion.nombre, t.provincia.ccaa.id, t.provincia.id, t.provincia.nombre, t.municipio.id, t.municipio.nombre, t.poblacion, t.lugar, t.matinal, t.soloMatinal, t.ocupacionEstado.nombre, t.observaciones) FROM Ocupacion t WHERE t.id= ?1")
    OcupacionEditDto findOcupacionEditDtoByArtistaIdAndDates(long id);



}
