package es.musicalia.gestmusica.acceso;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AccesoService {

	Acceso crearAccesoUsuarioAgenciaRol(Long idUsuario, Long idAgencia, Long idRol);

	List<AccesoDto> listaAccesosAgencia(Long idAgencia);
}
