package es.musicalia.gestmusica.artista;


import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.contacto.Contacto;
import es.musicalia.gestmusica.contacto.ContactoRepository;
import es.musicalia.gestmusica.localizacion.*;
import es.musicalia.gestmusica.tipoartista.TipoArtista;
import es.musicalia.gestmusica.tipoartista.TipoArtistaRepository;
import es.musicalia.gestmusica.tipoescenario.TipoEscenario;
import es.musicalia.gestmusica.tipoescenario.TipoEscenarioRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.usuario.UsuarioRepository;
import es.musicalia.gestmusica.util.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



@Service
@Transactional(readOnly = true)
public class ArtistaServiceImpl implements ArtistaService {

	private ArtistaRepository artistaRepository;
	private UsuarioRepository usuarioRepository;
	private ContactoRepository agenciaContactoRepository;
	private TipoEscenarioRepository tipoEscenarioRepository;
	private TipoArtistaRepository tipoArtistaRepository;
	private CcaaRepository ccaaRepository;
	private AgenciaRepository agenciaRepository;
	public ArtistaServiceImpl(ArtistaRepository artistaRepository, UsuarioRepository usuarioRepository, ContactoRepository agenciaContactoRepository,
							  TipoEscenarioRepository tipoEscenarioRepository,
							  TipoArtistaRepository tipoArtistaRepository,
							  CcaaRepository ccaaRepository, AgenciaRepository agenciaRepository){
		this.artistaRepository = artistaRepository;
		this.usuarioRepository = usuarioRepository;
		this.agenciaContactoRepository = agenciaContactoRepository;
		this.tipoEscenarioRepository = tipoEscenarioRepository;
		this.tipoArtistaRepository = tipoArtistaRepository;
		this.ccaaRepository = ccaaRepository;
		this.agenciaRepository = agenciaRepository;

	}

	@Override
	public List<ArtistaDto> findAllArtistasForUser(final Usuario usuario){
		return getArtistaDtos(this.artistaRepository.findAllArtistasOrderedByName());
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
	public Artista saveArtista(ArtistaDto artistaDto){

		Artista artista = newArtista(artistaDto.getId());
		artista.setNombre(artistaDto.getNombre());

		if (artistaDto.getIdUsuario()!=null){
			final Optional<Usuario> optionalUsuario = this.usuarioRepository.findById(artistaDto.getIdUsuario());
			if (optionalUsuario.isPresent()){
				artista.setUsuario(optionalUsuario.get());
			}
		}


		artista.setActivo(artistaDto.getActivo());

		if (artistaDto.getLogo()!=null && !artistaDto.getLogo().isEmpty()){
			artista.setLogo(artistaDto.getLogo());
		}

		artista.setCcaa(this.ccaaRepository.findById(artistaDto.getIdCcaa()).get());
		artista.setAgencia(this.agenciaRepository.findById(artistaDto.getIdAgencia()).get());

		artista.setEscenario(artistaDto.isEscenario());
		if (artistaDto.getIdTipoEscenario()!=null){
			artista.setTipoEscenario(this.tipoEscenarioRepository.findById(artistaDto.getIdTipoEscenario()).get());
		}
		artista.setTipoArtista(this.tipoArtistaRepository.findById(artistaDto.getIdTipoArtista()).get());
		artista.setComponentes(artistaDto.getComponentes());
		artista.setMedidasEscenario(artistaDto.getMedidasEscenario());
		artista.setRitmo(artistaDto.getRitmo());
		artista.setViento(artistaDto.getViento());
		artista.setBailarinas(artistaDto.getBailarinas());
		artista.setSolistas(artistaDto.getSolistas());
		artista.setActivo(artistaDto.getActivo());
		artista.setLuz(artistaDto.getLuz());
		artista.setSonido(artistaDto.getSonido());


		Contacto contacto = artista.getContacto() != null ? artista.getContacto() : new Contacto();
		contacto.setFacebook(StringUtils.removeHttp(artistaDto.getFacebook()));
		contacto.setEmail(artistaDto.getEmail());
		contacto.setFax(artistaDto.getFax());
		contacto.setTelefono(artistaDto.getTelefono());
		contacto.setTelefono2(artistaDto.getTelefono2());
		contacto.setTelefono3(artistaDto.getTelefono3());
		contacto.setInstagram(StringUtils.removeHttp(artistaDto.getInstagram()));
		contacto.setYoutube(StringUtils.removeHttp(artistaDto.getYoutube()));
		contacto.setWeb(StringUtils.removeHttp(artistaDto.getWeb()));
		contacto = this.agenciaContactoRepository.save(contacto);
		artista.setContacto(contacto);

		return this.artistaRepository.save(artista);

	}
	@Override
	public List<TipoEscenario> listaTipoEscenario(){
		return this.tipoEscenarioRepository.findAll();
	}
	@Override
	public List<TipoArtista> listaTipoArtista(){
		return this.tipoArtistaRepository.findAll();
	}
	private Artista newArtista(Long idArtista) {

		if (idArtista!=null) {
			final Optional<Artista> agenciaSearched = this.artistaRepository.findById(idArtista);
			return agenciaSearched.isPresent() ? agenciaSearched.get() : new Artista();
		}
		return new Artista();

	}


}
