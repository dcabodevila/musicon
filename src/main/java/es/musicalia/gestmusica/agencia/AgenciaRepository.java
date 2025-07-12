package es.musicalia.gestmusica.agencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AgenciaRepository extends JpaRepository<Agencia, Long> {

	@Query("select new es.musicalia.gestmusica.agencia.AgenciaRecord(a.id, a.nombre, a.descripcion, a.logo, a.usuario.id,concat(a.usuario.nombre, ' ', a.usuario.apellidos)) from Agencia a where a.activo order by a.nombre")
	List<AgenciaRecord> findAllAgenciasOrderedByName();

	@Query("select new es.musicalia.gestmusica.agencia.AgenciaRecord(a.id, a.nombre, a.descripcion, a.logo, a.usuario.id, concat(a.usuario.nombre, ' ', a.usuario.apellidos)) from Agencia a where a.activo and a.tarifasPublicas order by a.nombre")
	List<AgenciaRecord> findAllAgenciasRecordActivasTarifasPublicasByIdUsuario();
	@Query("select new es.musicalia.gestmusica.agencia.AgenciaRecord(a.id, a.nombre, a.descripcion, a.logo, a.usuario.id, concat(a.usuario.nombre, ' ', a.usuario.apellidos)) from Agencia a where a.id = :idAgencia and a.activo")
	AgenciaRecord findAgenciaRecordById(@Param("idAgencia") Long idAgencia);
	@Query("select new es.musicalia.gestmusica.agencia.AgenciaRecord(a.id, a.nombre, a.descripcion, a.logo, a.usuario.id, concat(a.usuario.nombre, ' ', a.usuario.apellidos)) from Agencia a where a.id in (:idsAgencias) and a.activo order by a.nombre")
	List<AgenciaRecord> findAllAgenciasByIds(@Param("idsAgencias") Set<Long> idsAgencias);

	@Query("select new es.musicalia.gestmusica.agencia.AgenciaRecord(a.id, a.nombre, a.descripcion, a.logo, a.usuario.id, concat(a.usuario.nombre, ' ', a.usuario.apellidos)) from Agencia a where a.id not in (:idsAgencias) and a.activo order by a.nombre")
	List<AgenciaRecord> findAllAgenciasNotByIds(@Param("idsAgencias") Set<Long> idsAgencias);

}
