package es.musicalia.gestmusica.tarifa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    @Query(value ="select new es.musicalia.gestmusica.tarifa.TarifaDto(t.id, t.fecha, t.artista.id, t.importe) FROM Tarifa t WHERE t.artista.id= ?1 ")
    List<TarifaDto> findTarifasByArtistaId(long idArtista);
}
