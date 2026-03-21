package es.musicalia.gestmusica.auth.model;

import jakarta.servlet.http.HttpServletRequest;

public interface SecurityService {
	String findLoggedInUsername();

	void reloadUserAuthorities();

	void invalidarSesionDeUsuario(Long idUsuario);

	void recargarOInvalidarSesion(Long idUsuarioAfectado);

	void autologin(String email, HttpServletRequest request);
}
