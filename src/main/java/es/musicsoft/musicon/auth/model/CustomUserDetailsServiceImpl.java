package es.musicsoft.musicon.auth.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import es.musicsoft.musicon.permiso.Permiso;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.musicsoft.musicon.usuario.Usuario;
import es.musicsoft.musicon.usuario.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Override
	@Transactional(readOnly = false)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		final Usuario usuario = this.usuarioRepository.findByUsername(username.trim());
		if (usuario == null) {
			throw new UsernameNotFoundException(username);
		}
		List<GrantedAuthority> auth = new ArrayList<GrantedAuthority>();
		if (usuario.getRol()!=null){
			final Set<Permiso> permisos = usuario.getRol().getPermisos();

			for (Permiso permiso : permisos){
				GrantedAuthority authority = new SimpleGrantedAuthority(permiso.getCodigo());
				auth.add(authority);
			}
		}


		final UserDetails userDetails = new CustomAuthenticatedUser(usuario, true, true, true, true, auth);
		final Date date = new Date();
		usuario.setFechaUltimoAcceso(new Timestamp(date.getTime()));
		this.usuarioRepository.save(usuario);
		return userDetails;
	}

}
