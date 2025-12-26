package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class SecurityServiceImpl implements SecurityService {
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private CustomUserDetailsServiceImpl userDetailsService;
    @Autowired
    private UserService userService;

	@Override
	public String findLoggedInUsername() {
		Object userDetails = SecurityContextHolder.getContext().getAuthentication().getDetails();
		if (userDetails instanceof UserDetails) {
			return ((UserDetails) userDetails).getUsername();
		}

		return null;
	}

	@Override
	public void reloadUserAuthorities() {

        final Usuario usuario = userService.obtenerUsuarioAutenticado().get();

        // 1. Cargamos los datos frescos (roles, permisos, etc.)
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getUsername());

        // 2. Creamos el token DIRECTAMENTE.
        // IMPORTANTE: Al pasarle las authorities como tercer parámetro, el token se marca como "authenticated = true" automáticamente.
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null, // No necesitamos la password porque ya confiamos en este usuario
                userDetails.getAuthorities()
        );

        // 3. (Opcional) Copiar los detalles (IP, sesión, etc.) de la autenticación anterior
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null) {
            authentication.setDetails(currentAuth.getDetails());
        }

        // 4. Actualizamos el contexto
        SecurityContextHolder.getContext().setAuthentication(authentication);
	}

}
