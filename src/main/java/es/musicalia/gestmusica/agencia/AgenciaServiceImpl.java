package es.musicalia.gestmusica.agencia;


import es.musicalia.gestmusica.contacto.ContactoRepository;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



@Service
@Transactional(readOnly = true)
public class AgenciaServiceImpl implements AgenciaService {

	private AgenciaRepository agenciaRepository;
	private MunicipioRepository municipioRepository;
	private ProvinciaRepository provinciaRepository;
	private UsuarioRepository usuarioRepository;

	private ContactoRepository agenciaContactoRepository;
	public AgenciaServiceImpl(AgenciaRepository agenciaRepository, MunicipioRepository municipioRepository, ProvinciaRepository provinciaRepository, UsuarioRepository usuarioRepository, ContactoRepository agenciaContactoRepository){
		this.agenciaRepository = agenciaRepository;
		this.municipioRepository = municipioRepository;
		this.provinciaRepository = provinciaRepository;
		this.usuarioRepository = usuarioRepository;
		this.agenciaContactoRepository = agenciaContactoRepository;
	}

	public List<AgenciaDto> findAllAgenciasForUser(final Usuario usuario){

		final boolean isUsuarioRolAdministrador = usuario.getRol()!=null && RolEnum.ROL_ADMINISTRADOR.getId().equals(usuario.getRol().getId());

		List<Agencia> listaAgencias = isUsuarioRolAdministrador ? this.agenciaRepository.findAllAgenciasOrderedByName() : this.agenciaRepository.findAllAgenciasByIdUsuario(usuario.getId());
		List<AgenciaDto> listaAgenciasDto = new ArrayList<>();

		if (listaAgencias!=null && !listaAgencias.isEmpty()){
			for (Agencia agencia : listaAgencias){
				listaAgenciasDto.add(getAgenciaDto(agencia));
			}
		}

		return listaAgenciasDto;

	}


	public AgenciaDto findAgenciaDtoById(Long idAgencia){
		final Agencia agencia = this.agenciaRepository.findById(idAgencia).orElseThrow();

		AgenciaDto agenciaDto = getAgenciaDto(agencia);

		return agenciaDto;

	}

	private static AgenciaDto getAgenciaDto(Agencia agencia) {
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
		}
		return agenciaDto;
	}

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
		agenciaContacto.setFacebook(removeHttp(agenciaDto.getFacebook()));
		agenciaContacto.setEmail(agenciaDto.getEmail());
		agenciaContacto.setFax(agenciaDto.getFax());
		agenciaContacto.setTelefono(agenciaDto.getTelefono());
		agenciaContacto.setInstagram(removeHttp(agenciaDto.getInstagram()));
		agenciaContacto.setWeb(removeHttp(agenciaDto.getWeb()));
		agenciaContacto = this.agenciaContactoRepository.save(agenciaContacto);
		agencia.setAgenciaContacto(agenciaContacto);

		return this.agenciaRepository.save(agencia);

	}

	private Agencia newAgencia(Long idAgencia) {

		if (idAgencia!=null) {
			final Optional<Agencia> agenciaSearched = this.agenciaRepository.findById(idAgencia);
			return agenciaSearched.isPresent() ? agenciaSearched.get() : new Agencia();
		}
		return new Agencia();

	}


	public static String removeHttp(String input) {
		if (input.startsWith("http://")) {
			return input.substring(7);
		}
		else if (input.startsWith("https://")) {
			return input.substring(8);
		}
		return input;
	}

}
