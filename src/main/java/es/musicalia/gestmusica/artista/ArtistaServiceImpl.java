package es.musicalia.gestmusica.artista;


import es.musicalia.gestmusica.acceso.Acceso;
import es.musicalia.gestmusica.acceso.AccesoRepository;
import es.musicalia.gestmusica.acceso.AccesoService;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.contacto.ContactoRepository;
import es.musicalia.gestmusica.localizacion.*;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.rol.RolRepository;
import es.musicalia.gestmusica.tipoartista.TipoArtista;
import es.musicalia.gestmusica.tipoartista.TipoArtistaRepository;
import es.musicalia.gestmusica.tipoescenario.TipoEscenarioRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.util.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
public class ArtistaServiceImpl implements ArtistaService {

	private final ArtistaRepository artistaRepository;
	private final UsuarioRepository usuarioRepository;
	private final ContactoRepository agenciaContactoRepository;
	private final TipoEscenarioRepository tipoEscenarioRepository;
	private final TipoArtistaRepository tipoArtistaRepository;
	private final CcaaRepository ccaaRepository;
	private final AgenciaRepository agenciaRepository;
	private final AccesoService accesoService;
	private final RolRepository rolRepository;
	private final AccesoRepository accesoRepository;

	public ArtistaServiceImpl(ArtistaRepository artistaRepository, UsuarioRepository usuarioRepository, ContactoRepository agenciaContactoRepository,
                              TipoEscenarioRepository tipoEscenarioRepository,
                              TipoArtistaRepository tipoArtistaRepository,
                              CcaaRepository ccaaRepository, AgenciaRepository agenciaRepository, AccesoService accesoService, RolRepository rolRepository, AccesoRepository accesoRepository){
		this.artistaRepository = artistaRepository;
		this.usuarioRepository = usuarioRepository;
		this.agenciaContactoRepository = agenciaContactoRepository;
		this.tipoEscenarioRepository = tipoEscenarioRepository;
		this.tipoArtistaRepository = tipoArtistaRepository;
		this.ccaaRepository = ccaaRepository;
		this.agenciaRepository = agenciaRepository;

        this.accesoService = accesoService;
        this.rolRepository = rolRepository;
        this.accesoRepository = accesoRepository;
    }

	@Override
	public List<ArtistaDto> findAllArtistasForUser(final Usuario usuario){
		return getArtistaDtos(this.artistaRepository.findAllArtistasOrderedByName());
	}
	@Override
	public List<ArtistaDto> findMisArtistas(Set<Long> idsMisArtistas) {
		return getArtistaDtos(this.artistaRepository.findMisArtistas(idsMisArtistas));
	}
	@Override
	public List<ArtistaDto> findOtrosArtistas(Set<Long> idsMisArtistas) {
		return getArtistaDtos(this.artistaRepository.findOtrosArtistas(idsMisArtistas));
	}


	@Override
	public List<ArtistaDto> findAllArtistasByAgenciaId(final Long idAgencia){
		return getArtistaDtos(this.artistaRepository.findAllArtistasByIdAgencia(idAgencia));
	}

	private static List<ArtistaDto> getArtistaDtos(List<Artista> listaArtistas) {
		List<ArtistaDto> listaArtistasDto = new ArrayList<>();

		if (listaArtistas !=null){
			for (Artista artista : listaArtistas){
				listaArtistasDto.add(getArtistaDto(artista));
			}
		}
		return listaArtistasDto;
	}
	@Override
	public ArtistaDto findArtistaDtoById(Long idArtista){
		return getArtistaDto(this.artistaRepository.findById(idArtista).get());
	}

	private static ArtistaDto getArtistaDto(Artista artista) {
		ModelMapper modelMapper = new ModelMapper();
		ArtistaDto artistaDto = modelMapper.map(artista, ArtistaDto.class);
		artistaDto.setNombreUsuario(artista.getUsuario().getNombreCompleto());

		if (CollectionUtils.isNotEmpty(artista.getTiposArtista())) {
			artistaDto.getIdsTipoArtista().addAll(
					artista.getTiposArtista().stream()
							.map(TipoArtista::getId)
							.collect(Collectors.toList())
			);
		}



		final Contacto contacto = artista.getContacto();

		if (contacto!=null){
			artistaDto.setEmail(contacto.getEmail());
			artistaDto.setFacebook(contacto.getFax());
			artistaDto.setWeb(contacto.getWeb());
			artistaDto.setInstagram(contacto.getInstagram());
			artistaDto.setFacebook(contacto.getFacebook());
			artistaDto.setTelefono(contacto.getTelefono());
			artistaDto.setTelefono2(contacto.getTelefono2());
			artistaDto.setTelefono3(contacto.getTelefono3());
			artistaDto.setYoutube(contacto.getYoutube());
		}
		return artistaDto;
	}

    @Override
    @Transactional(readOnly = false)
    public Artista saveArtista(ArtistaDto artistaDto) {
        Artista artista = findOrCreateArtista(artistaDto.getId());

        // Actualizar datos básicos
        actualizarDatosBasicos(artista, artistaDto);

        // Actualizar tipos de artista
        actualizarTiposArtista(artista, artistaDto.getIdsTipoArtista());

        // Actualizar contacto
        Contacto contacto = actualizarContacto(artista.getContacto(), artistaDto);
        contacto = agenciaContactoRepository.save(contacto);
        artista.setContacto(contacto);

		artista = artistaRepository.save(artista);
        // Crear accesos si es nuevo artista
        if (artistaDto.getId() == null) {
            crearAccesosUsuarioArtista(artista.getUsuario(), artistaDto.getIdAgencia(), artista.getId());
        }

        return artista;
    }

    private void actualizarDatosBasicos(Artista artista, ArtistaDto dto) {
        artista.setNombre(dto.getNombre());
        artista.setUsuario(usuarioRepository.findById(dto.getIdUsuario()).orElseThrow());
        artista.setActivo(dto.getActivo());
        Optional.ofNullable(dto.getLogo())
                .filter(logo -> !logo.isEmpty())
                .ifPresent(artista::setLogo);
        artista.setCcaa(ccaaRepository.findById(dto.getIdCcaa()).orElseThrow());
        artista.setAgencia(agenciaRepository.findById(dto.getIdAgencia()).orElseThrow());
        artista.setEscenario(dto.isEscenario());
        Optional.ofNullable(dto.getIdTipoEscenario())
                .ifPresent(id -> artista.setTipoEscenario(tipoEscenarioRepository.findById(id).orElseThrow()));
        artista.setComponentes(dto.getComponentes());
        artista.setMedidasEscenario(dto.getMedidasEscenario());
        artista.setRitmo(dto.getRitmo());
        artista.setViento(dto.getViento());
        artista.setBailarinas(dto.getBailarinas());
        artista.setSolistas(dto.getSolistas());
        artista.setLuz(dto.getLuz());
        artista.setSonido(dto.getSonido());
        artista.setTarifasPublicas(dto.getTarifasPublicas());
    }

    private void actualizarTiposArtista(Artista artista, List<Long> idsTipoArtista) {
        if (CollectionUtils.isNotEmpty(idsTipoArtista)) {
            if (artista.getTiposArtista() == null) {
                artista.setTiposArtista(new HashSet<>());
            }
            idsTipoArtista.forEach(id ->
                    artista.getTiposArtista().add(tipoArtistaRepository.findById(id).orElseThrow())
            );
        }
    }

    private Contacto actualizarContacto(Contacto contacto, ArtistaDto dto) {
        contacto = contacto != null ? contacto : new Contacto();
        contacto.setFacebook(StringUtils.removeHttp(dto.getFacebook()));
        contacto.setEmail(dto.getEmail());
        contacto.setFax(dto.getFax());
        contacto.setTelefono(dto.getTelefono());
        contacto.setTelefono2(dto.getTelefono2());
        contacto.setTelefono3(dto.getTelefono3());
        contacto.setInstagram(StringUtils.removeHttp(dto.getInstagram()));
        contacto.setYoutube(StringUtils.removeHttp(dto.getYoutube()));
        contacto.setWeb(StringUtils.removeHttp(dto.getWeb()));
        return contacto;
    }

    private void crearAccesosUsuarioArtista(Usuario usuario, Long idAgencia, Long idArtista) {

        accesoService.crearAccesoUsuarioAgenciaRol(usuario.getId(), idAgencia, rolRepository.findRolByCodigo(RolEnum.ROL_ARTISTA.getCodigo()).id(), idArtista);

        Set<String> rolesPermitidos = Set.of(
                RolEnum.ROL_REPRESENTANTE.getCodigo(),
                RolEnum.ROL_AGENCIA.getCodigo()
        );

		Optional<List<Acceso>> accesosUsuario = accesoRepository.findAllAccesosByAndIdAgenciaAndCodigoRolAndActivo(rolesPermitidos, idAgencia);

		accesosUsuario.ifPresent(accesos ->
				accesos.forEach(acceso -> accesoService.guardarPermisosArtistas(acceso, idArtista))
		);
    }
	@Override
	public List<CodigoNombreDto> listaTipoEscenario(){
		return this.tipoEscenarioRepository.findAll().stream()
				.map(tipo -> {
					return new CodigoNombreDto(tipo.getId(), tipo.getNombre());
				})
				.collect(Collectors.toList());

	}
	@Override
	public List<CodigoNombreDto> listaTipoArtista(){

		return this.tipoArtistaRepository.findAll().stream()
				.map(tipo -> {
					return new CodigoNombreDto(tipo.getId(), tipo.getNombre());
				})
				.collect(Collectors.toList());
	}

	@Override
	public List<ArtistaRecord> listaArtistaRecordByIdAgencia(Long idAgencia){
		return this.artistaRepository.findAllArtistasRecordByIdAgencia(idAgencia);
	}


    private Artista findOrCreateArtista(Long idArtista) {
        return Optional.ofNullable(idArtista)
                .flatMap(artistaRepository::findById)
                .orElse(new Artista());
    }


}
