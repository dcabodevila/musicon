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

	public CustomAuthenticatedUser(Usuario u, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired,
								   boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, Map<Long, Set<String>> mapPermisosArtista, Map<Long, Set<String>> mapPermisosAgencia) {

		super(u.getNombre().concat(" ").concat(u.getApellidos() != null ? u.getApellidos() : ""), u.getPassword(),
				enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
		this.usuario = u;
		this.userId = u.getId();
		this.mapPermisosArtista = mapPermisosArtista;
		this.mapPermisosAgencia = mapPermisosAgencia;
	}


}
