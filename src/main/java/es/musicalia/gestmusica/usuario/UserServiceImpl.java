package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.RegistrationForm;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.rol.RolRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.*;
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {


	private final UsuarioRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final CodigoVerificacionService codigoVerificacionService;
	private final RolRepository rolRepository;
	private final UsuarioMapper usuarioMapper;
	private final FileService fileService;
	private final EmailService emailService;

	UserServiceImpl(UsuarioRepository userRepository, PasswordEncoder passwordEncoder, CodigoVerificacionService codigoVerificacionService, RolRepository rolRepository, UsuarioMapper usuarioMapper, FileService fileService, EmailService emailService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.codigoVerificacionService = codigoVerificacionService;
        this.rolRepository = rolRepository;
        this.usuarioMapper = usuarioMapper;
        this.fileService = fileService;
        this.emailService = emailService;
    }



	@Transactional(readOnly = false)
	public Usuario saveRegistration(RegistrationForm registrationForm) throws EmailYaExisteException, EnvioEmailException {

		if (userRepository.existsUsuarioByEmail(registrationForm.getEmail())) {
			throw new EmailYaExisteException("El email ya está registrado");
		}
		Usuario user = registrationFormToUsuario(registrationForm);
		userRepository.save(user);

		// Generar y enviar código de verificación
		try {
			codigoVerificacionService.generarYEnviarCodigo(
					registrationForm.getEmail(),
					EmailTemplateEnum.REGISTRO
			);
			log.info("Código de verificación enviado a: {}", registrationForm.getEmail());
		}
		catch (Exception e) {
			log.error("Error enviando código de verificación: {}", e.getMessage());
			// Eliminar usuario si no se pudo enviar el código
			userRepository.delete(user);
			throw e;
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
	@Override
	public List<RepresentanteRecord> findAllRepresentanteRecords(){
		return this.userRepository.findAllRepresentantesRecords();
	}

	@Transactional(readOnly = false)
	@Override
	public Usuario activateUserByEmail(String email) throws UsuarioNoEncontradoException {
		Usuario usuario = userRepository.findUsuarioByMail(email).orElseThrow(() -> new UsuarioNoEncontradoException("No se encontró usuario con email: " + email));
		usuario.setActivo(true);
		usuario = userRepository.save(usuario);

		final List<Usuario> usuariosAdmin = findUsuariosAdmin();

		for (Usuario admin : usuariosAdmin) {
            try {
                this.emailService.enviarMensajePorEmail(admin.getEmail(), EmailTemplateEnum.VALIDAR_USUARIO);
            } catch (EnvioEmailException e) {
				log.error("No se ha podido enviar el correo a {}", admin.getEmail(), e);
            }
        }



		return usuario;
	}

	@Override
	public Optional<Usuario> obtenerUsuarioAutenticado() {

		return isUserAutheticated() ? Optional.of(((CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUsuario()) : Optional.empty();

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

	@Override
	public List<UsuarioAdminListRecord> findAllUsuarioAdminListRecords(){
		return this.userRepository.findAllUsuarioAdminListRecords();
	}


	@Transactional(readOnly = false)
	@Override
	public void validarUsuario(Long id) {
		Usuario usuario = userRepository.findById(id).orElseThrow();
		usuario.setValidado(true);
		if (usuario.getRolGeneral()==null){
			usuario.setRolGeneral(this.rolRepository.findRolByCodigo(RolEnum.ROL_AGENTE.getCodigo()));
		}
		userRepository.save(usuario);
	}

	@Transactional(readOnly = false)
	@Override
	public void toggleActivarUsuario(Long id) {
		Usuario usuario = userRepository.findById(id).orElseThrow();
		usuario.setActivo(!usuario.isActivo());
		userRepository.save(usuario);
	}

	@Override
	public UsuarioEdicionDTO getUsuarioEdicionDTO(Long idUsuario){
		return this.usuarioMapper.toUsuarioEdicionDTO(this.userRepository.findById(idUsuario).orElseThrow());
	}


	@Override
	public UsuarioEdicionDTO getMiPerfil(final Usuario usuario){

		return this.usuarioMapper.toUsuarioEdicionDTO(usuario);
	}


	@Transactional(readOnly = false)
	@Override
	public Usuario guardarUsuario(UsuarioEdicionDTO usuarioEdicionDTO, MultipartFile multipartFile) {

		final Usuario usuario = this.userRepository.findById(usuarioEdicionDTO.getId()).orElseThrow();
		usuario.setUsername( usuarioEdicionDTO.getUsername() );
		usuario.setNombre( usuarioEdicionDTO.getNombre() );
		usuario.setApellidos( usuarioEdicionDTO.getApellidos() );
		usuario.setEmail( usuarioEdicionDTO.getEmail() );

		if (multipartFile!=null){
			final String uploadedFile = this.fileService.guardarFichero(multipartFile);

			if (uploadedFile!=null ){
				usuario.setImagen(uploadedFile);
			}
		}

		return userRepository.save(usuario);
	}

	@Override
	public List<Usuario> findUsuariosAdmin() {
		return userRepository.findUsuariosByRolGeneralCodigo(RolEnum.ROL_ADMINISTRADOR.getCodigo());
	}

}
