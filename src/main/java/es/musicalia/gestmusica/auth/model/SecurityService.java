package es.musicalia.gestmusica.auth.model;

public interface SecurityService {
	String findLoggedInUsername();

	void autoLogin(String username, String password);
}
