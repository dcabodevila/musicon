package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.acceso.AccesoRepository;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.mensaje.Mensaje;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.orquestasdegalicia.ActuacionExterna;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.permiso.PermisoArtistaEnum;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import es.musicalia.gestmusica.usuario.EnvioEmailException;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.util.ConstantsGestmusica;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
	private final MensajeService mensajeService;

	public OcupacionServiceImpl(OcupacionRepository ocupacionRepository, ArtistaRepository artistaRepository, ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, TipoOcupacionRepository tipoOcupacionRepository, OcupacionEstadoRepository ocupacionEstadoRepository, TarifaRepository tarifaRepository, UserService userService, PermisoService permisoService, AccesoRepository accesoRepository, EmailService emailService, OcupacionMapper ocupacionMapper, MensajeService mensajeService){
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
        this.mensajeService = mensajeService;
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

        actualizarTarifasAnularOcupacion(ocupacion.getTarifa());

        ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(OcupacionEstadoEnum.ANULADO.getId()).orElseThrow());
		this.ocupacionRepository.save(ocupacion);

		try {
			enviarMensajeInternoOcupacionAnulada(ocupacion.getUsuario(), ocupacion);

			this.emailService.enviarMensajePorEmail(ocupacion.getUsuario().getEmail(), EmailTemplateEnum.EMAIL_NOTIFICACION_ANULACION);

		} catch (EnvioEmailException e) {
			return DefaultResponseBody.builder().success(true).message("Ocupacion anulada correctamente, pero ha habido un error enviando la notificación por correo").messageType("warning").build();
		} catch (Exception e) {
			log.error("error inesperado enviando notificacion de confirmación de ocupacion", e);
			return DefaultResponseBody.builder().success(true).message("Ocupacion anulada correctamente, pero ha habido un error enviando la notificación por correo").messageType("warning").build();
		}

		return DefaultResponseBody.builder().success(true).message("Ocupacion anulada correctamente").messageType("success").build();

	}

    private void actualizarTarifasAnularOcupacion(Tarifa oldTarifa) {
        Tarifa nuevaTarifa = copiarTarifa(oldTarifa);

        oldTarifa.setActivo(Boolean.FALSE);

        this.tarifaRepository.save(oldTarifa);
        this.tarifaRepository.save(nuevaTarifa);
    }

    private static Tarifa copiarTarifa(Tarifa oldTarifa) {
        Tarifa nuevaTarifa = new Tarifa();

        nuevaTarifa.setImporte(oldTarifa.getImporte());
        nuevaTarifa.setActivo(Boolean.TRUE);
        nuevaTarifa.setFecha(oldTarifa.getFecha());
        nuevaTarifa.setImporte(oldTarifa.getImporte());
        nuevaTarifa.setArtista(oldTarifa.getArtista());
        nuevaTarifa.setUsuarioCreacion(oldTarifa.getUsuarioCreacion());
        nuevaTarifa.setFechaCreacion(oldTarifa.getFechaCreacion());
        return nuevaTarifa;
    }

    @Override
	@Transactional(readOnly = false)
	public DefaultResponseBody confirmarOcupacion(long id){
		final Ocupacion ocupacion =  this.ocupacionRepository.findById(id).orElseThrow();
		ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(OcupacionEstadoEnum.OCUPADO.getId()).orElseThrow());
		ocupacion.setTipoOcupacion(this.tipoOcupacionRepository.findById(TipoOcupacionEnum.OCUPADO.getId()).orElseThrow());
		ocupacion.setUsuarioConfirmacion(this.userService.obtenerUsuarioAutenticado().orElseThrow());
		this.ocupacionRepository.save(ocupacion);

        try {

			ActuacionExterna actuacionExterna = getActuacionExterna(ocupacion.getArtista(), ocupacion);

//			this.orquestasDeGaliciaService.enviarActuacionOrquestasDeGalicia(false, actuacionExterna, OcupacionEstadoEnum.OCUPADO.getDescripcion());
			enviarMensajeInternoOcupacionConfirmada(ocupacion.getUsuario(), ocupacion);
			this.emailService.enviarMensajePorEmail(ocupacion.getUsuario().getEmail(), EmailTemplateEnum.EMAIL_NOTIFICACION_CONFIRMACION);



        } catch (EnvioEmailException e) {
			return DefaultResponseBody.builder().success(true).message("Ocupacion guardada correctamente, pero ha habido un error enviando la notificación por correo").messageType("warning").build();
        } catch (Exception e) {
			log.error("error inesperado enviando notificacion de confirmación de ocupacion", e);
			return DefaultResponseBody.builder().success(true).message("Ocupacion confirmada correctamente, pero ha habido un error enviando la notificación por correo").messageType("warning").build();
		}


		return DefaultResponseBody.builder().success(true).message("Ocupacion confirmada correctamente").messageType("success").build();

	}

	@Override
	@Transactional(readOnly = false)
	public DefaultResponseBody saveOcupacion(OcupacionSaveDto ocupacionSaveDto) throws es.musicalia.gestmusica.ocupacion.ModificacionOcupacionException {
		final Artista artista = this.artistaRepository.findById(ocupacionSaveDto.getIdArtista()).orElseThrow();

		final boolean permisoConfirmarOcupacionAgencia = this.permisoService.existePermisoUsuarioAgencia(artista.getAgencia().getId(), PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name());

		final Ocupacion ocupacion = guardarOcupacion(ocupacionSaveDto, permisoConfirmarOcupacionAgencia, false);

		try {
			if (!permisoConfirmarOcupacionAgencia){
				final Usuario usuario = ocupacion.getArtista().getAgencia().getUsuario();
				if (ocupacionSaveDto.getId() == null) {
					enviarMensajeInternoNuevaOcupacionPendiente(usuario, ocupacion);
				}
				else {
					enviarMensajeInternoModificacionOcupacionPendiente(usuario, ocupacion);
				}
				this.emailService.enviarMensajePorEmail(usuario.getEmail(), EmailTemplateEnum.EMAIL_NOTIFICACION_CONFIRMACION_PENDIENTE);

			}

		} catch (EnvioEmailException e) {
			log.error("error enviando notificacion de solicitud de ocupacion", e);
			return DefaultResponseBody.builder().success(true).message("Ocupacion guardada correctamente. Pero no se ha podido enviar la notificación por correo").messageType("warning").idEntidad(ocupacion.getId()).build();
		} catch (Exception e) {
			log.error("error enviando notificacion de solicitud de ocupacion", e);
			return DefaultResponseBody.builder().success(true).message("Ocupacion guardada correctamente, pero ha habido un error enviando la notificación por correo").messageType("warning").idEntidad(ocupacion.getId()).build();
		}



		log.info("Completado guardado ocupacionSave: {}", ocupacion.getId());
		return DefaultResponseBody.builder().success(true).message("Ocupacion guardada correctamente").messageType("success").idEntidad(ocupacion.getId()).build();



	}

	private void enviarMensajeInternoNuevaOcupacionPendiente(Usuario usuario, Ocupacion ocupacion) {
		Mensaje mensaje = new Mensaje();
		mensaje.setUsuarioRemite(this.userService.obtenerUsuarioAutenticado().orElseThrow());
		mensaje.setUsuarioReceptor(usuario);
		mensaje.setAsunto("Nueva ocupación pendiente de confirmación");
		mensaje.setMensaje("Nueva ocupación pendiente para " + ocupacion.getArtista().getNombre() + " el " + ocupacion.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " en "+ (ocupacion.getPoblacion()!=null? ocupacion.getPoblacion()+", " : "") + ocupacion.getMunicipio().getNombre() + ", " + ocupacion.getProvincia().getNombre());
		mensaje.setImagen("fa-calendar-plus text-secondary");
		mensaje.setUrlEnlace("/ocupacion/".concat(ocupacion.getId().toString()));
		this.mensajeService.enviarMensaje(mensaje, usuario.getId());
	}

	private void enviarMensajeInternoModificacionOcupacionPendiente(Usuario usuario, Ocupacion ocupacion) {
		Mensaje mensaje = new Mensaje();
		mensaje.setUsuarioRemite(this.userService.obtenerUsuarioAutenticado().orElseThrow());
		mensaje.setUsuarioReceptor(usuario);
		mensaje.setAsunto("Modificación ocupación pendiente de confirmación");
		mensaje.setMensaje("Modificación ocupación pendiente para " + ocupacion.getArtista().getNombre() + " el " + ocupacion.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " en "+ (ocupacion.getPoblacion()!=null? ocupacion.getPoblacion()+", " : "") + ocupacion.getMunicipio().getNombre() + ", " + ocupacion.getProvincia().getNombre());
		mensaje.setImagen("fa-calendar-plus text-secondary");
		mensaje.setUrlEnlace("/ocupacion/".concat(ocupacion.getId().toString()));
		this.mensajeService.enviarMensaje(mensaje, usuario.getId());
	}

	private void enviarMensajeInternoOcupacionConfirmada(Usuario usuario, Ocupacion ocupacion) {
		Mensaje mensaje = new Mensaje();
		mensaje.setUsuarioRemite(this.userService.obtenerUsuarioAutenticado().orElseThrow());
		mensaje.setUsuarioReceptor(usuario);
		mensaje.setAsunto("Ocupación confirmada");
		mensaje.setMensaje("Ocupación confirmada para " + ocupacion.getArtista().getNombre() + " el " + ocupacion.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " en "+ (ocupacion.getPoblacion()!=null? ocupacion.getPoblacion()+", " : "") + ocupacion.getMunicipio().getNombre() + ", " + ocupacion.getProvincia().getNombre());
		mensaje.setImagen("fa-calendar-plus text-success");
		this.mensajeService.enviarMensaje(mensaje, usuario.getId());
	}

	private void enviarMensajeInternoOcupacionAnulada(Usuario usuario, Ocupacion ocupacion) {
		Mensaje mensaje = new Mensaje();
		mensaje.setUsuarioRemite(this.userService.obtenerUsuarioAutenticado().orElseThrow());
		mensaje.setUsuarioReceptor(usuario);
		mensaje.setAsunto("Ocupación anulada");
		mensaje.setMensaje("Ocupación anulada para " + ocupacion.getArtista().getNombre() + " el " + ocupacion.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " en "+ (ocupacion.getPoblacion()!=null? ocupacion.getPoblacion()+", " : "") + ocupacion.getMunicipio().getNombre() + ", " + ocupacion.getProvincia().getNombre());
		mensaje.setImagen("fa-calendar-plus text-danger");
		this.mensajeService.enviarMensaje(mensaje, usuario.getId());
	}

	@Override
	public Ocupacion guardarOcupacion(OcupacionSaveDto ocupacionSaveDto, boolean permisoConfirmarOcupacionAgencia, boolean isSincronizacion) throws ModificacionOcupacionException {
		log.info("Empezando saveOcupacion: {}", ocupacionSaveDto.toString());

		final Ocupacion ocupacion = ocupacionSaveDto.getId()!=null? this.ocupacionRepository.findById(ocupacionSaveDto.getId()).orElse(new Ocupacion())  : new Ocupacion();

		setDatosUsuarioCreacionModificacion(ocupacionSaveDto.getId()==null, isSincronizacion, ocupacion, ocupacionSaveDto.getIdUsuario());

		final Artista artista = this.artistaRepository.findById(ocupacionSaveDto.getIdArtista()).orElseThrow();
		ocupacion.setArtista(artista);
		ocupacion.setImporte(ocupacionSaveDto.getImporte()!=null? ocupacionSaveDto.getImporte() : BigDecimal.ZERO);
		ocupacion.setPorcentajeRepre(ocupacionSaveDto.getPorcentajeRepre()!=null ? ocupacionSaveDto.getPorcentajeRepre(): BigDecimal.ZERO);
		ocupacion.setIva(ocupacionSaveDto.getIva()!=null?ocupacionSaveDto.getIva(): BigDecimal.ZERO);
		ocupacion.setTipoOcupacion(this.tipoOcupacionRepository.findById(ocupacionSaveDto.getIdTipoOcupacion()).orElseThrow());


		final Long idEstadoOcupacion = getIdEstadoOcupacion(ocupacionSaveDto.getIdTipoOcupacion(), permisoConfirmarOcupacionAgencia, ocupacionSaveDto.getId()==null, ocupacion.getOcupacionEstado() != null ? ocupacion.getOcupacionEstado().getNombre() : null);
		ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(idEstadoOcupacion).orElseThrow());


		ocupacion.setProvincia(this.provinciaRepository.findById(ocupacionSaveDto.getIdProvincia()).orElseThrow());
		if (ocupacionSaveDto.getIdMunicipio()!=null) {
			ocupacion.setMunicipio(this.municipioRepository.findById(ocupacionSaveDto.getIdMunicipio()).orElseThrow());
		} else {
			// Use a default municipality when not specified
			ocupacion.setMunicipio(this.municipioRepository.findById(ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL).orElseThrow());
		}
		ocupacion.setPoblacion(ocupacionSaveDto.getLocalidad()!=null ? ocupacionSaveDto.getLocalidad() : ConstantsGestmusica.LOCALIDAD_PROVISIONAL);
		ocupacion.setLugar(ocupacionSaveDto.getLugar());
		ocupacion.setTarifa(actualizarTarifaSegunOcupacion(ocupacionSaveDto.getIdArtista(), ocupacionSaveDto.getFecha(), ocupacion, isSincronizacion));
        ocupacion.setFecha(ocupacionSaveDto.getFecha());

        ocupacion.setObservaciones(ocupacionSaveDto.getObservaciones());
		ocupacion.setMatinal(ocupacionSaveDto.getMatinal());
		ocupacion.setSoloMatinal(ocupacionSaveDto.getSoloMatinal());
		ocupacion.setProvisional(ocupacionSaveDto.getProvisional());
		ocupacion.setTextoOrquestasDeGalicia(ocupacionSaveDto.getTextoOrquestasDeGalicia());
		if (ocupacion.getIdOcupacionLegacy()==null) {
			ocupacion.setIdOcupacionLegacy(ocupacionSaveDto.getIdOcupacionLegacy());
		}
		ocupacion.setActivo(true);

		return this.ocupacionRepository.save(ocupacion);

	}

	private void setDatosUsuarioCreacionModificacion(boolean isCreacion, boolean isSincronizacion, Ocupacion ocupacion, Long idUsuarioOcupacion) throws ModificacionOcupacionException {
		if (isSincronizacion){
			if (isCreacion){
				ocupacion.setUsuarioCreacion(ConstantsGestmusica.USUARIO_SINCRONIZACION);
				ocupacion.setUsuario(this.userService.findUsuarioById(ConstantsGestmusica.USUARIO_SINCRONIZADOR_ID));
				ocupacion.setFechaCreacion(LocalDateTime.now());
				ocupacion.setUsuarioCreacion(ConstantsGestmusica.USUARIO_SINCRONIZACION);

			}
			else {

				ocupacion.setUsuarioModificacion(ConstantsGestmusica.USUARIO_SINCRONIZACION);
				ocupacion.setFechaModificacion(LocalDateTime.now());

			}
		}
		else {


			final Usuario usuario = idUsuarioOcupacion != null ? this.userService.findUsuarioById(idUsuarioOcupacion) : this.userService.obtenerUsuarioAutenticado().orElseThrow();

			if (isCreacion){
				ocupacion.setUsuario(usuario);
				ocupacion.setFechaCreacion(LocalDateTime.now());
				ocupacion.setUsuarioCreacion(usuario.getUsername());

			}else {

                if (!(ocupacion.getUsuario().getId().equals(usuario.getId())) &&
                        !this.permisoService.existePermisoUsuarioAgencia(ocupacion.getArtista().getAgencia().getId(), PermisoAgenciaEnum.MODIFICAR_OCUPACION_OTROS.name())) {
                    throw new ModificacionOcupacionException("No tiene permisos para modificar ocupaciones de otros usuarios");
                }

                if (idUsuarioOcupacion!=null){
                    ocupacion.setUsuario(usuario);
                }
                ocupacion.setUsuarioModificacion(usuario.getUsername());
				ocupacion.setFechaModificacion(LocalDateTime.now());

			}
		}
	}


	private static ActuacionExterna getActuacionExterna(Artista artista, Ocupacion ocupacion) {
		ActuacionExterna actuacionExterna = new ActuacionExterna();
		actuacionExterna.setIdFormacionExterno(artista.getId().intValue());
		actuacionExterna.setIdActuacionExterno(ocupacion.getId().intValue());
		actuacionExterna.setFecha(ocupacion.getFecha().toLocalDate());
		actuacionExterna.setLugar(ocupacion.getPoblacion());
		actuacionExterna.setProvincia(ocupacion.getProvincia().getNombreOrquestasdegalicia());
		actuacionExterna.setVermu(ocupacion.isMatinal() || ocupacion.isSoloMatinal());
		actuacionExterna.setTarde(false);
		actuacionExterna.setNoche(!ocupacion.isSoloMatinal());
		actuacionExterna.setInformacion(ocupacion.getTextoOrquestasDeGalicia());
		return actuacionExterna;
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

		// Si ambas tienen matinal => existe ocupación
		return (optionalOcupacion.get(0).soloMatinal() == ocupacionSaveDto.getSoloMatinal()) || (optionalOcupacion.get(0).matinal() == ocupacionSaveDto.getMatinal());


	}


	private static Long getIdEstadoOcupacion(final Long idTipoOcupacion, boolean permisoConfirmarOcupacionAgencia, boolean isNuevaOcupacion, String estadoOcupacionActual) {


		if (permisoConfirmarOcupacionAgencia){

			if (isNuevaOcupacion){
				return idTipoOcupacion.equals(TipoOcupacionEnum.RESERVADO.getId()) ? TipoOcupacionEnum.RESERVADO.getId() : OcupacionEstadoEnum.OCUPADO.getId();
			}
			else {
				return OcupacionEstadoEnum.PENDIENTE.getDescripcion().equals(estadoOcupacionActual) ? OcupacionEstadoEnum.PENDIENTE.getId() :  idTipoOcupacion;
			}

		}
		else {
			if (TipoOcupacionEnum.RESERVADO.getId().equals(idTipoOcupacion)){
				return TipoOcupacionEnum.RESERVADO.getId();
			}
			else {
				return OcupacionEstadoEnum.PENDIENTE.getId();
			}

		}

	}

	private Tarifa actualizarTarifaSegunOcupacion(Long idArtista, LocalDateTime fechaDestino, Ocupacion ocupacion, boolean isSincronizacion) {

		Tarifa nuevaTarifa = obtenerTarifaByOcupacion(idArtista, fechaDestino,ocupacion);

		nuevaTarifa.setArtista(ocupacion.getArtista());

        if (nuevaTarifa.getId()!=null && nuevaTarifa.getImporte()!=null){
            nuevaTarifa.setImporte(nuevaTarifa.getImporte());
        } else {
            nuevaTarifa.setImporte(ocupacion.getImporte()!=null ? ocupacion.getImporte() : BigDecimal.ZERO);
        }

		nuevaTarifa.setFecha(fechaDestino);
		nuevaTarifa.setActivo(Boolean.TRUE);

		if (isSincronizacion){
			if (nuevaTarifa.getFechaCreacion()==null){
				nuevaTarifa.setFechaCreacion(LocalDateTime.now());
				nuevaTarifa.setUsuarioCreacion(ConstantsGestmusica.USUARIO_SINCRONIZACION);
			}
			else {
				nuevaTarifa.setFechaModificacion(LocalDateTime.now());
				nuevaTarifa.setUsuarioModificacion(ConstantsGestmusica.USUARIO_SINCRONIZACION);
			}

		}
		else {
			final String userName = this.userService.obtenerUsuarioAutenticado().orElseThrow().getUsername();

			if (nuevaTarifa.getFechaCreacion()==null){
				nuevaTarifa.setFechaCreacion(LocalDateTime.now());
				nuevaTarifa.setUsuarioCreacion(userName);
			}
			else {
				nuevaTarifa.setFechaModificacion(LocalDateTime.now());
				nuevaTarifa.setUsuarioModificacion(userName);
			}

		}


		return this.tarifaRepository.save(nuevaTarifa);


	}

	private Tarifa obtenerTarifaByOcupacion(Long idArtista, LocalDateTime fechaDestino, final Ocupacion ocupacion){
        final List<Tarifa> listaTarifas = this.tarifaRepository.findTarifasByArtistaIdAndDates(idArtista, fechaDestino.withHour(0).withMinute(0).withSecond(0), fechaDestino.withHour(23).withMinute(59).withSecond(59));

        // Si es una modificación
        if (ocupacion.getTarifa()!=null){

            if (ocupacion.getFecha().equals(fechaDestino)){
                return ocupacion.getTarifa();
            }
            else {
                if (listaTarifas!=null && !listaTarifas.isEmpty()){
                    return listaTarifas.get(0);
                }
                else {
                    Tarifa nuevaTarifaDestino = copiarTarifa(ocupacion.getTarifa());
                    nuevaTarifaDestino.setFecha(fechaDestino);
                    return nuevaTarifaDestino;

                }
            }

        }
        else if (listaTarifas!=null && !listaTarifas.isEmpty()){
			return listaTarifas.get(0);
		}

		return new Tarifa();
	}

    private void crearCopiaTarifaFechaAnterior(Long idArtista, Tarifa tarifaAnterior) {
        final List<Tarifa> listaTarifasAnteriores = this.tarifaRepository.findTarifasByArtistaIdAndDates(idArtista, tarifaAnterior.getFecha().withHour(0).withMinute(0).withSecond(0), tarifaAnterior.getFecha().withHour(23).withMinute(59).withSecond(59));

        if (listaTarifasAnteriores!=null && !listaTarifasAnteriores.isEmpty()){
            if (listaTarifasAnteriores.size()==1){

                this.tarifaRepository.save(copiarTarifa(tarifaAnterior));
            }
        }
        else {
            this.tarifaRepository.save(copiarTarifa(tarifaAnterior));
        }
    }

    @Override
	public List<OcupacionRecord> findOcupacionesDtoByAgenciaPendientes(Set<Long> idsAgencia){
		return this.ocupacionRepository.findOcupacionesDtoByAgenciaPendientes(idsAgencia).orElse(new ArrayList<>());
	}

	@Override
public Page<OcupacionListRecord> findOcupacionesByArtistasListAndDatesActivo(CustomAuthenticatedUser user, OcupacionListFilterDto ocupacionListFilterDto, Pageable pageable) {

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

    // Ejecutar la consulta paginada
    Page<Ocupacion> ocupacionesPage = ocupacionRepository.findAll(spec, pageable);
    
    // Mapear resultados a OcupacionListRecord
    List<OcupacionListRecord> ocupacionListRecords = ocupacionesPage.getContent().stream()
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

    return new PageImpl<>(ocupacionListRecords, pageable, ocupacionesPage.getTotalElements());
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
	public Optional<Ocupacion> buscarPorIdOcupacionLegacy(Integer idOcupacionLegacy) {
		return this.ocupacionRepository.findOcupacionByIdOcupacionLegacy(idOcupacionLegacy);
	}


}