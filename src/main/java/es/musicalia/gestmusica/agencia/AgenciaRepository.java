package es.musicalia.gestmusica.agencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AgenciaRepository extends JpaRepository<Agencia, Long> {

	@Query("select a from Agencia a where a.activo order by a.nombre")
	List<Agencia> findAllAgenciasOrderedByName();

	@Query("select a from Agencia a where a.usuario.id=?1 and a.activo order by a.nombre")
	List<Agencia> findAllAgenciasByIdUsuario(Long idUsuario);

	@Query("select new es.musicalia.gestmusica.agencia.AgenciaRecord(a.id, a.nombre) from Agencia a where a.activo and a.tarifasPublicas order by a.nombre")
	List<AgenciaRecord> findAllAgenciasRecordActivasTarifasPublicasByIdUsuario();

	@Query("select a from Agencia a where a.id in (:idsAgencias) and a.activo order by a.nombre")
	List<Agencia> findAllAgenciasByIds(@Param("idsAgencias") Set<Long> idsAgencias);

	@Query("select a from Agencia a where a.id not in (:idsAgencias) and a.activo order by a.nombre")
	List<Agencia> findAllAgenciasNotByIds(@Param("idsAgencias") Set<Long> idsAgencias);

}
