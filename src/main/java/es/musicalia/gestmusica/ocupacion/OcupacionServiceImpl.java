package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.acceso.AccesoRepository;
import es.musicalia.gestmusica.api.DatosGestmanagerConvertedDTO;
import es.musicalia.gestmusica.api.TipoEstadoGestmanagerEnum;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.permiso.PermisoArtistaEnum;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import es.musicalia.gestmusica.usuario.EnvioEmailException;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class OcupacionServiceImpl implements OcupacionService {


	private final OcupacionRepository ocupacionRepository;
	private final ArtistaRepository artistaRepository;
	private final ProvinciaRepository provinciaRepository;
	private final MunicipioRepository municipioRepository;
	private final TipoOcupacionRepository tipoOcupacionRepository;
	private final OcupacionEstadoRepository ocupacionEstadoRepository;
	private final TarifaRepository tarifaRepository;
	private final UserService userService;
	private final PermisoService permisoService;
	private final AccesoRepository accesoRepository;
	private final EmailService emailService;
	private final OcupacionMapper ocupacionMapper;

	public OcupacionServiceImpl(OcupacionRepository ocupacionRepository, ArtistaRepository artistaRepository, ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, TipoOcupacionRepository tipoOcupacionRepository, OcupacionEstadoRepository ocupacionEstadoRepository, TarifaRepository tarifaRepository, UserService userService, PermisoService permisoService, AccesoRepository accesoRepository, EmailService emailService, OcupacionMapper ocupacionMapper){
		this.ocupacionRepository = ocupacionRepository;
		this.artistaRepository = artistaRepository;
        this.provinciaRepository = provinciaRepository;
        this.municipioRepository = municipioRepository;
        this.tipoOcupacionRepository = tipoOcupacionRepository;
        this.ocupacionEstadoRepository = ocupacionEstadoRepository;
        this.tarifaRepository = tarifaRepository;
        this.userService = userService;
        this.permisoService = permisoService;
        this.accesoRepository = accesoRepository;
        this.emailService = emailService;
        this.ocupacionMapper = ocupacionMapper;
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
	public DefaultResponseBody anularOcupacion(long id){
		final Ocupacion ocupacion =  this.ocupacionRepository.findById(id).orElseThrow();

		ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(OcupacionEstadoEnum.ANULADO.getId()).orElseThrow());
		this.ocupacionRepository.save(ocupacion);

		//TODO: llamar a Orquestas de galicia

		try {
			this.emailService.enviarMensajePorEmail(ocupacion.getUsuario().getEmail(), EmailTemplateEnum.EMAIL_NOTIFICACION_ANULACION);
		} catch (EnvioEmailException e) {
			return DefaultResponseBody.builder().success(true).message("Ocupacion anulada correctamente, pero ha habido un error inesperado enviando la notificación por correo").messageType("warning").build();
		} catch (Exception e) {
			log.error("error inesperado enviando notificacion de confirmación de ocupacion", e);
			return DefaultResponseBody.builder().success(true).message("Ocupacion anulada correctamente, pero ha habido un error inesperado enviando la notificación por correo").messageType("warning").build();
		}

		return DefaultResponseBody.builder().success(true).message("Ocupacion anulada correctamente").messageType("success").build();

	}

	@Override
	@Transactional(readOnly = false)
	public DefaultResponseBody confirmarOcupacion(long id){
		final Ocupacion ocupacion =  this.ocupacionRepository.findById(id).orElseThrow();
		ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(OcupacionEstadoEnum.OCUPADO.getId()).orElseThrow());
		ocupacion.setTipoOcupacion(this.tipoOcupacionRepository.findById(TipoOcupacionEnum.OCUPADO.getId()).orElseThrow());
		ocupacion.setUsuarioConfirmacion(this.userService.obtenerUsuarioAutenticado());
		this.ocupacionRepository.save(ocupacion);

		//TODO: llamar a Orquestas de galicia

        try {
            this.emailService.enviarMensajePorEmail(ocupacion.getUsuario().getEmail(), EmailTemplateEnum.EMAIL_NOTIFICACION_CONFIRMACION);
        } catch (EnvioEmailException e) {
			return DefaultResponseBody.builder().success(true).message("Ocupacion guardada correctamente, pero ha habido un error inesperado enviando la notificación por correo").messageType("warning").build();
        } catch (Exception e) {
			log.error("error inesperado enviando notificacion de confirmación de ocupacion", e);
			return DefaultResponseBody.builder().success(true).message("Ocupacion confirmada correctamente, pero ha habido un error inesperado enviando la notificación por correo").messageType("warning").build();
		}


		return DefaultResponseBody.builder().success(true).message("Ocupacion confirmada correctamente").messageType("success").build();

	}

	@Override
	@Transactional(readOnly = false)
	public DefaultResponseBody saveOcupacion(OcupacionSaveDto ocupacionSaveDto) throws es.musicalia.gestmusica.ocupacion.ModificacionOcupacionException {

		log.info("Empezando saveOcupacion: {}", ocupacionSaveDto.toString());

		final Ocupacion ocupacion = ocupacionSaveDto.getId()!=null? this.ocupacionRepository.findById(ocupacionSaveDto.getId()).orElse(new Ocupacion())  : new Ocupacion();
		final Usuario usuario = this.userService.obtenerUsuarioAutenticado();

		if (ocupacionSaveDto.getId()!=null){
			final Usuario usuarioAutenticado = this.userService.obtenerUsuarioAutenticado();
			if (!(ocupacion.getUsuario().getId().equals(usuarioAutenticado.getId())) &&
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
		ocupacion.setTarifa(actualizarTarifaSegunOcupacion(ocupacionSaveDto.getIdArtista(), ocupacionSaveDto.getFecha(), ocupacion));
		ocupacion.setObservaciones(ocupacionSaveDto.getObservaciones());
		ocupacion.setMatinal(ocupacionSaveDto.getMatinal());
		ocupacion.setSoloMatinal(ocupacionSaveDto.getSoloMatinal());
		ocupacion.setActivo(true);

		Ocupacion ocupacionSave = this.ocupacionRepository.save(ocupacion);

		if (!permisoConfirmarOcupacionAgencia){

            try {
                this.emailService.enviarMensajePorEmail(ocupacion.getArtista().getAgencia().getUsuario().getEmail(), EmailTemplateEnum.EMAIL_NOTIFICACION_CONFIRMACION_PENDIENTE);
            } catch (EnvioEmailException e) {
                log.error("error enviando notificacion de solicitud de ocupacion", e);
				return DefaultResponseBody.builder().success(true).message("Ocupacion guardada correctamente. Pero no se ha podido enviar la notificación por correo").messageType("warning").build();
            }catch (Exception e) {
				log.error("error inesperado enviando notificacion de solicitud de ocupacion", e);
				return DefaultResponseBody.builder().success(true).message("Ocupacion guardada correctamente, pero ha habido un error inesperado enviando la notificación por correo").messageType("warning").build();
			}

        }
		else {

			//TODO notificar a orquestas de galicia si aplica

		}


		log.info("Completado guardado ocupacionSave: {}", ocupacionSave.getId());
		return DefaultResponseBody.builder().success(true).message("Ocupacion guardada correctamente").messageType("success").build();



	}

	@Override
	public boolean existeOcupacionFecha(OcupacionSaveDto ocupacionSaveDto){

		final List<OcupacionRecord> optionalOcupacion = ocupacionSaveDto.getId() == null ? this.ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(ocupacionSaveDto.getIdArtista(), ocupacionSaveDto.getFecha().withHour(0).withMinute(0).withSecond(0) , ocupacionSaveDto.getFecha().withHour(23).withMinute(59).withSecond(59)) : this.ocupacionRepository.findOcupacionesDtoByArtistaIdAndDatesNotInId(ocupacionSaveDto.getIdArtista(), ocupacionSaveDto.getFecha().withHour(0).withMinute(0).withSecond(0) , ocupacionSaveDto.getFecha().withHour(23).withMinute(59).withSecond(59), ocupacionSaveDto.getId());

		if (optionalOcupacion == null || optionalOcupacion.isEmpty()) {
			return false;
		}

		if (optionalOcupacion.size() > 1) {
			return true;
		}

		boolean soloMatinalExistente = optionalOcupacion.get(0).soloMatinal();
		boolean soloMatinalNuevo = ocupacionSaveDto.getSoloMatinal();

		return soloMatinalExistente == soloMatinalNuevo;


	}


	private static Long getIdEstadoOcupacion(final Long idTipoOcupacion, boolean permisoConfirmarOcupacionAgencia) {
		Long idEstadoOcupacion = permisoConfirmarOcupacionAgencia ? OcupacionEstadoEnum.OCUPADO.getId()  : OcupacionEstadoEnum.PENDIENTE.getId();
		if (TipoOcupacionEnum.RESERVADO.getId().equals(idTipoOcupacion)){
			idEstadoOcupacion = OcupacionEstadoEnum.RESERVADO.getId();
		}
		return idEstadoOcupacion;
	}

	private Tarifa actualizarTarifaSegunOcupacion(Long idArtista, LocalDateTime fecha, Ocupacion ocupacion) {

		Tarifa nuevaTarifa = obtenerTarifaByOcupacion(idArtista, fecha,ocupacion);

		nuevaTarifa.setArtista(ocupacion.getArtista());
		nuevaTarifa.setImporte(ocupacion.getImporte()!=null ? ocupacion.getImporte() : BigDecimal.ZERO);
		nuevaTarifa.setFecha(fecha);
		nuevaTarifa.setActivo(Boolean.TRUE);

		final String userName = this.userService.isUserAutheticated() ? this.userService.obtenerUsuarioAutenticado().getUsername() : "GESTMANAGER";

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

	private Tarifa obtenerTarifaByOcupacion(Long idArtista, LocalDateTime fecha, final Ocupacion ocupacion){

		if (ocupacion.getTarifa()!=null){
			return ocupacion.getTarifa();
		}

		final List<Tarifa> listaTarifas = this.tarifaRepository.findTarifasByArtistaIdAndDates(idArtista, fecha.withHour(0).withMinute(0).withSecond(0), fecha.withHour(23).withMinute(59).withSecond(59));

		if (listaTarifas!=null && !listaTarifas.isEmpty()){
			return listaTarifas.get(0);
		}

		return new Tarifa();
	}

	@Override
	public List<OcupacionRecord> findOcupacionesDtoByAgenciaPendientes(Set<Long> idsAgencia){
		return this.ocupacionRepository.findOcupacionesDtoByAgenciaPendientes(idsAgencia).orElse(new ArrayList<>());
	}

	@Override
	public List<OcupacionListRecord> findOcupacionesByArtistasListAndDatesActivo(CustomAuthenticatedUser user, OcupacionListFilterDto ocupacionListFilterDto) {

		// Filtrar por los artistas sobre los que tenemos permiso OCUPACIONES

		final CustomAuthenticatedUser authenticatedUser = (CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		final Set<Long> idsArtistas = authenticatedUser.getMapPermisosArtista().keySet().stream()
				.filter(artistaId -> authenticatedUser.getMapPermisosArtista().get(artistaId).contains(PermisoArtistaEnum.OCUPACIONES.name()))
				.collect(Collectors.toSet());

		final Specification<Ocupacion> spec
				= Specification
				.where(OcupacionSpecifications.hasArtistaIdsIn(idsArtistas))
				.and(OcupacionSpecifications.hasFechaAfter(ocupacionListFilterDto.getFechaDesde().atStartOfDay()))
				.and(OcupacionSpecifications.hasFechaBefore(ocupacionListFilterDto.getFechaHasta() != null ? ocupacionListFilterDto.getFechaHasta().atTime(23, 59, 59): null))
				.and(OcupacionSpecifications.isActivo())
				.and(OcupacionSpecifications.orderByIdDesc())
				.and(OcupacionSpecifications.hasAgenciaId(ocupacionListFilterDto.getIdAgencia()))
				.and(OcupacionSpecifications.hasArtistaId(ocupacionListFilterDto.getIdArtista()))
				.and(OcupacionSpecifications.hasEstadoNotAnulado())
				.and(OcupacionSpecifications.hasUsuarioId(isRolRepresentante(user.getUserId(), ocupacionListFilterDto.getIdAgencia()), user.getUserId()))
				;


		// Ejecutar la consulta y mapear resultados a OcupacionListRecord
		return ocupacionRepository.findAll(spec).stream()
				.map(ocupacion -> new OcupacionListRecord(
						ocupacion.getId(),
						ocupacion.getFecha(),
						ocupacion.getArtista().getId(),
						ocupacion.getArtista().getNombre(),
						ocupacion.getImporte().setScale(0).toPlainString(),
						true,
						ocupacion.getTipoOcupacion().getNombre(),
						ocupacion.getProvincia().getNombre(),
						ocupacion.getMunicipio().getNombre(),
						ocupacion.getPoblacion(),
						ocupacion.isMatinal(),
						ocupacion.isSoloMatinal(),
						ocupacion.getOcupacionEstado().getNombre(),
						ocupacion.getUsuario().getId(),
						ocupacion.getUsuario().getNombre() + " " + ocupacion.getUsuario().getApellidos(),
						ocupacion.getUsuarioConfirmacion() != null ? ocupacion.getUsuarioConfirmacion().getId() : null,
						ocupacion.getUsuarioConfirmacion() != null ? ocupacion.getUsuarioConfirmacion().getNombre() + " " + ocupacion.getUsuarioConfirmacion().getApellidos() : null,
						ocupacion.getFechaCreacion()

				))
				.toList();


	}

	private boolean isRolRepresentante(Long idUsuario, Long idAgencia){
		if (idAgencia!=null){
			return this.accesoRepository.findAccesoByIdUsuarioAndIdAgenciaAndCodigoRol(idUsuario, idAgencia, RolEnum.ROL_REPRESENTANTE.getCodigo()).isPresent();
		}

		return false;

	}

	@Override
	public OcupacionSaveDto getOcupacionSaveDto(Long idOcupacion){
		return this.ocupacionMapper.toDto(this.ocupacionRepository.findById(idOcupacion).orElseThrow());
	}

	@Override
	@Transactional(readOnly = false)
	public Ocupacion saveOcupacionFromGestmanager(DatosGestmanagerConvertedDTO datos) {
		Ocupacion ocupacion = gestManagerSincroToOcupacion(datos);
		return this.ocupacionRepository.save(ocupacion);

		//TODO: llamar a Orquestas de galicia
	}

	@Transactional(readOnly = false)
	@Override
	public DefaultResponseBody deleteOcupacionFromGestmanager(DatosGestmanagerConvertedDTO datos) {

		Long idArtista = datos.getIdArtistaGestmanager();
		LocalDateTime fecha = datos.getFecha();

		List<String> listaDatos = Arrays.asList(datos.getDescripcion().split("\\*"));
		if (CollectionUtils.isEmpty(listaDatos) || listaDatos.size() < 3) {
			throw new IllegalArgumentException("Campo descripcion no puede estar vacio o tener menos de 3 elementos separados por * ");
		}

		String provincia = listaDatos.get(0);
		String municipio = listaDatos.get(1);
		String localidad = listaDatos.get(2);

		final Artista artista = this.artistaRepository.findArtistaByIdArtistaGestmanager(datos.getIdArtistaGestmanager()).orElseThrow(() -> new IllegalArgumentException("Artista no encontrado con el idArtistaGestmanager: " + datos.getIdArtistaGestmanager()));

		Optional<List<Ocupacion>> listaOcupaciones = this.ocupacionRepository.findOcupacionesDtoByArtistaIdAndDatesCualquiera(artista.getId(), fecha.withHour(0).withMinute(0).withSecond(0), fecha.withHour(23).withMinute(59).withSecond(59));

		if (listaOcupaciones.isPresent() && !listaOcupaciones.get().isEmpty()) {

			if (listaOcupaciones.get().size() > 1) {
				List<Ocupacion> ocupacionesFiltradas = listaOcupaciones.get().stream()
						.filter(o -> o.getProvincia().getNombre().equalsIgnoreCase(provincia.trim()) &&
								o.getMunicipio().getNombre().equalsIgnoreCase(municipio.trim()) &&
								o.getPoblacion().equalsIgnoreCase(localidad.trim()))
						.toList();

				if (ocupacionesFiltradas.size() > 1) {
					DefaultResponseBody.builder().success(false).message("Se ha encontrado más de 1 ocupación que coincide con la ubicación especificada").messageType("warning").build();
				}
				if (ocupacionesFiltradas.isEmpty()) {
					DefaultResponseBody.builder().success(false).message("No se encontraron ocupaciones que coincidan con la ubicación especificada").messageType("warning").build();
				}
				return this.anularOcupacion(ocupacionesFiltradas.get(0).getId());

			} else {
				return this.anularOcupacion(listaOcupaciones.get().get(0).getId());
			}

		}
		else {
			throw new IllegalArgumentException("No se encontraron ocupaciones para el artista con id: " + idArtista + " y fecha: " + fecha);
		}




	}


	private Ocupacion gestManagerSincroToOcupacion(DatosGestmanagerConvertedDTO datos) {

		Ocupacion ocupacion = new Ocupacion();

		// Datos básicos
		ocupacion.setFecha(datos.getFecha());
		ocupacion.setPoblacion(datos.getPoblacion());
		ocupacion.setLugar(datos.getNombreLocal());
		ocupacion.setMatinal(datos.getMatinal());
		ocupacion.setSoloMatinal(datos.getSoloMatinal());

		// Entidades relacionadas
		ocupacion.setArtista(artistaRepository.findArtistaByIdArtistaGestmanager(datos.getIdArtistaGestmanager())
				.orElseThrow(() -> new IllegalArgumentException("Artista no encontrado: " + datos.getIdArtistaGestmanager())));

		ocupacion.setProvincia(provinciaRepository.findProvinciaByNombreUpperCase(datos.getProvincia().trim().toUpperCase())
				.orElseThrow(() -> new IllegalArgumentException("Provincia no encontrada: " + datos.getProvincia())));

		ocupacion.setMunicipio(municipioRepository.findMunicipioByNombreUpperCase(datos.getMunicipio().trim().toUpperCase())
				.orElseThrow(() -> new IllegalArgumentException("Municipio no encontrado: " + datos.getMunicipio())));

		// Estado y tipo según el estado recibido
		if (datos.getEstado() == TipoEstadoGestmanagerEnum.OCUPADO) {
			ocupacion.setTipoOcupacion(tipoOcupacionRepository.findById(TipoOcupacionEnum.OCUPADO.getId())
					.orElseThrow(() -> new IllegalArgumentException("Tipo ocupación no encontrado: OCUPADO")));
			ocupacion.setOcupacionEstado(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.OCUPADO.getId())
					.orElseThrow(() -> new IllegalArgumentException("Estado ocupación no encontrado: OCUPADO")));
		} else {
			ocupacion.setTipoOcupacion(tipoOcupacionRepository.findById(TipoOcupacionEnum.RESERVADO.getId())
					.orElseThrow(() -> new IllegalArgumentException("Tipo ocupación no encontrado: RESERVADO")));
			ocupacion.setOcupacionEstado(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.RESERVADO.getId())
					.orElseThrow(() -> new IllegalArgumentException("Estado ocupación no encontrado: RESERVADO")));
		}


		final List<Usuario> usuariosAdmin = this.userService.findUsuariosAdmin();

		if (usuariosAdmin.isEmpty()) {
			throw new IllegalArgumentException("No hay usuarios administradores registrados");
		}

		final Usuario usuario = usuariosAdmin.get(0);

		// Datos de auditoría
		ocupacion.setUsuario(usuario);
		ocupacion.setFechaCreacion(LocalDateTime.now());
		ocupacion.setUsuarioCreacion("GESTMANAGER");
		ocupacion.setActivo(true);

		// Valores por defecto
		ocupacion.setImporte(BigDecimal.ZERO);
		ocupacion.setPorcentajeRepre(BigDecimal.ZERO);
		ocupacion.setIva(BigDecimal.ZERO);
		ocupacion.setTarifa(actualizarTarifaSegunOcupacion(ocupacion.getArtista().getId(), ocupacion.getFecha(), ocupacion));
		return ocupacion;
	}




}
