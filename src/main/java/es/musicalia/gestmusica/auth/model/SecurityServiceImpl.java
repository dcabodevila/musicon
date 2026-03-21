package es.musicalia.gestmusica.auth.model;

import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SecurityServiceImpl implements SecurityService {
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private CustomUserDetailsServiceImpl userDetailsService;
    @Autowired
    private UserService userService;

    @Autowired
    private SessionRegistry sessionRegistry;

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

	@Override
	public void invalidarSesionDeUsuario(Long idUsuario) {
		if (idUsuario == null) {
			log.warn("invalidarSesionDeUsuario: idUsuario es nulo");
			return;
		}

		try {
			int sessionesInvalidadas = 0;
			// Iterar sobre todas las sesiones activas
			for (Object principal : sessionRegistry.getAllPrincipals()) {
				if (principal instanceof CustomAuthenticatedUser) {
					CustomAuthenticatedUser user = (CustomAuthenticatedUser) principal;
					// Si el ID del usuario coincide, invalidar todas sus sesiones
					if (user.getUserId() == idUsuario) {
						for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
							session.expireNow();
							sessionesInvalidadas++;
						}
					}
				}
			}
			log.info("Sesiones invalidadas para usuario ID {}: {} sesión(es)", idUsuario, sessionesInvalidadas);
		} catch (Exception e) {
			log.error("Error invalidando sesiones del usuario {}: {}", idUsuario, e.getMessage(), e);
		}
	}

	@Override
	public void recargarOInvalidarSesion(Long idUsuarioAfectado) {
		if (idUsuarioAfectado == null) {
			log.warn("recargarOInvalidarSesion: idUsuarioAfectado es nulo");
			return;
		}

		try {
			// Obtener usuario autenticado actual
			var usuarioActualOptional = userService.obtenerUsuarioAutenticado();
			if (usuarioActualOptional.isEmpty()) {
				log.warn("No hay usuario autenticado en el contexto");
				return;
			}

			Long idUsuarioActual = usuarioActualOptional.get().getId();

			// Si es el mismo usuario, recargamos sus autoridades
			if (idUsuarioAfectado.equals(idUsuarioActual)) {
				reloadUserAuthorities();
				log.info("Autoridades recargadas para usuario actual ID {}", idUsuarioActual);
			} else {
				// Si es otro usuario, invalidamos su sesión
				invalidarSesionDeUsuario(idUsuarioAfectado);
			}
		} catch (Exception e) {
			log.error("Error en recargarOInvalidarSesion para usuario {}: {}", idUsuarioAfectado, e.getMessage(), e);
		}
	}

	@Override
	public void autologin(String email, HttpServletRequest request) {
		try {
			UserDetails userDetails = userDetailsService.loadUserByUsername(email);

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails,
					null,
					userDetails.getAuthorities()
			);

			SecurityContext securityContext = SecurityContextHolder.getContext();
			securityContext.setAuthentication(authentication);

			HttpSession session = request.getSession(true);
			session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

			sessionRegistry.registerNewSession(session.getId(), userDetails);

			log.info("Autologin exitoso para email: {}", email);
		} catch (Exception e) {
			log.error("Error en autologin para email {}: {}", email, e.getMessage(), e);
			throw new RuntimeException("Error al realizar autologin para: " + email, e);
		}
	}

}
