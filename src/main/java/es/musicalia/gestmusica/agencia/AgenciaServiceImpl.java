package es.musicalia.gestmusica.agencia;


import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.contacto.ContactoRepository;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.localizacion.*;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.rol.RolRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.util.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
public class AgenciaServiceImpl implements AgenciaService {

	private final AgenciaRepository agenciaRepository;
	private final MunicipioRepository municipioRepository;
	private final ProvinciaRepository provinciaRepository;
	private final UsuarioRepository usuarioRepository;
	private final ContactoRepository agenciaContactoRepository;
	private final AccesoService accesoService;
	private final RolRepository rolRepository;

	public AgenciaServiceImpl(AgenciaRepository agenciaRepository, MunicipioRepository municipioRepository, ProvinciaRepository provinciaRepository, UsuarioRepository usuarioRepository, ContactoRepository agenciaContactoRepository, AccesoService accesoService, RolRepository rolRepository){
		this.agenciaRepository = agenciaRepository;
		this.municipioRepository = municipioRepository;
		this.provinciaRepository = provinciaRepository;
		this.usuarioRepository = usuarioRepository;
		this.agenciaContactoRepository = agenciaContactoRepository;
        this.accesoService = accesoService;
        this.rolRepository = rolRepository;
    }
	@Override
	public List<AgenciaDto> findAllAgenciasForUser(final Usuario usuario){

		List<Agencia> agencias = agenciaRepository.findAllAgenciasOrderedByName();
		if (agencias == null || agencias.isEmpty()) {
			return Collections.emptyList();
		}
		return agencias.stream()
				.map(this::getAgenciaDto)
				.collect(Collectors.toList());
	}

	@Override
	public List<AgenciaDto> findMisAgencias(Set<Long> idsMisAgencias){



		List<Agencia> agencias = agenciaRepository.findAllAgenciasByIds(idsMisAgencias);
		if (agencias == null || agencias.isEmpty()) {
			return Collections.emptyList();
		}
		return agencias.stream()
				.map(this::getAgenciaDto)
				.collect(Collectors.toList());
	}

	@Override
	public List<AgenciaDto> findOtrasAgencias(Set<Long> idsMisAgencias){

		List<Agencia> agencias = agenciaRepository.findAllAgenciasNotByIds(idsMisAgencias);
		if (agencias == null || agencias.isEmpty()) {
			return Collections.emptyList();
		}
		return agencias.stream()
				.map(this::getAgenciaDto)
				.collect(Collectors.toList());
	}

	@Override
	public AgenciaDto findAgenciaDtoById(Long idAgencia){
		return getAgenciaDto(this.agenciaRepository.findById(idAgencia).orElseThrow());
	}

	private AgenciaDto getAgenciaDto(Agencia agencia) {
		ModelMapper modelMapper = new ModelMapper();
		AgenciaDto agenciaDto = modelMapper.map(agencia, AgenciaDto.class);
		agenciaDto.setNombreUsuario(agencia.getUsuario().getNombre().concat(" ").concat(agencia.getUsuario().getApellidos()));
		final Contacto agenciaContacto = agencia.getAgenciaContacto();

		if (agenciaContacto!=null){
			agenciaDto.setEmail(agenciaContacto.getEmail());
			agenciaDto.setFacebook(agenciaContacto.getFax());
			agenciaDto.setWeb(agenciaContacto.getWeb());
			agenciaDto.setInstagram(agenciaContacto.getInstagram());
			agenciaDto.setFacebook(agenciaContacto.getFacebook());
			agenciaDto.setTelefono(agenciaContacto.getTelefono());
			agenciaDto.setTelefono2(agenciaContacto.getTelefono2());
			agenciaDto.setTelefono3(agenciaContacto.getTelefono3());
			agenciaDto.setYoutube(agenciaContacto.getYoutube());

		}
		return agenciaDto;
	}

	@Override
	public List<AgenciaRecord> listaAgenciasRecordActivasTarifasPublicas(){
		return this.agenciaRepository.findAllAgenciasRecordActivasTarifasPublicasByIdUsuario();
	}

	@Override
	@Transactional(readOnly = false)
	public Agencia saveAgencia(AgenciaDto agenciaDto){
		Agencia agencia = newAgencia(agenciaDto.getId());

		agencia.setNombre(agenciaDto.getNombre());
		agencia.setDescripcion(agenciaDto.getDescripcion());
		agencia.setDireccion(agenciaDto.getDireccion());
		agencia.setCif(agenciaDto.getCif());
		agencia.setLocalidad(agenciaDto.getLocalidad());

		if (agenciaDto.getIdMunicipio()!=null){
			final Optional<Municipio> optionalMunicipio = this.municipioRepository.findById(agenciaDto.getIdMunicipio());
			if (optionalMunicipio.isPresent()){
				agencia.setMunicipio(optionalMunicipio.get());
			}
		}

		if (agenciaDto.getIdProvincia()!=null){
			final Optional<Provincia> optionalProvincia = this.provinciaRepository.findById(agenciaDto.getIdProvincia());
			if (optionalProvincia.isPresent()){
				agencia.setProvincia(optionalProvincia.get());
			}
		}

		if (agenciaDto.getIdUsuario()!=null){
			final Optional<Usuario> optionalUsuario = this.usuarioRepository.findById(agenciaDto.getIdUsuario());
			if (optionalUsuario.isPresent()){
				agencia.setUsuario(optionalUsuario.get());
			}
		}

		if (agenciaDto.getActivo()!=null){
			agencia.setActivo(agenciaDto.getActivo());
		}
		if (agenciaDto.getTarifasPublicas()!=null) {
			agencia.setTarifasPublicas(agenciaDto.getTarifasPublicas());
		}

		if (agenciaDto.getLogo()!=null && !agenciaDto.getLogo().isEmpty()){
			agencia.setLogo(agenciaDto.getLogo());
		}

		Contacto agenciaContacto = agencia.getAgenciaContacto() != null ? agencia.getAgenciaContacto() : new Contacto();
		agenciaContacto.setFacebook(StringUtils.removeHttp(agenciaDto.getFacebook()));
		agenciaContacto.setEmail(agenciaDto.getEmail());
		agenciaContacto.setFax(agenciaDto.getFax());
		agenciaContacto.setTelefono(agenciaDto.getTelefono());
		agenciaContacto.setTelefono2(agenciaDto.getTelefono2());
		agenciaContacto.setTelefono3(agenciaDto.getTelefono3());
		agenciaContacto.setInstagram(StringUtils.removeHttp(agenciaDto.getInstagram()));
		agenciaContacto.setYoutube(StringUtils.removeHttp(agenciaDto.getYoutube()));
		agenciaContacto.setWeb(StringUtils.removeHttp(agenciaDto.getWeb()));
		agenciaContacto = this.agenciaContactoRepository.save(agenciaContacto);
		agencia.setAgenciaContacto(agenciaContacto);

		agencia = this.agenciaRepository.save(agencia);

		this.accesoService.crearAccesoUsuarioAgenciaRol(agencia.getUsuario().getId(), agencia.getId(), this.rolRepository.findRolByCodigo(RolEnum.ROL_AGENCIA.getCodigo()).id(), null);

		return agencia;

	}

	private Agencia newAgencia(Long idAgencia) {

		if (idAgencia!=null) {
			final Optional<Agencia> agenciaSearched = this.agenciaRepository.findById(idAgencia);
			return agenciaSearched.isPresent() ? agenciaSearched.get() : new Agencia();
		}
		return new Agencia();

	}




}
