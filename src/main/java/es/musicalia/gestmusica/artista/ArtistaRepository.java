package es.musicalia.gestmusica.artista;

import es.musicalia.gestmusica.agencia.Agencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	@Query("select new es.musicalia.gestmusica.artista.ArtistaRecord(a.id, a.nombre) from Artista a where a.agencia.id=?1 and a.activo order by a.nombre")
	List<ArtistaRecord> findAllArtistasRecordByIdAgencia(Long idAgencia);

	@Query("select a from Artista a where a.id in (:idsMisArtistas) and a.activo order by a.nombre")
	List<Artista> findMisArtistas(@Param("idsMisArtistas") Set<Long> idsMisArtistas);

	@Query("select a from Artista a where a.id not in (:idsMisArtistas) and a.activo order by a.nombre")
	List<Artista> findOtrosArtistas(@Param("idsMisArtistas") Set<Long> idsMisArtistas);

}
