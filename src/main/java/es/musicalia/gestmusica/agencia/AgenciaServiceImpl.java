package es.musicalia.gestmusica.agencia;


import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.ajustes.Ajustes;
import es.musicalia.gestmusica.ajustes.AjustesRepository;
import es.musicalia.gestmusica.contacto.ContactoRepository;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.localizacion.*;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.rol.RolRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.util.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


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
	private final AgenciaMapper agenciaMapper;
	private final AjustesRepository ajustesRepository;

	public AgenciaServiceImpl(AgenciaRepository agenciaRepository, MunicipioRepository municipioRepository, ProvinciaRepository provinciaRepository, UsuarioRepository usuarioRepository, ContactoRepository agenciaContactoRepository, AccesoService accesoService, RolRepository rolRepository, AgenciaMapper agenciaMapper, AjustesRepository ajustesRepository){
		this.agenciaRepository = agenciaRepository;
		this.municipioRepository = municipioRepository;
		this.provinciaRepository = provinciaRepository;
		this.usuarioRepository = usuarioRepository;
		this.agenciaContactoRepository = agenciaContactoRepository;
        this.accesoService = accesoService;
        this.rolRepository = rolRepository;
        this.agenciaMapper = agenciaMapper;
        this.ajustesRepository = ajustesRepository;
    }
	@Override
	public List<AgenciaRecord> findAllAgenciasForUser(){

		return agenciaRepository.findAllAgenciasOrderedByName();

	}

	@Override
	public AgenciaRecord findAgenciaRecordById(Long idAgencia){
		return this.agenciaRepository.findAgenciaRecordById(idAgencia);
	}

	@Override
	public List<AgenciaRecord> findMisAgencias(Set<Long> idsMisAgencias){

		return agenciaRepository.findAllAgenciasByIds(idsMisAgencias);
	}

	@Override
	public List<AgenciaRecord> findOtrasAgencias(Set<Long> idsMisAgencias){

		return agenciaRepository.findAllAgenciasNotByIds(idsMisAgencias);
	}

	@Override
	public AgenciaDto findAgenciaDtoById(Long idAgencia){
		return this.agenciaMapper.toDto(this.agenciaRepository.findById(idAgencia).orElseThrow());
	}

	private AgenciaDto getAgenciaDto(Agencia agencia) {
		ModelMapper modelMapper = new ModelMapper();
		AgenciaDto agenciaDto = modelMapper.map(agencia, AgenciaDto.class);
		agenciaDto.setNombreUsuario(agencia.getUsuario().getNombre().concat(" ").concat(agencia.getUsuario().getApellidos()));
		final Contacto agenciaContacto = agencia.getAgenciaContacto();

		if (agenciaContacto!=null){
			agenciaDto.setEmail(agenciaContacto.getEmail());
			agenciaDto.setFacebook(agenciaContacto.getFacebook());
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
			final Usuario usuario = this.usuarioRepository.findById(agenciaDto.getIdUsuario()).orElseThrow();
			usuario.setRolGeneral(this.rolRepository.findRolByCodigo(RolEnum.ROL_AGENCIA.getCodigo()));
			agencia.setUsuario(usuario);
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
		agenciaContacto.setTelefono(agenciaDto.getTelefono());
		agenciaContacto.setTelefono2(agenciaDto.getTelefono2());
		agenciaContacto.setTelefono3(agenciaDto.getTelefono3());
		agenciaContacto.setInstagram(StringUtils.removeHttp(agenciaDto.getInstagram()));
		agenciaContacto.setYoutube(StringUtils.removeHttp(agenciaDto.getYoutube()));
		agenciaContacto.setWeb(StringUtils.removeHttp(agenciaDto.getWeb()));
		agenciaContacto = this.agenciaContactoRepository.save(agenciaContacto);
		agencia.setAgenciaContacto(agenciaContacto);

		agencia = this.agenciaRepository.save(agencia);

		this.accesoService.crearAccesoUsuarioAgenciaRol(agencia.getUsuario().getId(), agencia.getId(), this.rolRepository.findRolRecordByCodigo(RolEnum.ROL_AGENCIA.getCodigo()).id(), null);

		asignarAgenciaAjustesListadosPorComunidad((agenciaDto.getId()==null), agencia);


		return agencia;

	}

	private void asignarAgenciaAjustesListadosPorComunidad(boolean isCreacion, Agencia agencia) {
		if (isCreacion) {
			Provincia provincia = agencia.getProvincia();
			if (provincia != null && provincia.getCcaa() != null) {
				List<Ajustes> ajustesList = this.ajustesRepository.findAjustesByCcaaId(provincia.getCcaa().getId());
				for (Ajustes ajustes : ajustesList) {
					ajustes.getAgencias().add(agencia);
					this.ajustesRepository.save(ajustes);
				}
			}

		}
	}

	private Agencia newAgencia(Long idAgencia) {

		if (idAgencia!=null) {
			final Optional<Agencia> agenciaSearched = this.agenciaRepository.findById(idAgencia);
			return agenciaSearched.isPresent() ? agenciaSearched.get() : new Agencia();
		}
		return new Agencia();

	}




}