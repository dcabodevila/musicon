package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.Agencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistaRepository extends JpaRepository<Artista, Long> {

	@Query("select a from Artista a order by a.nombre")
	List<Artista> findAllArtistasOrderedByName();

	@Query("select a from Artista a where a.usuario.id=?1 order by a.nombre")
	List<Artista> findAllArtistasByIdUsuario(Long idUsuario);

	@Query("select a from Artista a where a.agencia.id=?1 and a.activo order by a.nombre")
	List<Artista> findAllArtistasByIdAgencia(Long idAgencia);



}
