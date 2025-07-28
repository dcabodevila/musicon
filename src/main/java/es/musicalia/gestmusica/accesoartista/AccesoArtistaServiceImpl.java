package es.musicalia.gestmusica.accesoartista;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.permiso.Permiso;
import es.musicalia.gestmusica.permiso.PermisoRecord;
import es.musicalia.gestmusica.permiso.PermisoRepository;
import es.musicalia.gestmusica.permiso.TipoPermisoEnum;
import es.musicalia.gestmusica.rol.TipoRolEnum;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AccesoArtistaServiceImpl implements AccesoArtistaService {

	private final AccesoArtistaRepository accesoArtistaRepository;
	private final PermisoRepository permisoRepository;
	private final UsuarioRepository usuarioRepository;
	private final ArtistaRepository artistaRepository;

	public AccesoArtistaServiceImpl(AccesoArtistaRepository accesoRepository, PermisoRepository permisoRepository, UsuarioRepository usuarioRepository, ArtistaRepository artistaRepository){
		this.accesoArtistaRepository = accesoRepository;
        this.permisoRepository = permisoRepository;
		this.usuarioRepository = usuarioRepository;
        this.artistaRepository = artistaRepository;
    }

	@Override
	public List<AccesoArtistaDto> listaAccesosArtistaAgencia(Long idAgencia){
		return accesoArtistaRepository.findAllAccesosByIdAgencia(idAgencia)
				.orElseGet(Collections::emptyList) // Retorna una lista vacía si el Optional está vacío
				.stream() // Convierte la lista en un Stream
				.map(this::getAccesoArtistaDto) // Transforma cada Acceso en un AccesoDto
				.collect(Collectors.toList()); // Recoge el resultado en una lista
	}

	private AccesoArtistaDto getAccesoArtistaDto(AccesoArtista acceso) {
		AccesoArtistaDto accesoDto = new AccesoArtistaDto();
		accesoDto.setId(acceso.getId());
		accesoDto.setIdUsuario(acceso.getUsuario().getId());
		accesoDto.setNombreUsuario(acceso.getUsuario().getNombreCompleto());
		accesoDto.setArtista(acceso.getArtista().getNombre());
		accesoDto.setIdPermiso(acceso.getPermiso().getId());
		accesoDto.setPermiso(acceso.getPermiso().getDescripcion());

		return accesoDto;
	}

	@Override
	public List<PermisoRecord> obtenerPermisosTipoArtista(){
		return this.permisoRepository.findAllPermisoRecordByTipo(TipoPermisoEnum.ARTISTA.getId());
	}

	@Transactional
	@Override
	public AccesoArtista guardarAccesoArtista(AccesoArtistaDto accesoDto){

		AccesoArtista acceso = accesoDto.getId()!=null ? this.accesoArtistaRepository.findById(accesoDto.getId()).orElse(new AccesoArtista()) : new AccesoArtista();

		acceso.setUsuario(this.usuarioRepository.findById(accesoDto.getIdUsuario()).orElseThrow());
		acceso.setArtista(this.artistaRepository.findById(accesoDto.getIdArtista()).orElseThrow());
		acceso.setPermiso(this.permisoRepository.findById(accesoDto.getIdPermiso()).orElseThrow());
		acceso.setActivo(Boolean.TRUE);
		return this.accesoArtistaRepository.save(acceso);

	}

	@Transactional
	@Override
	public AccesoArtista eliminarAccesoArtista(Long idAccesoArtista){

		AccesoArtista acceso = this.accesoArtistaRepository.findById(idAccesoArtista).orElseThrow();
		acceso.setActivo(Boolean.FALSE);
		return this.accesoArtistaRepository.save(acceso);

	}
	@Override
	public Map<Long, Set<String>> obtenerMapPermisosArtista(Long idUsuario) {
		
		final Usuario u = this.usuarioRepository.findById(idUsuario).orElseThrow();
		if (TipoRolEnum.ADMIN.getDescripcion().equals(u.getRolGeneral().getCodigo())) {
			return this.artistaRepository.findAll().stream()
					.collect(Collectors.toMap(
							Artista::getId,
							artista -> this.permisoRepository.findAll().stream()
									.map(Permiso::getCodigo)
									.collect(Collectors.toSet())
					));
		}

		
		
		return accesoArtistaRepository.findAllAccesosArtistaByIdUsuario(idUsuario)
			.orElse(Collections.emptyList())
			.stream()
			.filter(accesoArtista ->
				accesoArtista != null &&
				accesoArtista.getPermiso() != null &&
				accesoArtista.getArtista() != null)
			.collect(Collectors.groupingBy(
				accesoArtista -> accesoArtista.getArtista().getId(),
				Collectors.mapping(
					accesoArtista -> accesoArtista.getPermiso().getCodigo(),
					Collectors.toSet()
				)
			));
	}

}