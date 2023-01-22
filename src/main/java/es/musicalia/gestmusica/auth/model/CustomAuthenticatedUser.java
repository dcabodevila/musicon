package es.musicalia.gestmusica.auth.model;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import es.musicalia.gestmusica.usuario.Usuario;

public class CustomAuthenticatedUser extends User {

	private static final long serialVersionUID = 1L;
	private long userId;
	private Usuario usuario;

	public CustomAuthenticatedUser(Usuario u, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired,
			boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities) {

		super(u.getNombre().concat(" ").concat(u.getApellidos() != null ? u.getApellidos() : ""), u.getPassword(),
				enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
		this.usuario = u;
		this.userId = u.getId();
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

}
