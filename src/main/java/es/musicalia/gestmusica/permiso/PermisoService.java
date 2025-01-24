package es.musicalia.gestmusica.permiso;

import es.musicalia.gestmusica.auth.model.RegistrationForm;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRecord;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PermisoService {

	boolean existePermisoGeneral(String codigoPermiso);

	boolean existePermisoUsuarioArtista(Long idArtista, String codigoPermiso);

	boolean existePermisoUsuarioAgencia(Long idAgencia, String codigoPermiso);

	Map<Long, Set<String>> obtenerMapPermisosAgencia(Long idUsuario);

	Set<Long> obtenerIdsAgenciaPorPermiso(Long idUsuario, String permisoBuscado);
}
