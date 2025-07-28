package es.musicalia.gestmusica.permiso;

import es.musicalia.gestmusica.acceso.AccesoRepository;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.rol.Rol;
import es.musicalia.gestmusica.rol.RolRepository;
import es.musicalia.gestmusica.rol.TipoRolEnum;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.util.GestmusicaUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PermisoServiceImpl implements PermisoService {

	private final AccesoRepository accesoRepository;
	private final PermisoRepository permisoRepository;
	private final UsuarioRepository usuarioRepository;
	private final AgenciaRepository	agenciaRepository;
	private final RolRepository rolRepository;

	public PermisoServiceImpl(AccesoRepository accesoRepository, PermisoRepository permisoRepository, UsuarioRepository usuarioRepository, AgenciaRepository agenciaRepository, RolRepository rolRepository){
		this.accesoRepository = accesoRepository;
        this.permisoRepository = permisoRepository;
        this.usuarioRepository = usuarioRepository;
        this.agenciaRepository = agenciaRepository;
        this.rolRepository = rolRepository;
    }

	@Override
	public boolean existePermisoGeneral(String codigoPermiso) {

		if (GestmusicaUtils.isUserAutheticated()) {

			Collection<? extends GrantedAuthority> authorities =
					((CustomAuthenticatedUser) SecurityContextHolder
							.getContext().getAuthentication().getPrincipal())
							.getAuthorities();

			// Verificamos si alguno de los GrantedAuthority coincide con el código
			return authorities.stream()
					.anyMatch(auth -> auth.getAuthority().equals(codigoPermiso));
		}
		return false;

	}

	@Override
	public boolean existePermisoUsuarioArtista(Long idArtista, String codigoPermiso) {
		if (!GestmusicaUtils.isUserAutheticated()) {
			return false;
		}

		// Obtiene el mapa de permisos del usuario autenticado
		final Map<Long, Set<String>> mapPermisosArtista =
				((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
						.getMapPermisosArtista();

		// Verifica si el mapa contiene el idArtista y si el permiso existe
		return mapPermisosArtista != null &&
				mapPermisosArtista.getOrDefault(idArtista, Set.of()).contains(codigoPermiso);
	}

	@Override
	public boolean existePermisoUsuarioAgencia(Long idAgencia, String codigoPermiso) {
		if (!GestmusicaUtils.isUserAutheticated()) {
			return false;
		}

		// Obtiene el mapa de permisos del usuario autenticado
		Map<Long, Set<String>> mapPermisosAgencia =
				((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
						.getMapPermisosAgencia();

		// Verifica si el mapa contiene el idAgencia y si el permiso existe
		return mapPermisosAgencia != null &&
				mapPermisosAgencia.getOrDefault(idAgencia, Set.of()).contains(codigoPermiso);
	}

	@Override
	public Map<Long, Set<String>> obtenerMapPermisosAgencia(Long idUsuario) {

		final Usuario u = this.usuarioRepository.findById(idUsuario).orElseThrow();
		if (TipoRolEnum.ADMIN.getDescripcion().equals(u.getRolGeneral().getCodigo())) {
			final Rol rol = this.rolRepository.findRolByCodigo(TipoRolEnum.AGENCIA.getDescripcion());

			return this.agenciaRepository.findAll().stream()
					.collect(Collectors.toMap(
							Agencia::getId,
							agencia -> this.permisoRepository.findAllPermisoRecordByRol(rol.getId()).stream()
									.map(PermisoRecord::codigo)
									.collect(Collectors.toSet())
					));
		}

		return accesoRepository.findAllAccesosByIdUsuario(idUsuario)
				.orElseGet(Collections::emptyList) // Evitar inicializaciones manuales
				.stream()
				.filter(acceso -> acceso.getRol() != null && CollectionUtils.isNotEmpty(acceso.getRol().getPermisos())) // Filtrar roles nulos y permisos vacíos
				.collect(Collectors.groupingBy(
						acceso -> acceso.getAgencia().getId(),
						Collectors.flatMapping(
								acceso -> acceso.getRol().getPermisos().stream()
										.filter(permiso -> TipoPermisoEnum.AGENCIA.getId().equals(permiso.getTipoPermiso())) // Filtrar permisos de tipo AGENCIA
										.map(Permiso::getCodigo),
								Collectors.toSet()
						)
				))
				.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(v1, v2) -> v1,
						HashMap::new
				));
	}

	@Override
	public Set<Long> obtenerIdsAgenciaPorPermiso(Long idUsuario, String permisoBuscado) {
		return accesoRepository.findAllAccesosByIdUsuario(idUsuario)
				.orElseGet(Collections::emptyList) // Evitar inicializaciones manuales
				.stream()
				.filter(acceso -> acceso.getRol() != null
						&& CollectionUtils.isNotEmpty(acceso.getRol().getPermisos())) // Filtrar roles nulos y permisos vacíos
				.filter(acceso -> acceso.getRol().getPermisos().stream()
						.anyMatch(permiso -> permiso.getCodigo().equals(permisoBuscado))) // Verificar si el permiso está presente
				.map(acceso -> acceso.getAgencia().getId()) // Extraer IDs de agencias
				.collect(Collectors.toSet()); // Convertir a Set para evitar duplicados
	}

	@Override
	public List<PermisoRecord> obtenerPermisosByIdRol(Long idRol){
		return this.permisoRepository.findAllPermisoRecordByRol(idRol);
	}








}
