package es.musicalia.gestmusica.accesoartista;

import es.musicalia.gestmusica.permiso.PermisoRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AccesoArtistaService {

	List<AccesoArtistaDto> listaAccesosArtistaAgencia(Long idAgencia);

	List<PermisoRecord> obtenerPermisosTipoArtista();

	@Transactional
	AccesoArtista guardarAccesoArtista(AccesoArtistaDto accesoDto);

	@Transactional
	AccesoArtista eliminarAccesoArtista(Long idAccesoArtista);

	Map<Long, Set<String>> obtenerMapPermisosArtista(Long idUsuario);
}
