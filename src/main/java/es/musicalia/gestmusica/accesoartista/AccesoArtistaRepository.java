package es.musicalia.gestmusica.accesoartista;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AccesoArtistaRepository extends JpaRepository<AccesoArtista, Long> {

    @Query("select a from AccesoArtista a where a.artista.agencia.id = ?1 and a.activo order by a.artista.nombre, a.usuario.nombre, a.usuario.apellidos")
    Optional<List<AccesoArtista>> findAllAccesosByIdAgencia(Long idAgencia);



}
