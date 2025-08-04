package es.musicalia.gestmusica.acceso;

import es.musicalia.gestmusica.permiso.PermisoRecord;
import es.musicalia.gestmusica.rol.RolRecord;

import java.util.List;
import java.util.Set;

public interface AccesoService {


	Acceso crearAccesoUsuarioAgenciaRol(Long idUsuario, Long idAgencia, Long idRol, Long idArtista);

	List<AccesoDto> listaAccesosAgencia(Long idAgencia);

    List<RolRecord> obtenerRolesAgencia();

	List<PermisoRecord> obtenerPermisos(Long idRol);

	void eliminarAcceso(Long idAcceso);

	Acceso guardarAcceso(AccesoDto accesoDto);

	void guardarPermisosArtistas(Acceso acceso, Long idArtista);

    List<AccesoDetailRecord> findAllAccesosDetailRecordByIdUsuario(Long idUsuario);

	List<AccesoDetailRecord> getMisAccesos(Long userId);

    boolean isUsuarioAccesoEnAgencia(Set<Long> idsAgencia, Long idUsuarioConsulta);
}
