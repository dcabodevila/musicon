package es.musicalia.gestmusica.acceso;

import es.musicalia.gestmusica.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.query.Param;

@Repository
public interface AccesoRepository extends JpaRepository<Acceso, Long> {

    @Query("select a from Acceso a where a.usuario.id = ?1 and a.activo")
    Optional<List<Acceso>> findAllAccesosByIdUsuario(Long idUsuario);

    @Query("select a from Acceso a where a.usuario.id = ?1 and a.agencia.id= ?2 and a.activo")
    Optional<Acceso> findAccesoByIdUsuarioAndIdAgencia(Long idUsuario, Long idAgencia);

    @Query("select a from Acceso a where a.agencia.id = ?1 and a.activo order by a.usuario.nombre, a.usuario.apellidos, a.rol.descripcion")
    Optional<List<Acceso>> findAllAccesosByIdAgencia(Long idAgencia);

    @Query("SELECT a FROM Acceso a INNER JOIN FETCH a.rol r WHERE a.agencia.id=:idAgencia AND r.codigo IN (:codigoRol) AND a.activo = true")
    Optional<List<Acceso>> findAllAccesosByAndIdAgenciaAndCodigoRolAndActivo(@Param("codigoRol") Set<String> codigoRol, Long idAgencia);
}