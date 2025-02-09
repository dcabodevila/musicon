package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.auth.model.RegistrationForm;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface UserService {
	Usuario save(Usuario usuario);

	Usuario findByUsername(String login);

	Usuario findByToken(String token);

	boolean isUserAutheticated();

	UserDetails obtenerUserDetails();

	Usuario changePassword(String userName, String pwd);

	Usuario saveRegistration(RegistrationForm registrationForm);

	List<UsuarioRecord> findAllUsuarioRecords();

	Usuario obtenerUsuarioAutenticado();

	boolean usernameExists(final String username);
}
