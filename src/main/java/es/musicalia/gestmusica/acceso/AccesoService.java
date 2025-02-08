package es.musicalia.gestmusica.acceso;

import es.musicalia.gestmusica.permiso.PermisoRecord;
import es.musicalia.gestmusica.rol.RolRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AccesoService {

	Acceso crearAccesoUsuarioAgenciaRol(Long idUsuario, Long idAgencia, Long idRol);

	List<AccesoDto> listaAccesosAgencia(Long idAgencia);

    List<RolRecord> obtenerRolesAgencia();

	List<PermisoRecord> obtenerPermisos(Long idRol);

	@Transactional
	Acceso guardarAcceso(AccesoDto accesoDto);

	@Transactional
	Acceso eliminarAcceso(Long idAcceso);
}
