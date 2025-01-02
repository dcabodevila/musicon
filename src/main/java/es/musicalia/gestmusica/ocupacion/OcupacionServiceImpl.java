package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.localizacion.*;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OcupacionServiceImpl implements OcupacionService {

	private final OcupacionRepository ocupacionRepository;
	private final ArtistaRepository artistaRepository;
	private final CcaaRepository comunidadRepository;
	private final ProvinciaRepository provinciaRepository;
	private final MunicipioRepository municipioRepository;
	private final TipoOcupacionRepository tipoOcupacionRepository;
	private final OcupacionEstadoRepository ocupacionEstadoRepository;
	private final TarifaRepository tarifaRepository;
	private final UserService userService;

	public OcupacionServiceImpl(OcupacionRepository ocupacionRepository, ArtistaRepository artistaRepository, CcaaRepository comunidadRepository, ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, TipoOcupacionRepository tipoOcupacionRepository, OcupacionEstadoRepository ocupacionEstadoRepository, TarifaRepository tarifaRepository, UserService userService){
		this.ocupacionRepository = ocupacionRepository;
		this.artistaRepository = artistaRepository;
        this.comunidadRepository = comunidadRepository;
        this.provinciaRepository = provinciaRepository;
        this.municipioRepository = municipioRepository;
        this.tipoOcupacionRepository = tipoOcupacionRepository;
        this.ocupacionEstadoRepository = ocupacionEstadoRepository;
        this.tarifaRepository = tarifaRepository;
        this.userService = userService;
    }

	@Override
	public List<CodigoNombreDto> listarTiposOcupacion() {
		return Arrays.stream(TipoOcupacionEnum.values())
				.map(e -> new CodigoNombreDto(e.getId(), e.getDescripcion()))
				.collect(Collectors.toList());
	}

	@Override
	public OcupacionEditDto findOcupacionEditDtoByArtistaIdAndDates(long id){
		return this.ocupacionRepository.findOcupacionEditDtoByArtistaIdAndDates(id);
	}

	@Override
	@Transactional(readOnly = false)
	public Ocupacion saveOcupacion(OcupacionSaveDto ocupacionSaveDto){

		final Ocupacion ocupacion = ocupacionSaveDto.getId()!=null? this.ocupacionRepository.findById(ocupacionSaveDto.getId()).orElse(new Ocupacion())  : new Ocupacion();

		ocupacion.setArtista(this.artistaRepository.findById(ocupacionSaveDto.getIdArtista()).get());
		ocupacion.setFecha(ocupacionSaveDto.getFecha());
		ocupacion.setImporte(ocupacionSaveDto.getImporte());
		ocupacion.setTipoOcupacion(this.tipoOcupacionRepository.findById(ocupacionSaveDto.getIdTipoOcupacion()).get());

		Long idEstadoOcupacion = getIdEstadoOcupacion(ocupacionSaveDto.getIdTipoOcupacion());
		ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(idEstadoOcupacion).get());

		ocupacion.setProvincia(this.provinciaRepository.findById(ocupacionSaveDto.getIdProvincia()).get());
		ocupacion.setMunicipio(this.municipioRepository.findById(ocupacionSaveDto.getIdMunicipio()).get());
		ocupacion.setPoblacion(ocupacionSaveDto.getLocalidad());
		ocupacion.setLugar(ocupacionSaveDto.getLugar());
		ocupacion.setTarifa(actualizarTarifaSegunOcupacion(ocupacionSaveDto, ocupacion));
		ocupacion.setObservaciones(ocupacionSaveDto.getObservaciones());
		ocupacion.setMatinal(ocupacionSaveDto.getMatinal());
		ocupacion.setSoloMatinal(ocupacionSaveDto.getSoloMatinal());
		ocupacion.setActivo(true);

		final Usuario usuario = this.userService.obtenerUsuarioAutenticado();
		ocupacion.setUsuario(usuario);

		if (ocupacion.getFechaCreacion()==null){
			ocupacion.setFechaCreacion(LocalDateTime.now());
			ocupacion.setUsuarioCreacion(usuario.getUsername());
		}
		else {
			ocupacion.setFechaModificacion(LocalDateTime.now());
			ocupacion.setUsuarioModificacion(usuario.getUsername());
		}

		return this.ocupacionRepository.save(ocupacion);
	}

	@Override
	public boolean existeOcupacionFecha(OcupacionSaveDto ocupacionSaveDto){

		final List<OcupacionDto> optionalOcupacion = ocupacionSaveDto.getId() == null ? this.ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(ocupacionSaveDto.getIdArtista(), ocupacionSaveDto.getFecha().withHour(0).withMinute(0).withSecond(0) , ocupacionSaveDto.getFecha().withHour(23).withMinute(59).withSecond(59)) : this.ocupacionRepository.findOcupacionesDtoByArtistaIdAndDatesNotInId(ocupacionSaveDto.getIdArtista(), ocupacionSaveDto.getFecha().withHour(0).withMinute(0).withSecond(0) , ocupacionSaveDto.getFecha().withHour(23).withMinute(59).withSecond(59), ocupacionSaveDto.getId());

		return (optionalOcupacion!=null && !optionalOcupacion.isEmpty());
	}


	private static Long getIdEstadoOcupacion(final Long idTipoOcupacion) {
		Long idEstadoOcupacion = OcupacionEstadoEnum.PENDIENTE.getId();
		if (TipoOcupacionEnum.RESERVADO.getId().equals(idTipoOcupacion)){
			idEstadoOcupacion = OcupacionEstadoEnum.RESERVADO.getId();
		}
		return idEstadoOcupacion;
	}

	private Tarifa actualizarTarifaSegunOcupacion(OcupacionSaveDto ocupacionSaveDto, Ocupacion ocupacion) {

		Tarifa nuevaTarifa = obtenerTarifaByOcupacion(ocupacionSaveDto,ocupacion);

		nuevaTarifa.setArtista(ocupacion.getArtista());
		nuevaTarifa.setImporte(ocupacion.getImporte());
		nuevaTarifa.setFecha(ocupacionSaveDto.getFecha());
		nuevaTarifa.setActivo(Boolean.TRUE);

		final String userName = this.userService.obtenerUsuarioAutenticado().getUsername();

		if (nuevaTarifa.getFechaCreacion()==null){
			nuevaTarifa.setFechaCreacion(LocalDateTime.now());
			nuevaTarifa.setUsuarioCreacion(userName);
		}
		else {
			nuevaTarifa.setFechaModificacion(LocalDateTime.now());
			nuevaTarifa.setUsuarioModificacion(userName);
		}

		return this.tarifaRepository.save(nuevaTarifa);


	}

	private Tarifa obtenerTarifaByOcupacion(final OcupacionSaveDto ocupacionSaveDto, final Ocupacion ocupacion){

		if (ocupacion.getTarifa()!=null){
			return ocupacion.getTarifa();
		}

		final List<Tarifa> listaTarifas = this.tarifaRepository.findTarifasByArtistaIdAndDates(ocupacionSaveDto.getIdArtista(), ocupacionSaveDto.getFecha().withHour(0).withMinute(0).withSecond(0), ocupacionSaveDto.getFecha().withHour(23).withMinute(59).withSecond(59));

		if (listaTarifas!=null && !listaTarifas.isEmpty()){
			return listaTarifas.get(0);
		}

		return new Tarifa();
	}

}
