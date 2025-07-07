package es.musicalia.gestmusica.rol;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    @Query("select new es.musicalia.gestmusica.rol.RolRecord(u.id, u.nombre, u.descripcion, u.codigo) from Rol u where u.tipoRol=?1 order by u.nombre")
    List<RolRecord> findAllUsuarioRecords(Integer tipoRol);

    @Query("select new es.musicalia.gestmusica.rol.RolRecord(u.id, u.nombre, u.descripcion, u.codigo) from Rol u where u.codigo=?1")
    RolRecord findRolRecordByCodigo(String codigo);

    @Query("select u from Rol u where u.codigo=?1")
    Rol findRolByCodigo(String codigo);
}
