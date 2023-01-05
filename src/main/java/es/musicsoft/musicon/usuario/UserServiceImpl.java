package es.musicsoft.musicon.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.musicsoft.musicon.auth.model.CustomAuthenticatedUser;
import es.musicsoft.musicon.auth.model.RegistrationForm;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

	@Autowired
	private UsuarioRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	public Usuario save(Usuario usuario) {
		usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
		return userRepository.save(usuario);
	}

	@Transactional(readOnly = false)
	public Usuario saveRegistration(RegistrationForm registrationForm) {

		final Usuario user = registrationFormToUsuario(registrationForm);
		return userRepository.saveAndFlush(user);
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

	public Usuario findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	@Transactional(readOnly = false)
	public Usuario changePassword(String userName, String pwd) {
		final Usuario usuario = this.userRepository.findByUsername(userName);
		usuario.setPassword(passwordEncoder.encode(pwd));
		return this.userRepository.save(usuario);
	}

	public Usuario findByToken(String token) {
		return userRepository.findByToken(token);
	}

	public boolean isUserAutheticated() {
		return !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser");
	}

	public UserDetails obtenerUserDetails() {
		Object userDetails = SecurityContextHolder.getContext().getAuthentication().getDetails();
		if (userDetails instanceof UserDetails) {
			return ((UserDetails) userDetails);
		}
		return null;

	}


}
