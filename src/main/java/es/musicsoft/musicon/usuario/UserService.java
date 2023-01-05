package es.musicsoft.musicon.usuario;

import org.springframework.security.core.userdetails.UserDetails;
import es.musicsoft.musicon.auth.model.RegistrationForm;

import java.util.List;

public interface UserService {
	Usuario save(Usuario usuario);

	Usuario findByUsername(String login);

	Usuario findByToken(String token);

	boolean isUserAutheticated();

	UserDetails obtenerUserDetails();

	Usuario changePassword(String userName, String pwd);

	Usuario saveRegistration(RegistrationForm registrationForm);


}
