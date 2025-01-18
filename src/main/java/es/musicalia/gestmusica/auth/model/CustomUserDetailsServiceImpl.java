package es.musicalia.gestmusica.auth.model;

import java.sql.Timestamp;
import java.util.*;

import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.permiso.Permiso;
import es.musicalia.gestmusica.permiso.PermisoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsServiceImpl implements UserDetailsService {


	private final UsuarioRepository usuarioRepository;
	private final PermisoService permisoService;

	public CustomUserDetailsServiceImpl(PermisoService permisoService, UsuarioRepository usuarioRepository){
		this.usuarioRepository = usuarioRepository;
		this.permisoService = permisoService;
	}

	@Override
	@Transactional(readOnly = false)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		final Usuario usuario = this.usuarioRepository.findByUsername(username.trim());
		if (usuario == null) {
			throw new UsernameNotFoundException(username);
		}
		final List<GrantedAuthority> auth = new ArrayList<GrantedAuthority>();
		if (usuario.getRolGeneral()!=null){
			final Set<Permiso> permisos = usuario.getRolGeneral().getPermisos();

			for (Permiso permiso : permisos){
				GrantedAuthority authority = new SimpleGrantedAuthority(permiso.getCodigo());
				auth.add(authority);
			}
		}
		//TODO: obtenerPermisosArtista
		Map<Long, Set<String>> mapPermisosArtista = new HashMap<>();

		final Map<Long, Set<String>> mapPermisosAgencia = this.permisoService.obtenerMapPermisosAgencia(usuario.getId());

		final UserDetails userDetails = new CustomAuthenticatedUser(usuario, true, true, true, true, auth, mapPermisosArtista, mapPermisosAgencia);
		final Date date = new Date();
		usuario.setFechaUltimoAcceso(new Timestamp(date.getTime()));
		this.usuarioRepository.save(usuario);
		return userDetails;
	}


}
