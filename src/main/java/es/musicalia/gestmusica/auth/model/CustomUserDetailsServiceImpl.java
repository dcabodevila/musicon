package es.musicalia.gestmusica.auth.model;

import java.sql.Timestamp;
import java.util.*;

import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.accesoartista.AccesoArtistaService;
import es.musicalia.gestmusica.permiso.Permiso;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.rol.TipoRolEnum;
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
	private final AccesoArtistaService accesoArtistaService;

	public CustomUserDetailsServiceImpl(PermisoService permisoService, UsuarioRepository usuarioRepository, AccesoArtistaService accesoArtistaService){
		this.usuarioRepository = usuarioRepository;
		this.permisoService = permisoService;
        this.accesoArtistaService = accesoArtistaService;
    }

	@Override
	@Transactional(readOnly = false)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		final String emailPattern = "^[A-Za-z0-9+_.-]+@(.+)$";
		final String trimmedUsername = username.trim();
		final Usuario usuario = trimmedUsername.matches(emailPattern) ? this.usuarioRepository.findUsuarioActivoByMail(trimmedUsername).orElseThrow( () -> new UsernameNotFoundException(username)) : this.usuarioRepository.findByUsername(trimmedUsername).orElseThrow(() -> new UsernameNotFoundException(username));

		final List<GrantedAuthority> auth = new ArrayList<GrantedAuthority>();


		boolean isUserAdmin = false;
		if (usuario.getRolGeneral()!=null){
			final Set<Permiso> permisos = usuario.getRolGeneral().getPermisos();

			for (Permiso permiso : permisos){
				GrantedAuthority authority = new SimpleGrantedAuthority(permiso.getCodigo());
				auth.add(authority);
			}

			isUserAdmin = TipoRolEnum.ADMIN.name().equals(usuario.getRolGeneral().getNombre());

		}

		final Map<Long, Set<String>> mapPermisosArtista = this.accesoArtistaService.obtenerMapPermisosArtista(usuario.getId());
		final Map<Long, Set<String>> mapPermisosAgencia = this.permisoService.obtenerMapPermisosAgencia(usuario.getId());

		final UserDetails userDetails = new CustomAuthenticatedUser(usuario, true, true, true, true, auth, mapPermisosArtista, mapPermisosAgencia);
		final Date date = new Date();
		usuario.setFechaUltimoAcceso(new Timestamp(date.getTime()));
		this.usuarioRepository.save(usuario);
		return userDetails;
	}


}
