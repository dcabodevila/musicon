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
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	public List<Artista> findAllArtistasForUser(final Usuario usuario){
		return this.artistaRepository.findAllArtistasOrderedByName();
	}

	public ArtistaDto findArtistaDtoById(Long idArtista){
		final Artista artista = this.artistaRepository.findById(idArtista).get();

		ModelMapper modelMapper = new ModelMapper();
		ArtistaDto agenciaDto = modelMapper.map(artista, ArtistaDto.class);

		final Contacto agenciaContacto = artista.getContacto();

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
	public Artista saveArtista(ArtistaDto artistaDto){

		Artista artista = newArtista(artistaDto.getId());
		artista.setNombre(artistaDto.getNombre());


		if (artistaDto.getIdUsuario()!=null){
			final Optional<Usuario> optionalUsuario = this.usuarioRepository.findById(artistaDto.getIdUsuario());
			if (optionalUsuario.isPresent()){
				artista.setUsuario(optionalUsuario.get());
			}
		}

		if (artistaDto.getActivo()!=null){
			artista.setActivo(artistaDto.getActivo());
		}

		if (artistaDto.getLogo()!=null && !artistaDto.getLogo().isEmpty()){
			artista.setLogo(artistaDto.getLogo());
		}

		artista.setCcaa(this.ccaaRepository.findById(artistaDto.getIdCcaa()).get());
		artista.setAgencia(this.agenciaRepository.findById(artistaDto.getIdAgencia()).get());


		Contacto artistaContacto = artista.getContacto() != null ? artista.getContacto() : new Contacto();
		artistaContacto.setFacebook(artistaDto.getFacebook());
		artistaContacto.setEmail(artistaDto.getEmail());
		artistaContacto.setFax(artistaDto.getFax());
		artistaContacto.setTelefono(artistaDto.getTelefono());
		artistaContacto.setInstagram(artistaDto.getInstagram());
		artistaContacto.setWeb(artistaDto.getWeb());
		artistaContacto = this.agenciaContactoRepository.save(artistaContacto);
		artista.setContacto(artistaContacto);

		return this.artistaRepository.save(artista);

	}

	public List<TipoEscenario> listaTipoEscenario(){
		return this.tipoEscenarioRepository.findAll();
	}
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
