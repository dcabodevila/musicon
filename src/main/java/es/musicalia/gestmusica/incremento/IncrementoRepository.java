package es.musicalia.gestmusica.incremento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncrementoRepository extends JpaRepository<Incremento, Long> {
    @Query(value ="select new es.musicalia.gestmusica.incremento.IncrementoListDto( i.id, i.artista.id, i.artista.nombre, i.provincia.id, i.provincia.nombre, i.tipoIncremento.id, i.tipoIncremento.nombre, i.incremento)  FROM Incremento i WHERE i.artista.id= ?1 order by i.provincia.nombre")
    List<IncrementoListDto> findIncrementosByArtistaId(long idArtista);

    @Query(value ="select i FROM Incremento i WHERE i.artista.id= ?1 and i.provincia.id=?2")
    Incremento findIncrementoByAgenciaIdAndProvinciaId(long idArtista, long idProvincia);


}
