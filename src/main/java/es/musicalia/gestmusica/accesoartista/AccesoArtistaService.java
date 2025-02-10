package es.musicalia.gestmusica.accesoartista;

import es.musicalia.gestmusica.permiso.PermisoRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AccesoArtistaService {

	List<AccesoArtistaDto> listaAccesosArtistaAgencia(Long idAgencia);

	List<PermisoRecord> obtenerPermisosTipoArtista();

	@Transactional
	AccesoArtista guardarAcceso(AccesoArtistaDto accesoDto);

	@Transactional
	AccesoArtista eliminarAccesoArtista(Long idAccesoArtista);
}
