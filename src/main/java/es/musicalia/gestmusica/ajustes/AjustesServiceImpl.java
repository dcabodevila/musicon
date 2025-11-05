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
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AjustesServiceImpl implements AjustesService {

	private final AjustesRepository ajustesRepository;
	private final TipoArtistaRepository tipoArtistaRepository;
	private final AgenciaRepository agenciaRepository;
	private final CcaaRepository ccaaRepository;

	public AjustesServiceImpl(AjustesRepository ajustesRepository,  TipoArtistaRepository tipoArtistaRepository,  AgenciaRepository agenciaRepository, CcaaRepository ccaaRepository){
		this.ajustesRepository = ajustesRepository;
		this.tipoArtistaRepository = tipoArtistaRepository;
		this.agenciaRepository = agenciaRepository;
		this.ccaaRepository = ccaaRepository;
	}

    @Override
    @Transactional(readOnly = false)
    public Ajustes saveAjustesDto(AjustesDto ajustesDto, Usuario usuario){
        if (ajustesDto.isPredeterminado()) {
            desmarcarAjustesPreviosPorDefecto(usuario.getId());
        }

        Ajustes ajustes = ajustesDto.getId()!=null? this.ajustesRepository.findById(ajustesDto.getId()).orElseThrow() : new Ajustes();
        ajustes.setNombre(ajustesDto.getNombre());
        ajustes.setUsuario(usuario);
        ajustes.setPredeterminado(ajustesDto.isPredeterminado());

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

    private void desmarcarAjustesPreviosPorDefecto(Long idUsuario) {
        List<Ajustes> ajustesPrevios = this.ajustesRepository.findAllAjustesByIdUsuario(idUsuario);
        ajustesPrevios.forEach(ajustes -> ajustes.setPredeterminado(false));
        this.ajustesRepository.saveAll(ajustesPrevios);
    }

	@Override
	public AjustesDto getAjustesByIdUsuario(final Long idUsuario){


		AjustesDto ajustesDto = new AjustesDto();
		final Ajustes ajustes = this.ajustesRepository.findAjustesPredeterminadoByIdUsuario(idUsuario);

		if (ajustes!=null){
			ajustesDto.setId(ajustes.getId());
            ajustesDto.setNombre(ajustes.getNombre());

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

            ajustesDto.setPredeterminado(ajustes.isPredeterminado());

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

	@Override
	public List<AjustesDto> getAllAjustesByIdUsuario(final Long idUsuario) {
		return this.ajustesRepository.findAllAjustesByIdUsuario(idUsuario)
				.stream()
				.map(this::convertAjustesToDto)
				.collect(Collectors.toList());
	}

    @Override
    public AjustesDto getAjustesById(Long id) {
        final Ajustes ajustes = this.ajustesRepository.findById(id).orElseThrow();
        return convertAjustesToDto(ajustes);
    }

    @Override
    public AjustesDto getAjustesByIdAjuste(final Long idAjuste) {
		final Ajustes ajustes = this.ajustesRepository.findById(idAjuste).orElseThrow();
		return convertAjustesToDto(ajustes);
	}

	private AjustesDto convertAjustesToDto(final Ajustes ajustes) {
		AjustesDto ajustesDto = new AjustesDto();
		ajustesDto.setId(ajustes.getId());
		ajustesDto.setNombre(ajustes.getNombre());

		if (ajustes.getTipoArtistas() != null) {
			ajustesDto.setIdsTipoArtista(
					ajustes.getTipoArtistas()
							.stream()
							.map(TipoArtista::getId)
							.collect(Collectors.toList())
			);
		}

		if (ajustes.getAgencias() != null) {
			ajustesDto.setIdsAgencias(
					ajustes.getAgencias()
							.stream()
							.map(Agencia::getId)
							.collect(Collectors.toList())
			);
		}

		if (ajustes.getCcaa() != null) {
			ajustesDto.setIdsComunidades(
					ajustes.getCcaa()
							.stream()
							.map(Ccaa::getId)
							.collect(Collectors.toList())
			);
		}

        ajustesDto.setPredeterminado(ajustes.isPredeterminado());

		return ajustesDto;
	}
}
