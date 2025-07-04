package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.auth.model.RegistrationForm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserService {
	Usuario save(Usuario usuario);

	boolean isUserAutheticated();

	Usuario saveRegistration(RegistrationForm registrationForm) throws EmailYaExisteException;

	List<UsuarioRecord> findAllUsuarioRecords();

	@Transactional(readOnly = false)
	Usuario activateUserByEmail(String email) throws UsuarioNoEncontradoException;

	Usuario obtenerUsuarioAutenticado();

	boolean usernameExists(final String username);

	@Transactional(readOnly = false)
	Usuario changePasswordByEmail(String email, String newPassword) throws UsuarioNoEncontradoException;

	boolean existsUsuarioByEmail(String email);
}
