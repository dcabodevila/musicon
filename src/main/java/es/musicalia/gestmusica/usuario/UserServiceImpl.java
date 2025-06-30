package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.RegistrationForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

	@Autowired
	private UsuarioRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private CodigoVerificacionService codigoVerificacionService;


	@Override
	public Usuario save(Usuario usuario) {
		usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
		return userRepository.save(usuario);
	}

	@Transactional(readOnly = false)
	public Usuario saveRegistration(RegistrationForm registrationForm) throws EmailYaExisteException {

		if (userRepository.existsUsuarioByEmail(registrationForm.getEmail())) {
			throw new EmailYaExisteException("El email ya está registrado");
		}
		Usuario user = registrationFormToUsuario(registrationForm);
		userRepository.save(user);

		// Generar y enviar código de verificación
		try {
			codigoVerificacionService.generarYEnviarCodigo(
					registrationForm.getEmail(),
					CodigoVerificacion.TipoVerificacion.REGISTRO
			);
			log.info("Código de verificación enviado a: {}", registrationForm.getEmail());
		} catch (Exception e) {
			log.error("Error enviando código de verificación: {}", e.getMessage());
			// Eliminar usuario si no se pudo enviar el código
			userRepository.delete(user);
			throw new RuntimeException("No se pudo enviar el código de verificación. Inténtalo de nuevo.");
		}


		return user;
	}
	
	


	private Usuario registrationFormToUsuario(RegistrationForm registrationForm) {
		Usuario user = new Usuario();
		user.setUsername(registrationForm.getUsername().trim());
		user.setPassword(passwordEncoder.encode(registrationForm.getPassword()));
		user.setNombre(registrationForm.getNombre().trim());
		user.setApellidos(registrationForm.getApellidos());
		user.setActivateKey(UUID.randomUUID().toString());
		user.setEmail(registrationForm.getEmail().trim());
		user.setFechaRegistro(new Timestamp(new Date().getTime()));
		user.setActivo(false);
		return user;
	}

	@Override
	public boolean usernameExists(final String username) {
		return userRepository.existsUsuarioActivoByUsername(username);
	}


	@Transactional(readOnly = false)
	public Usuario changePassword(String userName, String pwd) {
		final Usuario usuario = this.userRepository.findByUsername(userName).orElseThrow(() -> new UsernameNotFoundException(userName));
		usuario.setPassword(passwordEncoder.encode(pwd));
		return this.userRepository.save(usuario);
	}


	@Override
	public boolean isUserAutheticated() {
		return !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser");
	}

	@Override
	public List<UsuarioRecord> findAllUsuarioRecords(){
		return this.userRepository.findAllUsuarioRecords();
	}

	@Transactional(readOnly = false)
	@Override
	public Usuario activateUserByEmail(String email) throws UsuarioNoEncontradoException {
		Usuario usuario = userRepository.findUsuarioByMail(email).orElseThrow(() -> new UsuarioNoEncontradoException("No se encontró usuario con email: " + email));
		usuario.setActivo(true);
		return userRepository.save(usuario);
	}

	@Override
	public Usuario obtenerUsuarioAutenticado() {

		if (isUserAutheticated()) {

			return ((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
					.getUsuario();

		}
		return null;

	}


	@Transactional(readOnly = false)
	@Override
	public Usuario changePasswordByEmail(String email, String newPassword) throws UsuarioNoEncontradoException {
		final Usuario usuario = this.userRepository.findUsuarioByMail(email)
				.orElseThrow(() -> new UsuarioNoEncontradoException("No se encontró usuario con email: " + email));
		usuario.setPassword(passwordEncoder.encode(newPassword));
		usuario.setActivo(true);
		return this.userRepository.save(usuario);
	}

	@Override
	public boolean existsUsuarioByEmail(String email) {
		return userRepository.existsUsuarioByEmail(email);
	}

}
