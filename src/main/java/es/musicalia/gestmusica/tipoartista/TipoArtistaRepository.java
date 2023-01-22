package es.musicalia.gestmusica.tipoartista;

import es.musicalia.gestmusica.tipoartista.TipoArtista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoArtistaRepository extends JpaRepository<TipoArtista, Long> {


}
