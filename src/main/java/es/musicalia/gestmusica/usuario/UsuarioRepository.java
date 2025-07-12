package es.musicalia.gestmusica.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

	@Query("select new es.musicalia.gestmusica.usuario.UsuarioRecord(u.id, u.nombre || ' ' || u.apellidos) from Usuario u where u.activo order by u.nombre || ' ' || u.apellidos")
	List<UsuarioRecord> findAllUsuarioRecords();
	@Query("select new es.musicalia.gestmusica.usuario.UsuarioAdminListRecord(u.id, u.nombre,u.apellidos, u.email, rol.nombre, u.fechaUltimoAcceso, u.imagen, u.activo, u.validado) from Usuario u left join u.rolGeneral rol order by u.nombre || ' ' || u.apellidos")
	List<UsuarioAdminListRecord> findAllUsuarioAdminListRecords();

	@Query("select new es.musicalia.gestmusica.usuario.RepresentanteRecord(u.id, u.nombre,u.apellidos, rol.nombre, u.imagen) from Usuario u left join u.rolGeneral rol where (rol.codigo<>'ADMIN' or rol.id is null) order by u.nombre || ' ' || u.apellidos")
	List<RepresentanteRecord> findAllRepresentantesRecords();

	@Query("select CASE WHEN COUNT(u) > 0 THEN true ELSE false END from Usuario u where u.email = ?1 ")
	boolean existsUsuarioByEmail(String email);

}
