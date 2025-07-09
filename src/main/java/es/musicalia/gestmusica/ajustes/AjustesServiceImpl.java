package es.musicalia.gestmusica.ajustes;

import es.musicalia.gestmusica.agencia.AgenciaRecord;
import es.musicalia.gestmusica.agencia.AgenciaRepository;
import es.musicalia.gestmusica.localizacion.CcaaRepository;
import es.musicalia.gestmusica.tipoartista.TipoArtistaRepository;
import es.musicalia.gestmusica.usuario.Usuario;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import es.musicalia.gestmusica.tipoartista.TipoArtista;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.localizacion.Ccaa;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AjustesServiceImpl implements AjustesService {

	private AjustesRepository ajustesRepository;
	private TipoArtistaRepository tipoArtistaRepository;
	private AgenciaRepository agenciaRepository;
	private CcaaRepository ccaaRepository;

	public AjustesServiceImpl(AjustesRepository ajustesRepository,  TipoArtistaRepository tipoArtistaRepository,  AgenciaRepository agenciaRepository, CcaaRepository ccaaRepository){
		this.ajustesRepository = ajustesRepository;
		this.tipoArtistaRepository = tipoArtistaRepository;
		this.agenciaRepository = agenciaRepository;
		this.ccaaRepository = ccaaRepository;
	}

	@Override
	@Transactional(readOnly = false)
	public Ajustes saveAjustesDto(AjustesDto ajustesDto, Usuario usuario){

		Ajustes ajustes = ajustesDto.getId()!=null? this.ajustesRepository.findById(ajustesDto.getId()).get() : new Ajustes();

		ajustes.setUsuario(usuario);

		if (CollectionUtils.isNotEmpty(ajustesDto.getIdsTipoArtista())){
			ajustes.setTipoArtistas(new HashSet<>(this.tipoArtistaRepository.findAllById(ajustesDto.getIdsTipoArtista())));
		}

		if (CollectionUtils.isNotEmpty(ajustesDto.getIdsAgencias())){
			ajustes.setAgencias(new HashSet<>(this.agenciaRepository.findAllById(ajustesDto.getIdsAgencias())));
		}

		if (CollectionUtils.isNotEmpty(ajustesDto.getIdsComunidades())){
			ajustes.setCcaa(new HashSet<>(this.ccaaRepository.findAllById(ajustesDto.getIdsComunidades())));
		}

		return this.ajustesRepository.save(ajustes);

	}

	@Override
	public AjustesDto getAjustesByIdUsuario(final Long idUsuario){


		AjustesDto ajustesDto = new AjustesDto();
		final Ajustes ajustes = this.ajustesRepository.findAjustesByIdUsuario(idUsuario);

		if (ajustes!=null){
			ajustesDto.setId(ajustes.getId());

			// Convertir los ids de tipoArtistas a una lista
			if (ajustes.getTipoArtistas() != null) {
				ajustesDto.setIdsTipoArtista(
						ajustes.getTipoArtistas()
								.stream()
								.map(TipoArtista::getId) // Suponiendo que TipoArtista tiene un método getId()
								.collect(Collectors.toList())
				);
			}

			// Convertir los ids de agencias a una lista
			if (ajustes.getAgencias() != null) {
				ajustesDto.setIdsAgencias(
						ajustes.getAgencias()
								.stream()
								.map(Agencia::getId) // Suponiendo que Agencia tiene un método getId()
								.collect(Collectors.toList())
				);
			}

			// Convertir los ids de ccaa a una lista
			if (ajustes.getCcaa() != null) {
				ajustesDto.setIdsComunidades(
						ajustes.getCcaa()
								.stream()
								.map(Ccaa::getId) // Suponiendo que Ccaa tiene un método getId()
								.collect(Collectors.toList())
				);
			}


		}
		else {
			//Cargamos todos
			ajustesDto.setIdsTipoArtista(this.tipoArtistaRepository.findAll()
							.stream()
							.map(TipoArtista::getId) // Suponiendo que TipoArtista tiene un método getId()
							.collect(Collectors.toList())
			);

			ajustesDto.setIdsAgencias(this.agenciaRepository.findAllAgenciasOrderedByName().stream()
					.map(AgenciaRecord::id) // Suponiendo que TipoArtista tiene un método getId()
					.collect(Collectors.toList()));
			ajustesDto.setIdsComunidades(this.ccaaRepository.findAll().stream()
					.map(Ccaa::getId) // Suponiendo que TipoArtista tiene un método getId()
					.collect(Collectors.toList()));
		}

		return ajustesDto;



	}

}
