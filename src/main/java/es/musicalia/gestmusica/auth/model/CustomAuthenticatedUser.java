package es.musicalia.gestmusica.auth.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import es.musicalia.gestmusica.permiso.Permiso;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import es.musicalia.gestmusica.usuario.Usuario;

@Getter
@Setter
public class CustomAuthenticatedUser extends User {

	private static final long serialVersionUID = 1L;
	private long userId;
	private Usuario usuario;
	private Map<Long, Set<String>> mapPermisosArtista;
	private Map<Long, Set<String>> mapPermisosAgencia;
	private String nombreCompleto;

	public CustomAuthenticatedUser(Usuario u, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired,
								   boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, Map<Long, Set<String>> mapPermisosArtista, Map<Long, Set<String>> mapPermisosAgencia) {

		super(nombreDeLogin(u), u.getPassword(),
				enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
		this.usuario = u;
		this.userId = u.getId();
		this.nombreCompleto = nombreCompleto(u);
		this.mapPermisosArtista = mapPermisosArtista;
		this.mapPermisosAgencia = mapPermisosAgencia;
	}

	/**
	 * Login name used as principal identity (remember-me, WebAuthn and session registry reload the user by it).
	 */
	private static String nombreDeLogin(Usuario u) {
		if (u.getUsername() != null && !u.getUsername().isBlank()) {
			return u.getUsername();
		}
		if (u.getEmail() != null && !u.getEmail().isBlank()) {
			return u.getEmail();
		}
		return nombreCompleto(u);
	}

	private static String nombreCompleto(Usuario u) {
		return u.getApellidos() == null || u.getApellidos().isBlank()
				? u.getNombre()
				: u.getNombre() + " " + u.getApellidos();
	}

	public boolean hasAccesoAgencia(Long idAgencia) {
		return idAgencia == null || (mapPermisosAgencia != null && mapPermisosAgencia.containsKey(idAgencia));
	}

	public boolean hasPermisoArtista(Long idArtista, String codigoPermiso) {
		return idArtista != null && mapPermisosArtista != null
				&& mapPermisosArtista.getOrDefault(idArtista, Set.of()).contains(codigoPermiso);
	}

	public boolean hasPermisoEnAlgunArtista(String codigoPermiso) {
		return mapPermisosArtista != null && mapPermisosArtista.values().stream()
				.anyMatch(permisos -> permisos != null && permisos.contains(codigoPermiso));
	}

}
