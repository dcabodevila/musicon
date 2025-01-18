package es.musicalia.gestmusica.acceso;

import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.rol.RolRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AccesoServiceImpl implements AccesoService {

	private final AccesoRepository accesoRepository;
	private final RolRepository rolRepository;
	private final AgenciaRepository agenciaRepository;
	private final UsuarioRepository usuarioRepository;

	public AccesoServiceImpl(AccesoRepository accesoRepository, RolRepository rolRepository, AgenciaRepository agenciaRepository,UsuarioRepository usuarioRepository){
		this.accesoRepository = accesoRepository;
        this.rolRepository = rolRepository;
        this.agenciaRepository = agenciaRepository;
		this.usuarioRepository = usuarioRepository;
    }

	@Override
	public Acceso crearAccesoUsuarioAgenciaRol(Long idUsuario, Long idAgencia, Long idRol){

		final Acceso acceso = this.accesoRepository
				.findAccesoByIdUsuarioAndIdAgencia(idUsuario, idAgencia)
				.orElseGet(Acceso::new);

		acceso.setUsuario(this.usuarioRepository.findById(idUsuario).get());
		acceso.setAgencia(this.agenciaRepository.findById(idAgencia)
				.orElseThrow(() -> new EntityNotFoundException("Agencia no encontrada con ID: " + idAgencia)));
		acceso.setRol(this.rolRepository.findById(idRol)
				.orElseThrow(() -> new EntityNotFoundException("Rol no encontrado con ID: " + idRol)));

		return this.accesoRepository.save(acceso);

	}

	@Override
	public List<AccesoDto> listaAccesosAgencia(Long idAgencia){
		return accesoRepository.findAllAccesosByIdAgencia(idAgencia)
				.orElseGet(Collections::emptyList) // Retorna una lista vacía si el Optional está vacío
				.stream() // Convierte la lista en un Stream
				.map(this::getAccesoDto) // Transforma cada Acceso en un AccesoDto
				.collect(Collectors.toList()); // Recoge el resultado en una lista
	}

	private AccesoDto getAccesoDto(Acceso acceso) {
		AccesoDto accesoDto = new AccesoDto();
		accesoDto.setId(acceso.getId());
		accesoDto.setIdUsuario(acceso.getUsuario().getId());
		accesoDto.setNombreUsuario(acceso.getUsuario().getNombreCompleto());
		accesoDto.setIdAgencia(acceso.getAgencia().getId());
		accesoDto.setAgencia(acceso.getAgencia().getNombre());
		accesoDto.setIdRol(acceso.getRol().getId());
		accesoDto.setRol(acceso.getRol().getNombre());
		return accesoDto;
	}


}
