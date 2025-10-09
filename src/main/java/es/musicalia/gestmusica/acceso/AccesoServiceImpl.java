package es.musicalia.gestmusica.acceso;


import es.musicalia.gestmusica.accesoartista.AccesoArtista;
import es.musicalia.gestmusica.accesoartista.AccesoArtistaRepository;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.permiso.Permiso;
import es.musicalia.gestmusica.permiso.PermisoRecord;
import es.musicalia.gestmusica.permiso.PermisoRepository;
import es.musicalia.gestmusica.permiso.TipoPermisoEnum;
import es.musicalia.gestmusica.rol.*;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AccesoServiceImpl implements AccesoService {

	private final AccesoRepository accesoRepository;
	private final RolRepository rolRepository;
	private final PermisoRepository permisoRepository;
	private final AgenciaRepository agenciaRepository;
	private final UsuarioRepository usuarioRepository;
	private final AccesoArtistaRepository accesoArtistaRepository;
	private final ArtistaRepository artistaRepository;
	private final AccesoMapper accesoMapper;

	public AccesoServiceImpl(AccesoRepository accesoRepository, RolRepository rolRepository, PermisoRepository permisoRepository, AgenciaRepository agenciaRepository, UsuarioRepository usuarioRepository, AccesoArtistaRepository accesoArtistaRepository, ArtistaRepository artistaRepository, AccesoMapper accesoMapper){
		this.accesoRepository = accesoRepository;
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.agenciaRepository = agenciaRepository;
		this.usuarioRepository = usuarioRepository;
        this.accesoArtistaRepository = accesoArtistaRepository;
        this.artistaRepository = artistaRepository;
        this.accesoMapper = accesoMapper;

    }

	@Override
	public Acceso crearAccesoUsuarioAgenciaRol(Long idUsuario, Long idAgencia, Long idRol, Long idArtista){

		Acceso acceso = obtenerOCrearAccesoAgencia(idUsuario, idAgencia, idRol);
		AccesoDto accesoDto = accesoMapper.toAccesoDto(acceso);
		accesoDto.setIdArtista(idArtista);
		guardarAcceso(accesoDto);

		return acceso;

	}




	@Override
	public List<AccesoDto> listaAccesosAgencia(Long idAgencia){
		return accesoRepository.findAllAccesosByIdAgencia(idAgencia)
				.orElseGet(Collections::emptyList) // Retorna una lista vacía si el Optional está vacío
				.stream() // Convierte la lista en un Stream
				.map(accesoMapper::toAccesoDto) // Transforma cada Acceso en un AccesoDto
				.collect(Collectors.toList()); // Recoge el resultado en una lista
	}

	@Override
	public List<RolRecord> obtenerRolesAgencia(){
		return this.rolRepository.findAllUsuarioRecords(TipoRolEnum.AGENCIA.getId());
	}

	@Override
	public List<PermisoRecord> obtenerPermisos(Long idRol){

		return this.permisoRepository.findAllPermisoRecordByRol(idRol);
	}

	@Transactional
	@Override
	public Acceso guardarAcceso(AccesoDto accesoDto) {

        if (accesoDto.getId() != null) {
            eliminarAcceso(accesoDto.getId());
            accesoDto.setId(null);
        }

		Acceso acceso = guardarEntityAcceso(accesoDto);

		guardarPermisosArtistas(acceso, accesoDto.getIdArtista(), true);

		return acceso;
	}

	private Acceso guardarEntityAcceso(AccesoDto accesoDto) {
		Acceso acceso = obtenerOCrearAccesoAgencia(accesoDto);
		Usuario usuario = obtenerUsuario(accesoDto.getIdUsuario());

		acceso.setUsuario(usuario);
		acceso.setAgencia(obtenerAgencia(accesoDto.getIdAgencia()));
        final Rol rolAcceso = obtenerRol(accesoDto.getIdRol());
		acceso.setRol(rolAcceso);
		acceso.setArtista(accesoDto.getIdArtista()!=null ? this.artistaRepository.findById(accesoDto.getIdArtista()).orElseThrow() : null);
		acceso.setActivo(Boolean.TRUE);

		acceso = accesoRepository.save(acceso);

        final Rol rolGeneral = obtenerRolGeneralUsuario(usuario);

        if (rolGeneral != null && !rolGeneral.getCodigo().equals(usuario.getRolGeneral().getCodigo())) {
            usuario.setRolGeneral(rolGeneral);
            usuarioRepository.save(usuario);
        }

		return acceso;
	}

    private Rol obtenerRolGeneralUsuario(Usuario u) {
        try {
            final Optional<List<Acceso>> listaAccesosUsuario = this.accesoRepository.findAllAccesosByIdUsuario(u.getId());

            if (listaAccesosUsuario.isPresent() && !listaAccesosUsuario.get().isEmpty()) {
                List<Acceso> accesos = listaAccesosUsuario.get();

                // Verificar si tiene al menos un acceso como agencia
                boolean tieneAccesoAgencia = accesos.stream()
                        .anyMatch(acceso -> RolEnum.ROL_AGENCIA.getCodigo().equals(acceso.getRol().getCodigo()));

                if (tieneAccesoAgencia) {
                    // Se asigna rol general agencia
                    return this.rolRepository.findRolByCodigo(RolEnum.ROL_AGENCIA.getCodigo());
                }

                // Si no es agencia, verificar si tiene acceso como representante
                boolean tieneAccesoRepresentante = accesos.stream()
                        .anyMatch(acceso -> RolEnum.ROL_REPRESENTANTE.getCodigo().equals(acceso.getRol().getCodigo()));

                if (tieneAccesoRepresentante) {
                    // Se asigna rol general representante
                    return this.rolRepository.findRolByCodigo(RolEnum.ROL_REPRESENTANTE.getCodigo());
                }

                // Si no tiene acceso como representante, verificar si tiene rol artista
                boolean tieneRolArtista = accesos.stream()
                        .anyMatch(acceso -> RolEnum.ROL_ARTISTA.getCodigo().equals(acceso.getRol().getCodigo()));

                if (tieneRolArtista) {
                    return this.rolRepository.findRolByCodigo(RolEnum.ROL_ARTISTA.getCodigo());
                }
            }

            return this.rolRepository.findRolByCodigo(RolEnum.ROL_AGENTE.getCodigo());
        } catch (Exception e) {
            log.error("error boteniendo el rol general del usuario", e);
        }

        return null;

    }

	private Acceso obtenerOCrearAccesoAgencia(Long idUsuario, Long idAgencia, Long idRol) {
		Acceso acceso = this.accesoRepository
				.findAccesoByIdUsuarioAndIdAgenciaAndCodigoRol(idUsuario, idAgencia, RolEnum.ROL_AGENCIA.getCodigo())
				.orElseGet(Acceso::new);

		acceso.setUsuario(this.usuarioRepository.findById(idUsuario).orElseThrow());
		acceso.setAgencia(this.agenciaRepository.findById(idAgencia)
				.orElseThrow(() -> new EntityNotFoundException("Agencia no encontrada con ID: " + idAgencia)));
		acceso.setRol(this.rolRepository.findById(idRol)
				.orElseThrow(() -> new EntityNotFoundException("Rol no encontrado con ID: " + idRol)));
		acceso.setActivo(Boolean.TRUE);

		acceso = this.accesoRepository.save(acceso);
		return acceso;
	}

	private Acceso obtenerOCrearAccesoAgencia(AccesoDto accesoDto) {
		return accesoDto.getId() != null
				? accesoRepository.findById(accesoDto.getId()).orElse(new Acceso())
				: new Acceso();
	}

	private Usuario obtenerUsuario(Long idUsuario) {
		return usuarioRepository.findById(idUsuario)
				.orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + idUsuario));
	}

	private Agencia obtenerAgencia(Long idAgencia) {
		return agenciaRepository.findById(idAgencia)
				.orElseThrow(() -> new EntityNotFoundException("Agencia no encontrada con ID: " + idAgencia));
	}

	private Rol obtenerRol(Long idRol) {
		return rolRepository.findById(idRol)
				.orElseThrow(() -> new EntityNotFoundException("Rol no encontrado con ID: " + idRol));
	}

	@Transactional(readOnly = false)
	public void guardarPermisosArtistas(Acceso acceso, Long idArtista, boolean eliminarAntiguos) {

        if (eliminarAntiguos) {
            eliminarPermisosArtistas(acceso.getUsuario().getId(), acceso.getAgencia().getId(), acceso.getRol().getId());
        }

		Set<Permiso> permisosArtista = obtenerPermisosArtista(acceso.getRol().getId());

		List<Artista> artistasAgencia = idArtista != null ?
				artistaRepository.findById(idArtista)
						.map(List::of)
						.orElse(Collections.emptyList()) :
				artistaRepository.findAllArtistasByIdAgencia(acceso.getAgencia().getId());

		// Procesar y guardar los accesos
		artistasAgencia.forEach(artista ->
				permisosArtista.forEach(permiso ->
						crearOActualizarAccesoArtista(artista, acceso.getUsuario(), permiso)
				)
		);
	}

    @Transactional
    public void eliminarPermisosArtistaRolArtista(Long idArtista, Long idUsuario, Long idRol) {
        Objects.requireNonNull(idArtista, "El ID del artista no puede ser null");
        Objects.requireNonNull(idUsuario, "El ID del usuario no puede ser null");
        Objects.requireNonNull(idRol, "El ID del rol no puede ser null");

        final Set<Permiso> permisosArtista = permisoRepository
                .findAllPermisosByIdRolAndTipoPermiso(
                        idRol,
                        TipoPermisoEnum.ARTISTA.getId()
                )
                .orElseThrow(() -> new EntityNotFoundException("No se encontraron permisos para el rol especificado"));

        permisosArtista.forEach(permiso ->
                revocarPermisoArtista(idArtista, idUsuario, permiso.getId())
        );
    }

	@Transactional
	public void eliminarPermisosArtistas(Long idAgencia, Long idUsuario, Long idRol) {
		Set<Permiso> permisosArtista = permisoRepository.findAllPermisosByIdRolAndTipoPermiso(
				idRol,
				TipoPermisoEnum.ARTISTA.getId()
		).orElseThrow(() -> new EntityNotFoundException("No se encontraron permisos para el rol especificado"));

		final List<Artista> artistasAgencia = artistaRepository.findAllArtistasByIdAgencia(idAgencia);
		
		// Procesamos las eliminaciones directamente sin recolectar resultados
		artistasAgencia.forEach(artista -> 
			permisosArtista.forEach(permiso ->
					revocarPermisoArtista(artista.getId(), idUsuario, permiso.getId())
			)
		);
	}

	private Set<Permiso> obtenerPermisosArtista(Long idRol) {
		return permisoRepository.findAllPermisosByIdRolAndTipoPermiso(
				idRol,
				TipoPermisoEnum.ARTISTA.getId()
		).orElseThrow();
	}

	private AccesoArtista crearOActualizarAccesoArtista(Artista artista, Usuario usuario, Permiso permiso) {
		AccesoArtista accesoArtista = accesoArtistaRepository
				.findAllAccesosByIdArtistaIdUsuarioIdPermiso(artista.getId(), usuario.getId(), permiso.getId())
				.orElse(new AccesoArtista());

		accesoArtista.setPermiso(permiso);
		accesoArtista.setArtista(artista);
		accesoArtista.setUsuario(usuario);
		accesoArtista.setActivo(Boolean.TRUE);

		return this.accesoArtistaRepository.save(accesoArtista);

	}

    /**
     * Revoca el permiso específico de un usuario para acceder a un artista.
     *
     * @param idArtista ID del artista del cual se revocará el acceso
     * @param idUsuario ID del usuario al cual se le revocará el acceso
     * @param idPermiso ID del permiso específico que será revocado
     * @throws IllegalArgumentException si cualquiera de los IDs es null
     * @throws NoSuchElementException   si no se encuentra el acceso especificado
     */
    private void revocarPermisoArtista(Long idArtista, Long idUsuario, Long idPermiso) {
        Objects.requireNonNull(idArtista, "El ID del artista no puede ser null");
        Objects.requireNonNull(idUsuario, "El ID del usuario no puede ser null");
        Objects.requireNonNull(idPermiso, "El ID del permiso no puede ser null");

        accesoArtistaRepository
                .findAllAccesosByIdArtistaIdUsuarioIdPermiso(idArtista, idUsuario, idPermiso).ifPresent(accesoArtistaRepository::delete);

    }

	@Transactional
	@Override
	public void eliminarAcceso(Long idAcceso){

		Acceso acceso = this.accesoRepository.findById(idAcceso).orElseThrow();

        if (RolEnum.ROL_ARTISTA.getCodigo().equals(acceso.getRol().getCodigo())) {
            eliminarPermisosArtistaRolArtista(acceso.getArtista().getId(), acceso.getUsuario().getId(), acceso.getRol().getId());
        } else {
            this.eliminarPermisosArtistas(acceso.getAgencia().getId(), acceso.getUsuario().getId(),acceso.getRol().getId());
        }


		this.accesoRepository.delete(acceso);

        Usuario usuario = acceso.getUsuario();

        final Rol rolGeneral = obtenerRolGeneralUsuario(usuario);

        if (rolGeneral != null && !rolGeneral.getCodigo().equals(usuario.getRolGeneral().getCodigo())) {
            usuario.setRolGeneral(rolGeneral);
            usuarioRepository.save(usuario);
        }

	}

	@Override
	public List<AccesoDetailRecord> findAllAccesosDetailRecordByIdUsuario(Long idUsuario) {
		return accesoRepository.findAllAccesosDetailRecordByIdUsuario(idUsuario);
	}

    @Override
    public Optional<List<Acceso>> findAllAccesosByAndIdAgenciaAndCodigoRolAndActivo(Set<String> codigosRol, Long idAgencia){
        return this.accesoRepository.findAllAccesosByAndIdAgenciaAndCodigoRolAndActivo(codigosRol,idAgencia);
    }

	@Override
	public List<AccesoDetailRecord> getMisAccesos(Long userId){

		return this.findAllAccesosDetailRecordByIdUsuario(userId);

	}

	@Override
	public boolean isUsuarioAccesoEnAgencia(final Set<Long> idsAgencia, Long idUsuarioConsulta){

		return accesoRepository.existsActiveAccessByUserIdAndAgencyIds(idUsuarioConsulta, idsAgencia);


	}

}