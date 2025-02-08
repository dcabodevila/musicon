package es.musicalia.gestmusica.rol;

import es.musicalia.gestmusica.usuario.UsuarioRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    @Query("select new es.musicalia.gestmusica.rol.RolRecord(u.id, u.nombre, u.descripcion) from Rol u where u.tipoRol=?1 order by u.nombre")
    List<RolRecord> findAllUsuarioRecords(Integer tipoRol);

}
