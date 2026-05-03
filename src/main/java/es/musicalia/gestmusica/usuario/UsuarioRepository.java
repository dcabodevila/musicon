package es.musicalia.gestmusica.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.musicalia.gestmusica.info.InfoCcaaMetricRecord;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	@Query("select u from Usuario u where u.username = ?1 and u.activo")
	Optional<Usuario> findByUsername(String username);

	@Query("select CASE WHEN COUNT(u) > 0 THEN true ELSE false END from Usuario u where u.username = ?1 and u.activo = true")
	boolean existsUsuarioActivoByUsername(String username);

	@Query("select u from Usuario u where u.recover = ?1")
	Usuario findByToken(String token);

	@Query("select u from Usuario u where u.activo and u.email = ?1 order by id desc")
	Optional<Usuario> findUsuarioActivoByMail(String email);

	@Query("select u from Usuario u where u.email = ?1 order by id desc")
	Optional<Usuario> findUsuarioByMail(String email);

    @Query("select new es.musicalia.gestmusica.usuario.UsuarioRecord(u.id, u.nombre || ' ' || u.apellidos, u.nombreComercial) from Usuario u where u.activo order by u.nombre || ' ' || u.apellidos")
    List<UsuarioRecord> findAllUsuarioRecords();

	@Query("select new es.musicalia.gestmusica.usuario.UsuarioRecord(u.id, u.nombre || ' ' || u.apellidos, u.nombreComercial) from Usuario u where u.activo and u.rolGeneral.codigo<>'ADMIN' order by u.nombre || ' ' || u.apellidos")
	List<UsuarioRecord> findAllUsuarioRecordsNotAdmin();
	@Query("select new es.musicalia.gestmusica.usuario.UsuarioAdminListRecord(u.id, u.nombre,u.apellidos, u.email, rol.nombre, u.fechaUltimoAcceso, u.imagen, u.activo, u.validado, u.nombreComercial, u.provincia.nombre) from Usuario u left join u.rolGeneral rol order by u.id desc")
	List<UsuarioAdminListRecord> findAllUsuarioAdminListRecords();

	@Query("select new es.musicalia.gestmusica.usuario.RepresentanteRecord(u.id, u.nombre,u.apellidos, rol.nombre, u.imagen, u.nombreComercial, u.provincia.nombre) from Usuario u left join u.rolGeneral rol where (rol.codigo<>'ADMIN' or rol.id is null) order by u.nombre || ' ' || u.apellidos")
	List<RepresentanteRecord> findAllRepresentantesRecords();

	@Query("select CASE WHEN COUNT(u) > 0 THEN true ELSE false END from Usuario u where u.email = ?1 ")
	boolean existsUsuarioByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

    @Query("select CASE WHEN COUNT(u) > 0 THEN true ELSE false END from Usuario u where u.activo and u.email = ?1 ")
    boolean existsUsuarioActivoByEmail(String email);


	@Query("select u from Usuario u join u.rolGeneral rol where rol.codigo = ?1")
	List<Usuario> findUsuariosByRolGeneralCodigo(String codigo);

	List<Usuario> findByActivoTrue();

	@Query("select u from Usuario u where u.emailBajaToken = ?1")
	Optional<Usuario> findByEmailBajaToken(String token);

	@Query("""
		SELECT DISTINCT u FROM Usuario u
		LEFT JOIN FETCH u.provincia p
		LEFT JOIN FETCH p.ccaa c
		LEFT JOIN FETCH u.rolGeneral r
		WHERE u.id IN :ids
		ORDER BY u.nombre, u.apellidos
		""")
	List<Usuario> findAllByIdWithRelaciones(@Param("ids") List<Long> ids);

	@Query("SELECT COUNT(u) FROM Usuario u WHERE u.rolGeneral.codigo IN :codigos")
	long countByRolGeneralCodigoIn(@Param("codigos") List<String> codigos);

	long countByRolGeneralCodigoInAndActivoTrue(List<String> codigos);

	@Query("""
		SELECT u FROM Usuario u
		LEFT JOIN FETCH u.provincia p
		LEFT JOIN FETCH p.ccaa c
		LEFT JOIN FETCH u.rolGeneral r
		WHERE u.activo = true
		AND (:ccaaId IS NULL OR c.id = :ccaaId)
		AND (:provinciaId IS NULL OR p.id = :provinciaId)
		AND (:rolCodigo IS NULL OR r.codigo = :rolCodigo)
		ORDER BY u.nombre, u.apellidos
		""")
	List<Usuario> findUsuariosParaComunicacion(
			@Param("ccaaId") Long ccaaId,
			@Param("provinciaId") Long provinciaId,
			@Param("rolCodigo") String rolCodigo
	);

	@Query("""
		SELECT new es.musicalia.gestmusica.info.InfoCcaaMetricRecord(c.nombre, COUNT(u), 0)
		FROM Usuario u
		JOIN u.provincia p
		JOIN p.ccaa c
		WHERE u.activo = true
		GROUP BY c.nombre
		""")
	List<InfoCcaaMetricRecord> countUsuariosActivosValidosPorCcaa();
}
