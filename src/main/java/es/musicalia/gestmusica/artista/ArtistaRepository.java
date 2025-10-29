package es.musicalia.gestmusica.artista;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ArtistaRepository extends JpaRepository<Artista, Long> {

	@Query("select new es.musicalia.gestmusica.artista.ArtistaRecord(a.id, a.nombre, a.logo) from Artista a where a.activo order by a.nombre")
	List<ArtistaRecord> findAllArtistasOrderedByName();

	@Query("select a from Artista a where a.agencia.id=?1 and a.activo order by a.nombre")
	List<Artista> findAllArtistasByIdAgencia(Long idAgencia);

	@Query("select new es.musicalia.gestmusica.artista.ArtistaRecord(a.id, a.nombre, a.logo) from Artista a where a.agencia.id=?1 and a.activo order by a.nombre")
	List<ArtistaRecord> findAllArtistasRecordByIdAgencia(Long idAgencia);

	@Query("select new es.musicalia.gestmusica.artista.ArtistaRecord(a.id, a.nombre, a.logo) from Artista a where a.id in (:idsMisArtistas) and a.activo order by a.nombre")
	List<ArtistaRecord> findMisArtistas(@Param("idsMisArtistas") Set<Long> idsMisArtistas);

	@Query("select new es.musicalia.gestmusica.artista.ArtistaRecord(a.id, a.nombre, a.logo) from Artista a where a.id not in (:idsMisArtistas) and a.activo order by a.nombre")
	List<ArtistaRecord> findOtrosArtistas(@Param("idsMisArtistas") Set<Long> idsMisArtistas);

	@Query("SELECT DISTINCT a FROM Artista a " +
		   "JOIN a.tiposArtista ta " +
		   "WHERE a.ccaa.id IN (:idsComunidades) " +
		   "AND ta.id IN (:idsTipoArtista) " +
		   "AND a.tarifasPublicas = true " +
		   "AND a.activo = true")
	Set<Artista> findArtistasByComunidadesAndTipos(
		@Param("idsComunidades") Set<Long> idsComunidades,
		@Param("idsTipoArtista") Set<Long> idsTipoArtista
	);

	@Query("select new es.musicalia.gestmusica.artista.ArtistaAgenciaRecord(a.id, a.agencia.id) from Artista a where a.idArtistaGestmanager = :idArtistaGestmanager")
	Optional<ArtistaAgenciaRecord> findArtistaByIdArtistaGestmanager(@Param("idArtistaGestmanager") Long idArtistaGestmanager);

    @Query("select new es.musicalia.gestmusica.artista.ArtistaRecord(a.id, a.nombre, a.logo) from Artista a where a.activo order by a.nombre")
    Page<ArtistaRecord> findAllArtistasOrderedByNamePaginated(Pageable pageable);

    @Query("select new es.musicalia.gestmusica.artista.ArtistaRecord(a.id, a.nombre, a.logo) from Artista a where a.id in (:idsMisArtistas) and a.activo order by a.nombre")
    Page<ArtistaRecord> findMisArtistasPaginated(@Param("idsMisArtistas") Set<Long> idsMisArtistas, Pageable pageable);

}