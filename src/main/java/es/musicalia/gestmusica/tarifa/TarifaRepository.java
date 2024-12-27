package es.musicalia.gestmusica.tarifa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    @Query(value ="select new es.musicalia.gestmusica.tarifa.TarifaDto(t.id, t.fecha, t.artista.id, cast(TRUNC(t.importe,0) as string), true) FROM Tarifa t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo")
    Optional<List<TarifaDto>> findTarifasDtoByArtistaIdAndDates(long idArtista, LocalDateTime start, LocalDateTime end);

    @Query(value ="select t FROM Tarifa t WHERE t.artista.id= ?1 AND t.fecha >= ?2 AND t.fecha<= ?3 and t.activo")
    List<Tarifa> findTarifasByArtistaIdAndDates(long idArtista, LocalDateTime start, LocalDateTime end);


}
