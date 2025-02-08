package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.Agencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ArtistaRepository extends JpaRepository<Artista, Long> {

	@Query("select a from Artista a where a.activo order by a.nombre")
	List<Artista> findAllArtistasOrderedByName();

	@Query("select a from Artista a where a.agencia.id=?1 and a.activo order by a.nombre")
	List<Artista> findAllArtistasByIdAgencia(Long idAgencia);

	@Query("select a.id from Artista a where a.tarifasPublicas and a.activo")
	Set<Long> findAllArtistasTarifasPublicas();

}
