package es.musicalia.gestmusica.permiso;

import es.musicalia.gestmusica.rol.Rol;
import es.musicalia.gestmusica.usuario.UsuarioRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    @Query("select new es.musicalia.gestmusica.permiso.PermisoRecord(p.id, p.codigo, p.descripcion) from Rol r join r.permisos p where r.id=?1 order by p.descripcion")
    List<PermisoRecord> findAllPermisoRecordByRol(Long idRol);

    @Query("select new es.musicalia.gestmusica.permiso.PermisoRecord(p.id, p.codigo, p.descripcion) from Permiso p where p.tipoPermiso=?1 order by p.descripcion")
    List<PermisoRecord> findAllPermisoRecordByTipo(Integer idTipoPermiso);
}
