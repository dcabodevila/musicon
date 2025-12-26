package es.musicalia.gestmusica.auth.model;

public interface SecurityService {
	String findLoggedInUsername();

	void reloadUserAuthorities();
}
