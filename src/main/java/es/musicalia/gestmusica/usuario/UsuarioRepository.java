package es.musicalia.gestmusica.usuario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	@Query("select u from Usuario u where u.username = ?1")
	Usuario findByUsername(String username);

	boolean existsUsuarioByUsername(String model);

	@Query("select u from Usuario u where u.recover = ?1")
	Usuario findByToken(String token);

	@Query("select u from Usuario u where u.email = ?1 order by id desc")
	List<Usuario> findByMail(String email);

	@Query("select u from Usuario u order by id desc")
	List<Usuario> findAllUsuarios();

	@Query("select new es.musicalia.gestmusica.usuario.UsuarioRecord(u.id, u.nombre || ' ' || u.apellidos) from Usuario u order by id desc")
	List<UsuarioRecord> findAllUsuarioRecords();

}
