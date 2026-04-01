package es.musicalia.gestmusica.usuario;

import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.auth.model.RegistrationForm;
import es.musicalia.gestmusica.file.FileService;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.mensaje.Mensaje;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.rol.RolRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
	private final ProvinciaRepository provinciaRepository;
	private final MensajeService mensajeService;

	UserServiceImpl(UsuarioRepository userRepository, PasswordEncoder passwordEncoder, CodigoVerificacionService codigoVerificacionService, RolRepository rolRepository, UsuarioMapper usuarioMapper, FileService fileService, EmailService emailService, ProvinciaRepository provinciaRepository, MensajeService mensajeService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.codigoVerificacionService = codigoVerificacionService;
        this.rolRepository = rolRepository;
        this.usuarioMapper = usuarioMapper;
        this.fileService = fileService;
        this.emailService = emailService;
        this.provinciaRepository = provinciaRepository;
        this.mensajeService = mensajeService;
    }



	@Transactional(readOnly = false)
	public Usuario saveRegistration(RegistrationForm registrationForm) throws EmailYaExisteException {

		if (userRepository.existsUsuarioByEmail(registrationForm.getEmail())) {
			throw new EmailYaExisteException("El email ya está registrado");
		}
		Usuario user = registrationFormToUsuario(registrationForm);

		// Si es agencia, validar automáticamente y asignar rol ROL_AGENTE
		if (registrationForm.getTipoUsuario() == TipoUsuarioEnum.AGENCIA) {
			user.setValidado(true);
			user.setRolGeneral(this.rolRepository.findRolByCodigo(RolEnum.ROL_AGENTE.getCodigo()));
		}

		userRepository.save(user);

		try {
			codigoVerificacionService.generarYEnviarCodigo(user.getEmail(), EmailTemplateEnum.REGISTRO);
		} catch (Exception e) {
			log.error("No se pudo enviar el código de verificación a {}: {}", user.getEmail(), e.getMessage(), e);
		}

        final List<Usuario> usuariosAdmin = findUsuariosAdmin();

        for (Usuario admin : usuariosAdmin) {
            try {
                enviarMensajeInternoNuevoUsuario(admin, user);
                this.emailService.enviarMensajePorEmail(admin.getEmail(), EmailTemplateEnum.VALIDAR_USUARIO);
            } catch (EnvioEmailException e) {
                log.error("No se ha podido enviar el correo a {}", admin.getEmail(), e);
            }
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
		user.setNombreComercial(registrationForm.getNombreComercial());
		user.setTelefono(registrationForm.getTelefono());
		user.setProvincia(this.provinciaRepository.findById(registrationForm.getIdProvincia()).orElseThrow());
		user.setActivo(true);
        user.setEmailVerified(false);
		user.setTipoUsuario(registrationForm.getTipoUsuario());
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
		final var authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && !"anonymousUser".equals(authentication.getName());
	}

	@Override
	public List<UsuarioRecord> findAllUsuarioRecordsNotAdmin(){
		return this.userRepository.findAllUsuarioRecordsNotAdmin();
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
        usuario.setEmailVerified(true);
		return userRepository.save(usuario);

	}

	private void enviarMensajeInternoNuevoUsuario(Usuario usuarioAdmin, Usuario nuevoUsuario) {
		Mensaje mensaje = new Mensaje();
		mensaje.setUsuarioRemite(nuevoUsuario);
		mensaje.setUsuarioReceptor(usuarioAdmin);
		mensaje.setAsunto("Nuevo usuario");
		mensaje.setMensaje("Nuevo usuario registrado " + nuevoUsuario.getNombre() + " " + nuevoUsuario.getApellidos());
		mensaje.setImagen("fa-users text-success");
		this.mensajeService.enviarMensaje(mensaje, usuarioAdmin.getId());
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
		return this.userRepository.save(usuario);
	}

    @Override
    public boolean existsUsuarioActivoByEmail(String email) {
        return userRepository.existsUsuarioActivoByEmail(email);
    }


	@Override
	public List<UsuarioAdminListRecord> findAllUsuarioAdminListRecords(){
		return this.userRepository.findAllUsuarioAdminListRecords();
	}


	@Transactional(readOnly = false)
	@Override
	public Usuario validarUsuario(Long id) {
		Usuario usuario = userRepository.findById(id).orElseThrow();
		usuario.setValidado(true);
		if (usuario.getRolGeneral()==null){
			usuario.setRolGeneral(this.rolRepository.findRolByCodigo(RolEnum.ROL_AGENTE.getCodigo()));
		}
		return userRepository.save(usuario);
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
	public UsuarioEdicionDTO getMiPerfil(final Long idUsuario){
		final Usuario usuarioActual = this.userRepository.findById(idUsuario).orElseThrow();
		return this.usuarioMapper.toUsuarioEdicionDTO(usuarioActual);
	}


	@Transactional(readOnly = false)
	@Override
	public Usuario guardarUsuario(UsuarioEdicionDTO usuarioEdicionDTO, MultipartFile multipartFile) throws EmailYaExisteException {

		final String emailNormalizado = usuarioEdicionDTO.getEmail() != null ? usuarioEdicionDTO.getEmail().trim() : null;
		if (emailNormalizado != null && this.userRepository.existsByEmailAndIdNot(emailNormalizado, usuarioEdicionDTO.getId())) {
			throw new EmailYaExisteException("El email ya está registrado por otro usuario");
		}

		final Usuario usuario = this.userRepository.findById(usuarioEdicionDTO.getId()).orElseThrow();
		usuario.setUsername( usuarioEdicionDTO.getUsername() );
		usuario.setNombre( usuarioEdicionDTO.getNombre() );
		usuario.setApellidos( usuarioEdicionDTO.getApellidos() );
		usuario.setEmail(emailNormalizado);
		usuario.setTelefono( usuarioEdicionDTO.getTelefono() );
		usuario.setProvincia(this.provinciaRepository.findById(usuarioEdicionDTO.getIdProvincia()).orElseThrow());
		usuario.setNombreComercial( usuarioEdicionDTO.getNombreComercial() );

		if (multipartFile!=null){
			final String uploadedFile = this.fileService.guardarFichero(multipartFile);

			if (uploadedFile!=null ){
				usuario.setImagen(uploadedFile);
			}
		}

		try {
			return userRepository.save(usuario);
		} catch (DataIntegrityViolationException e) {
			if (esConflictoUnicidadEmail(e)) {
				throw new EmailYaExisteException("El email ya está registrado por otro usuario");
			}
			throw e;
		}
	}

	private boolean esConflictoUnicidadEmail(DataIntegrityViolationException e) {
		Throwable current = e;
		while (current != null) {
			final String message = current.getMessage();
			if (message != null) {
				final String lower = message.toLowerCase();
				if (lower.contains("uk_usuario_email") || lower.contains("usuario_email_key")
					|| (lower.contains("duplicate key") && lower.contains("email"))) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}

	@Override
	public List<Usuario> findUsuariosAdmin() {
		return userRepository.findUsuariosByRolGeneralCodigo(RolEnum.ROL_ADMINISTRADOR.getCodigo());
	}
    @Override
    public Usuario findUsuarioById(Long idUsuario){
        return userRepository.findById(idUsuario).orElseThrow();
    }

	@Override
	public Usuario findById(Long id) {
		return userRepository.findById(id).orElseThrow();
	}

}
