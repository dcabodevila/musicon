package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.localizacion.*;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.permiso.PermisoArtistaEnum;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OcupacionServiceImpl implements OcupacionService {
	private Logger logger = LoggerFactory.getLogger(OcupacionServiceImpl.class);

	private final OcupacionRepository ocupacionRepository;
	private final ArtistaRepository artistaRepository;
	private final ProvinciaRepository provinciaRepository;
	private final MunicipioRepository municipioRepository;
	private final TipoOcupacionRepository tipoOcupacionRepository;
	private final OcupacionEstadoRepository ocupacionEstadoRepository;
	private final TarifaRepository tarifaRepository;
	private final UserService userService;
	private final PermisoService permisoService;

	public OcupacionServiceImpl(OcupacionRepository ocupacionRepository, ArtistaRepository artistaRepository, ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, TipoOcupacionRepository tipoOcupacionRepository, OcupacionEstadoRepository ocupacionEstadoRepository, TarifaRepository tarifaRepository, UserService userService, PermisoService permisoService){
		this.ocupacionRepository = ocupacionRepository;
		this.artistaRepository = artistaRepository;
        this.provinciaRepository = provinciaRepository;
        this.municipioRepository = municipioRepository;
        this.tipoOcupacionRepository = tipoOcupacionRepository;
        this.ocupacionEstadoRepository = ocupacionEstadoRepository;
        this.tarifaRepository = tarifaRepository;
        this.userService = userService;
        this.permisoService = permisoService;
    }

	@Override
	public List<CodigoNombreDto> listarTiposOcupacion(Long idArtista) {

		return Arrays.stream(TipoOcupacionEnum.values())
				.filter(e -> TipoOcupacionEnum.OCUPADO.getId().equals(e.getId()) ||
						(TipoOcupacionEnum.RESERVADO.getId().equals(e.getId()) &&
								permisoService.existePermisoUsuarioArtista(idArtista, PermisoArtistaEnum.RESERVAR_OCUPACION.name())))
				.map(e -> new CodigoNombreDto(e.getId(), e.getDescripcion()))
				.collect(Collectors.toList());
	}

	@Override
	public OcupacionEditDto findOcupacionEditDtoByArtistaIdAndDates(long id){
		return this.ocupacionRepository.findOcupacionEditDtoByArtistaIdAndDates(id);
	}

	@Override
	@Transactional(readOnly = false)
	public Void anularOcupacion(long id){
		final Ocupacion ocupacion =  this.ocupacionRepository.findById(id).get();

		ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(OcupacionEstadoEnum.ANULADO.getId()).get());
		this.ocupacionRepository.save(ocupacion);
		return null;

	}

	@Override
	@Transactional(readOnly = false)
	public Void confirmarOcupacion(long id){
		final Ocupacion ocupacion =  this.ocupacionRepository.findById(id).get();
		ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(OcupacionEstadoEnum.OCUPADO.getId()).get());
		ocupacion.setTipoOcupacion(this.tipoOcupacionRepository.findById(TipoOcupacionEnum.OCUPADO.getId()).get());
		this.ocupacionRepository.save(ocupacion);
		return null;

	}

	@Override
	@Transactional(readOnly = false)
	public Ocupacion saveOcupacion(OcupacionSaveDto ocupacionSaveDto) throws es.musicalia.gestmusica.ocupacion.ModificacionOcupacionException {

		logger.info("Empezando saveOcupacion: {}", ocupacionSaveDto.toString());

		final Ocupacion ocupacion = ocupacionSaveDto.getId()!=null? this.ocupacionRepository.findById(ocupacionSaveDto.getId()).orElse(new Ocupacion())  : new Ocupacion();
		final Usuario usuario = this.userService.obtenerUsuarioAutenticado();

		if (ocupacionSaveDto.getId()!=null){
			final Usuario usuarioAutenticado = this.userService.obtenerUsuarioAutenticado();
			if (!(ocupacion.getUsuario().getId()==usuarioAutenticado.getId()) &&
					!this.permisoService.existePermisoUsuarioAgencia(ocupacion.getArtista().getAgencia().getId(), PermisoAgenciaEnum.MODIFICAR_OCUPACION_OTROS.name())) {
				throw new es.musicalia.gestmusica.ocupacion.ModificacionOcupacionException("No tiene permisos para modificar ocupaciones de otros usuarios");
			}
			ocupacion.setFechaModificacion(LocalDateTime.now());
			ocupacion.setUsuarioModificacion(usuario.getUsername());

		}else {
			ocupacion.setUsuario(usuario);
			ocupacion.setFechaCreacion(LocalDateTime.now());
			ocupacion.setUsuarioCreacion(usuario.getUsername());
		}


		final Artista artista = this.artistaRepository.findById(ocupacionSaveDto.getIdArtista()).orElseThrow();

		ocupacion.setArtista(artista);
		ocupacion.setFecha(ocupacionSaveDto.getFecha());
		ocupacion.setImporte(ocupacionSaveDto.getImporte()!=null? ocupacionSaveDto.getImporte() : BigDecimal.ZERO);
		ocupacion.setPorcentajeRepre(ocupacionSaveDto.getPorcentajeRepre()!=null ? ocupacionSaveDto.getPorcentajeRepre(): BigDecimal.ZERO);
		ocupacion.setIva(ocupacionSaveDto.getIva()!=null?ocupacionSaveDto.getIva(): BigDecimal.ZERO);
		ocupacion.setTipoOcupacion(this.tipoOcupacionRepository.findById(ocupacionSaveDto.getIdTipoOcupacion()).orElseThrow());

		final boolean permisoConfirmarOcupacionAgencia = this.permisoService.existePermisoUsuarioAgencia(artista.getAgencia().getId(), PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name());

		Long idEstadoOcupacion = getIdEstadoOcupacion(ocupacionSaveDto.getIdTipoOcupacion(), permisoConfirmarOcupacionAgencia);
		ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(idEstadoOcupacion).orElseThrow());

		ocupacion.setProvincia(this.provinciaRepository.findById(ocupacionSaveDto.getIdProvincia()).orElseThrow());
		ocupacion.setMunicipio(this.municipioRepository.findById(ocupacionSaveDto.getIdMunicipio()).orElseThrow());
		ocupacion.setPoblacion(ocupacionSaveDto.getLocalidad());
		ocupacion.setLugar(ocupacionSaveDto.getLugar());
		ocupacion.setTarifa(actualizarTarifaSegunOcupacion(ocupacionSaveDto, ocupacion));
		ocupacion.setObservaciones(ocupacionSaveDto.getObservaciones());
		ocupacion.setMatinal(ocupacionSaveDto.getMatinal());
		ocupacion.setSoloMatinal(ocupacionSaveDto.getSoloMatinal());
		ocupacion.setActivo(true);





		Ocupacion ocupacionSave = this.ocupacionRepository.save(ocupacion);

        logger.info("Completado guardado ocupacionSave: {}", ocupacionSave.getId());

		return ocupacionSave;


	}

	@Override
	public boolean existeOcupacionFecha(OcupacionSaveDto ocupacionSaveDto){

		final List<OcupacionDto> optionalOcupacion = ocupacionSaveDto.getId() == null ? this.ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(ocupacionSaveDto.getIdArtista(), ocupacionSaveDto.getFecha().withHour(0).withMinute(0).withSecond(0) , ocupacionSaveDto.getFecha().withHour(23).withMinute(59).withSecond(59)) : this.ocupacionRepository.findOcupacionesDtoByArtistaIdAndDatesNotInId(ocupacionSaveDto.getIdArtista(), ocupacionSaveDto.getFecha().withHour(0).withMinute(0).withSecond(0) , ocupacionSaveDto.getFecha().withHour(23).withMinute(59).withSecond(59), ocupacionSaveDto.getId());

		return (optionalOcupacion!=null && !optionalOcupacion.isEmpty());
	}


	private static Long getIdEstadoOcupacion(final Long idTipoOcupacion, boolean permisoConfirmarOcupacionAgencia) {
		Long idEstadoOcupacion = permisoConfirmarOcupacionAgencia ? OcupacionEstadoEnum.OCUPADO.getId()  : OcupacionEstadoEnum.PENDIENTE.getId();
		if (TipoOcupacionEnum.RESERVADO.getId().equals(idTipoOcupacion)){
			idEstadoOcupacion = OcupacionEstadoEnum.RESERVADO.getId();
		}
		return idEstadoOcupacion;
	}

	private Tarifa actualizarTarifaSegunOcupacion(OcupacionSaveDto ocupacionSaveDto, Ocupacion ocupacion) {

		Tarifa nuevaTarifa = obtenerTarifaByOcupacion(ocupacionSaveDto,ocupacion);

		nuevaTarifa.setArtista(ocupacion.getArtista());
		nuevaTarifa.setImporte(ocupacion.getImporte()!=null ? ocupacion.getImporte() : BigDecimal.ZERO);
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

	@Override
	public List<OcupacionDto> findOcupacionesDtoByAgenciaPendientes(Set<Long> idsAgencia){
		return this.ocupacionRepository.findOcupacionesDtoByAgenciaPendientes(idsAgencia).orElse(new ArrayList<>());
	}

}
