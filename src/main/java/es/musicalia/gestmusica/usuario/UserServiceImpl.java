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
	public Usuario guardarUsuario(UsuarioEdicionDTO usuarioEdicionDTO, MultipartFile multipartFile) {

		final Usuario usuario = this.userRepository.findById(usuarioEdicionDTO.getId()).orElseThrow();
		usuario.setUsername( usuarioEdicionDTO.getUsername() );
		usuario.setNombre( usuarioEdicionDTO.getNombre() );
		usuario.setApellidos( usuarioEdicionDTO.getApellidos() );
		usuario.setEmail( usuarioEdicionDTO.getEmail() );
		usuario.setTelefono( usuarioEdicionDTO.getTelefono() );
		usuario.setProvincia(this.provinciaRepository.findById(usuarioEdicionDTO.getIdProvincia()).orElseThrow());
		usuario.setNombreComercial( usuarioEdicionDTO.getNombreComercial() );

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
    @Override
    public Usuario findUsuarioById(Long idUsuario){
        return userRepository.findById(idUsuario).orElseThrow();
    }

}
