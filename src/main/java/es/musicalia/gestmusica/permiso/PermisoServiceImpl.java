package es.musicalia.gestmusica.permiso;

import es.musicalia.gestmusica.acceso.Acceso;
import es.musicalia.gestmusica.acceso.AccesoRepository;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
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

	public PermisoServiceImpl(AccesoRepository accesoRepository){
		this.accesoRepository = accesoRepository;
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

		// Verifica si el mapa contiene el idArtista y si el permiso existe
		return mapPermisosAgencia != null &&
				mapPermisosAgencia.getOrDefault(idAgencia, Set.of()).contains(codigoPermiso);
	}

	@Override
	public Map<Long, Set<String>> obtenerMapPermisosAgencia(Long idUsuario) {
		return accesoRepository.findAllAccesosByIdUsuario(idUsuario)
				.orElseGet(Collections::emptyList) // Evitar inicializaciones manuales
				.stream()
				.filter(acceso -> acceso.getRol() != null && CollectionUtils.isNotEmpty(acceso.getRol().getPermisos())) // Filtrar roles nulos y permisos vacíos
				.collect(Collectors.toMap(
						acceso -> acceso.getAgencia().getId(), // Clave: ID de la agencia
						acceso -> acceso.getRol().getPermisos().stream()
								.map(Permiso::getCodigo) // Extraer códigos de permisos
								.collect(Collectors.toSet()), // Convertir a Set
						(existing, replacement) -> {
							existing.addAll(replacement); // Combinar permisos en caso de colisión
							return existing;
						}
				));
	}





}
