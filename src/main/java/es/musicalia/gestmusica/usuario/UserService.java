package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.auth.model.RegistrationForm;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface UserService {

	boolean isUserAutheticated();

	Usuario saveRegistration(RegistrationForm registrationForm) throws EmailYaExisteException, EnvioEmailException;

	List<UsuarioRecord> findAllUsuarioRecords();

	List<RepresentanteRecord> findAllRepresentanteRecords();

	@Transactional(readOnly = false)
	Usuario activateUserByEmail(String email) throws UsuarioNoEncontradoException;

	Optional<Usuario> obtenerUsuarioAutenticado();

	boolean usernameExists(final String username);

	@Transactional(readOnly = false)
	Usuario changePasswordByEmail(String email, String newPassword) throws UsuarioNoEncontradoException;

	boolean existsUsuarioByEmail(String email);

	List<UsuarioAdminListRecord> findAllUsuarioAdminListRecords();

	@Transactional(readOnly = false)
	void validarUsuario(Long id);

	@Transactional(readOnly = false)
	void toggleActivarUsuario(Long id);

	UsuarioEdicionDTO getUsuarioEdicionDTO(Long idUsuario);

	UsuarioEdicionDTO getMiPerfil(Usuario usuario);

	@Transactional(readOnly = false)
	Usuario guardarUsuario(UsuarioEdicionDTO usuarioEdicionDTO, MultipartFile multipartFile);

	List<Usuario> findUsuariosAdmin();

}
