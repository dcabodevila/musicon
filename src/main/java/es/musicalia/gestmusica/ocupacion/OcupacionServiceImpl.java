package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.acceso.AccesoRepository;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.excel.ExcelExportService;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.localizacion.CodigoNombreDto;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.mensaje.Mensaje;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.orquestasdegalicia.ActuacionExterna;
import es.musicalia.gestmusica.orquestasdegalicia.OrquestasDeGaliciaService;
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
import es.musicalia.gestmusica.util.DateUtils;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
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
    private final OrquestasDeGaliciaService orquestasDeGaliciaService;
	private final ExcelExportService excelExportService;
	private final InformeService informeService;

	public OcupacionServiceImpl(OcupacionRepository ocupacionRepository, ArtistaRepository artistaRepository, ProvinciaRepository provinciaRepository, MunicipioRepository municipioRepository, TipoOcupacionRepository tipoOcupacionRepository, OcupacionEstadoRepository ocupacionEstadoRepository, TarifaRepository tarifaRepository, UserService userService, PermisoService permisoService, AccesoRepository accesoRepository, EmailService emailService, OcupacionMapper ocupacionMapper, MensajeService mensajeService, OrquestasDeGaliciaService orquestasDeGaliciaService, ExcelExportService excelExportService, InformeService informeService){
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
        this.orquestasDeGaliciaService = orquestasDeGaliciaService;
		this.excelExportService = excelExportService;
        this.informeService = informeService;
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

		Ocupacion ocupacion =  this.ocupacionRepository.findById(id).orElseThrow();

        final Tarifa nuevaTarifa = actualizarTarifasAnularOcupacion(ocupacion.getTarifa());

        ocupacion.setOcupacionEstado(this.ocupacionEstadoRepository.findById(OcupacionEstadoEnum.ANULADO.getId()).orElseThrow());
		ocupacion.setFechaModificacion(LocalDateTime.now());
		ocupacion.setTarifa(nuevaTarifa);
		ocupacion = this.ocupacionRepository.save(ocupacion);

        if (ocupacion.isPublicadoOdg()){
            try {
                this.orquestasDeGaliciaService.eliminarActuacion(ocupacion.getId().intValue());
				ocupacion.setPublicadoOdg(false);
				this.ocupacionRepository.save(ocupacion);
            } catch (Exception e) {
                log.error("error inesperado eliminando actuacion de orquestas de galicia", e);
            }
        }

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

    private Tarifa actualizarTarifasAnularOcupacion(Tarifa oldTarifa) {
        Tarifa nuevaTarifa = copiarTarifa(oldTarifa);

        oldTarifa.setActivo(Boolean.FALSE);

        this.tarifaRepository.save(oldTarifa);
        return this.tarifaRepository.save(nuevaTarifa);
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

			ActuacionExterna actuacionExterna = getActuacionExterna(ocupacion);

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
		ocupacion.setHoraActuacion(ocupacionSaveDto.getHoraActuacion());
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


	private static ActuacionExterna getActuacionExterna(Ocupacion ocupacion) {
		ActuacionExterna actuacionExterna = new ActuacionExterna();
		actuacionExterna.setIdFormacionExterno(ocupacion.getArtista().getId().intValue());
		actuacionExterna.setIdActuacionExterno(ocupacion.getId().intValue());
		actuacionExterna.setFecha(ocupacion.getFecha().toLocalDate());

        final String lugar = obtenerLugarOrquestaDeGalicia(ocupacion.getMunicipio().getNombre(), ocupacion.getPoblacion());

		actuacionExterna.setLugar(lugar);
		actuacionExterna.setProvincia(ocupacion.getProvincia().getNombreOrquestasdegalicia());
		actuacionExterna.setVermu(ocupacion.isMatinal() || ocupacion.isSoloMatinal());
		actuacionExterna.setTarde(false);
		actuacionExterna.setNoche(!ocupacion.isSoloMatinal());
		actuacionExterna.setInformacion(ocupacion.getTextoOrquestasDeGalicia());

		return actuacionExterna;
	}

	private static String obtenerLugarOrquestaDeGalicia(final String municipio, final String poblacion){
		final StringBuilder sb = new StringBuilder();

		final String municipioNormalizado = capitalizarNombreMunicipio(municipio);
		final String poblacionNormalizada = capitalizarNombreMunicipio(poblacion);

		if (poblacion != null && !poblacion.isEmpty() && !poblacion.equalsIgnoreCase("PROVISIONAL")) {
			// Comparar sin considerar el case
			final boolean poblacionCoincideConMunicipio = municipioNormalizado != null &&
					(poblacionNormalizada.equalsIgnoreCase(municipioNormalizado) ||
							municipioNormalizado.toUpperCase().contains(poblacionNormalizada.toUpperCase()));

			if (poblacionCoincideConMunicipio) {
				// Si coinciden, mostrar solo el municipio
				sb.append(municipioNormalizado);
			} else {
				// Si no coinciden, mostrar población, municipio
				sb.append(poblacionNormalizada);

				if (municipioNormalizado != null && !municipioNormalizado.isEmpty()) {
					sb.append(", ").append(municipioNormalizado);
				}
			}
		} else {
			// Si no hay población o es provisional, mostrar solo municipio
			sb.append(municipioNormalizado);
		}

		return sb.toString();
	}


	private static String capitalizarNombreMunicipio(String nombre) {
		if (nombre == null || nombre.isEmpty()) {
			return nombre;
		}

		// Palabras que deben ir en minúscula (preposiciones y artículos en gallego/castellano)
		Set<String> minusculas = Set.of(
				"de", "do", "da", "dos", "das",
				"del", "los", "las", "el", "la",
				"e", "y", "o", "a"
		);

		String[] partes = nombre.trim().split(",");

		// Si hay coma, reorganizar: la parte después de la coma va al principio
		if (partes.length > 1) {
			// Invertir el orden: [parte_después_coma, parte_antes_coma]
			String[] partesReorganizadas = new String[partes.length];
			for (int i = 0; i < partes.length; i++) {
				partesReorganizadas[i] = partes[partes.length - 1 - i];
			}
			partes = partesReorganizadas;
		}

		StringBuilder sbFinal = new StringBuilder();

		for (int p = 0; p < partes.length; p++) {
			String parte = partes[p].trim();
			String[] palabras = parte.toLowerCase().split("\\s+");
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < palabras.length; i++) {
				String palabra = palabras[i];
				// La primera palabra de cada parte siempre va en mayúscula
				if (i == 0 || !minusculas.contains(palabra)) {
					sb.append(Character.toUpperCase(palabra.charAt(0)))
							.append(palabra.substring(1));
				} else {
					sb.append(palabra);
				}
				if (i < palabras.length - 1) {
					sb.append(" ");
				}
			}

			sbFinal.append(sb.toString());
			if (p < partes.length - 1) {
				sbFinal.append(" ");
			}
		}

		return sbFinal.toString();
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
	public List<OcupacionListRecord> findOcupacionesByArtistasListAndDatesActivo(CustomAuthenticatedUser user, OcupacionListFilterDto ocupacionListFilterDto) {
		return findOcupacionesByArtistasListAndDatesActivoPaginado(user, ocupacionListFilterDto, null, Pageable.unpaged()).getContent();
	}

	@Override
	public Page<OcupacionListRecord> findOcupacionesByArtistasListAndDatesActivoPaginado(CustomAuthenticatedUser user,
																							 OcupacionListFilterDto ocupacionListFilterDto,
																							 String searchValue,
																							 Pageable pageable) {
		Specification<Ocupacion> spec = buildOcupacionListadoSpecification(user, ocupacionListFilterDto, searchValue, !pageable.isPaged());
		return ocupacionRepository.findAll(spec, pageable).map(this::toOcupacionListRecord);
	}

	private Specification<Ocupacion> buildOcupacionListadoSpecification(CustomAuthenticatedUser user,
																				 OcupacionListFilterDto ocupacionListFilterDto,
																				 String searchValue,
																				 boolean includeDefaultOrder) {
		final CustomAuthenticatedUser authenticatedUser = (CustomAuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		final Set<Long> idsArtistas = authenticatedUser.getMapPermisosArtista().keySet().stream()
				.filter(artistaId -> authenticatedUser.getMapPermisosArtista().get(artistaId).contains(PermisoArtistaEnum.OCUPACIONES.name()))
				.collect(Collectors.toSet());

		Specification<Ocupacion> specification = Specification
				.where(OcupacionSpecifications.hasArtistaIdsIn(idsArtistas))
				.and(OcupacionSpecifications.hasFechaAfter(ocupacionListFilterDto.getFechaDesde().atStartOfDay()))
				.and(OcupacionSpecifications.hasFechaBefore(ocupacionListFilterDto.getFechaHasta() != null ? ocupacionListFilterDto.getFechaHasta().atTime(23, 59, 59) : null))
				.and(OcupacionSpecifications.isActivo())
				.and(OcupacionSpecifications.hasAgenciaId(ocupacionListFilterDto.getIdAgencia()))
				.and(OcupacionSpecifications.hasArtistaId(ocupacionListFilterDto.getIdArtista()))
				.and(OcupacionSpecifications.hasEstadoNotAnulado())
				.and(OcupacionSpecifications.hasUsuarioId(isRolRepresentante(user.getUserId(), ocupacionListFilterDto.getIdAgencia()), user.getUserId()))
				.and(OcupacionSpecifications.hasGlobalSearch(searchValue));

		if (includeDefaultOrder) {
			specification = specification.and(OcupacionSpecifications.orderByIdDesc());
		}

		return specification;
	}

	private OcupacionListRecord toOcupacionListRecord(Ocupacion ocupacion) {
		return new OcupacionListRecord(
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
		);
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
    @Transactional
    @Override
    public DefaultResponseBody publicarOcupacionOrquestasDeGalicia(Long idOcupacion) {

        final Ocupacion ocupacion = this.ocupacionRepository.findById(idOcupacion).orElseThrow();


        if (!TipoOcupacionEnum.OCUPADO.getDescripcion().equalsIgnoreCase(ocupacion.getTipoOcupacion().getNombre())){
            return DefaultResponseBody.builder().idEntidad(idOcupacion).success(false).messageType("danger").message("Solo se pueden publicar en estado ocupada").build();
        }

        if (ocupacion.getProvincia()==null || ConstantsGestmusica.ID_PROVINCIA_PROVISIONAL == ocupacion.getProvincia().getId()){
            return DefaultResponseBody.builder().idEntidad(idOcupacion).success(false).messageType("danger").message("Es necesario especificar la provincia de la ocupación").build();
        }

        if (ocupacion.getMunicipio()==null || ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL == ocupacion.getMunicipio().getId()){

            return DefaultResponseBody.builder().idEntidad(idOcupacion).success(false).messageType("danger").message("Es necesario especificar el municipio de la ocupación").build();

        }

        // Publicar en ODG
        final ActuacionExterna actuacionExterna = getActuacionExterna(ocupacion);
        final DefaultResponseBody response = this.orquestasDeGaliciaService.crearActuacion(actuacionExterna);

        if (response.isSuccess()){
            ocupacion.setPublicadoOdg(true);
            ocupacion.setFechaPublicacionOdg(LocalDateTime.now());
            ocupacionRepository.save(ocupacion);
        }

        return response;

    }
    @Transactional
    @Override
    public DefaultResponseBody actualizarOcupacionOrquestasDeGalicia(Long idOcupacion) throws OrquestasDeGaliciaException {

        final Ocupacion ocupacion = this.ocupacionRepository.findById(idOcupacion).orElseThrow();

        if (!TipoOcupacionEnum.OCUPADO.getDescripcion().equalsIgnoreCase(ocupacion.getTipoOcupacion().getNombre())) {
            return DefaultResponseBody.builder().idEntidad(idOcupacion).success(false).messageType("danger").message("Solo se pueden actualizar en estado ocupada").build();
        }

        if (ocupacion.getProvincia() == null || ConstantsGestmusica.ID_PROVINCIA_PROVISIONAL == ocupacion.getProvincia().getId()) {
            return DefaultResponseBody.builder().idEntidad(idOcupacion).success(false).messageType("danger").message("Es necesario especificar la provincia de la ocupación").build();
        }

        if (ocupacion.getMunicipio() == null || ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL == ocupacion.getMunicipio().getId()) {
            return DefaultResponseBody.builder().idEntidad(idOcupacion).success(false).messageType("danger").message("Es necesario especificar el municipio de la ocupación").build();
        }

        if (!ocupacion.isPublicadoOdg()) {
            return DefaultResponseBody.builder().idEntidad(idOcupacion).success(false).messageType("danger").message("La ocupación no ha sido publicada en OrquestasDeGalicia").build();
        }

        // Actualizar en ODG
        final ActuacionExterna actuacionExterna = getActuacionExterna(ocupacion);
        try {
            Optional<ActuacionExterna> actuacionExternaAModificar = this.orquestasDeGaliciaService.obtenerActuacion(actuacionExterna.getIdActuacionExterno());
            if (actuacionExternaAModificar.isEmpty()){

                ocupacion.setPublicadoOdg(false);
                ocupacionRepository.save(ocupacion);

                return DefaultResponseBody.builder().success(false).messageType("error").message("No se ha encontrado la actuación a modificar en Orquestas de Galicia").build();
            }
            final DefaultResponseBody response = this.orquestasDeGaliciaService.modificarActuacion(actuacionExterna.getIdActuacionExterno(), actuacionExterna);

            if (response.isSuccess()) {

                ocupacion.setFechaPublicacionOdg(LocalDateTime.now());
                ocupacionRepository.save(ocupacion);
            }

            return response;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return DefaultResponseBody.builder().success(false).messageType("error").message("Error al actualizar la actuación en OrquestasDeGalicia").build();




    }

    @Transactional
    @Override
    public DefaultResponseBody eliminarOcupacionOrquestasDeGalicia(Long idOcupacion) {
        final Ocupacion ocupacion = this.ocupacionRepository.findById(idOcupacion).orElseThrow();

        if (!ocupacion.isPublicadoOdg()) {
            return DefaultResponseBody.builder().idEntidad(idOcupacion).success(false).messageType("danger").message("La ocupación no ha sido publicada en OrquestasDeGalicia").build();
        }

        // Eliminar de ODG
        final DefaultResponseBody response = this.orquestasDeGaliciaService.eliminarActuacion(ocupacion.getId().intValue());

        if (response.isSuccess()){
            ocupacion.setPublicadoOdg(false);
            ocupacion.setFechaPublicacionOdg(LocalDateTime.now());
            ocupacionRepository.save(ocupacion);
        }


        return response ;
    }

	@Override
	public ByteArrayOutputStream exportOcupacionesToExcel(CustomAuthenticatedUser user, OcupacionListFilterDto ocupacionListFilterDto) {
		// Obtener las ocupaciones filtradas
		List<OcupacionListRecord> ocupaciones = findOcupacionesByArtistasListAndDatesActivo(user, ocupacionListFilterDto);

		// Convertir a DTOs para Excel y ordenar por fecha ascendente
		List<OcupacionExcelDto> datosExcel = ocupaciones.stream()
				.sorted(Comparator.comparing(OcupacionListRecord::start))
				.map(this::mapToOcupacionExcelDto)
				.collect(Collectors.toList());

		// Exportar a Excel
		return excelExportService.exportToExcel(datosExcel, OcupacionExcelDto.class, "Ocupaciones");
	}

	private OcupacionExcelDto mapToOcupacionExcelDto(OcupacionListRecord ocupacion) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

		// Buscar el usuario para obtener nombre comercial y teléfono
		Usuario usuario = userService.findUsuarioById(ocupacion.idUsuario());

		return OcupacionExcelDto.builder()
				.id(ocupacion.id())
				.artista(ocupacion.artista() != null ? ocupacion.artista() : "")
				.fecha(ocupacion.start().format(formatter))
				.localidad(ocupacion.localidad() != null ? ocupacion.localidad() : "")
				.municipio(ocupacion.municipio() != null ? ocupacion.municipio() : "")
				.provincia(ocupacion.provincia() != null ? ocupacion.provincia() : "")
				.matinal(ocupacion.matinal() ? "Sí" : "No")
				.soloMatinal(ocupacion.soloMatinal() ? "Sí" : "No")
				.estado(ocupacion.estado() != null ? ocupacion.estado() : "")
				.nombreComercialRepresentante(usuario.getNombreComercial() != null ? usuario.getNombreComercial() : "")
				.telefonoRepresentante(usuario.getTelefono() != null ? usuario.getTelefono() : "")
				.build();
	}

	@Override
	public byte[] exportOcupacionesToPDF(CustomAuthenticatedUser user, OcupacionListFilterDto ocupacionListFilterDto) {
		// Obtener las ocupaciones filtradas y convertir a DTOs
		List<OcupacionListRecord> ocupaciones = findOcupacionesByArtistasListAndDatesActivo(user, ocupacionListFilterDto);

		List<OcupacionExcelDto> datosOcupaciones = ocupaciones.stream()
				.sorted(Comparator.comparing(OcupacionListRecord::start))
				.map(this::mapToOcupacionExcelDto)
				.collect(Collectors.toList());

		// Crear DataSource con la colección de ocupaciones
		JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datosOcupaciones);

		// Preparar parámetros para el informe JasperReports
		Map<String, Object> parametros = new HashMap<>();
		parametros.put("titulo", "Listado de Ocupaciones");
		parametros.put("fechaGeneracion", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

		// Agregar filtros aplicados
		StringBuilder filtrosAplicados = new StringBuilder();
		if (ocupacionListFilterDto.getIdAgencia() != null) {
			filtrosAplicados.append("Agencia filtrada | ");
		}
		if (ocupacionListFilterDto.getIdArtista() != null) {
			filtrosAplicados.append("Artista filtrado | ");
		}
		if (ocupacionListFilterDto.getFechaDesde() != null) {
			filtrosAplicados.append("Desde: ").append(ocupacionListFilterDto.getFechaDesde().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
		}
		if (ocupacionListFilterDto.getFechaHasta() != null) {
			filtrosAplicados.append(" Hasta: ").append(ocupacionListFilterDto.getFechaHasta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
		}
		parametros.put("filtros", filtrosAplicados.toString());

		String fileNameToExport = "Ocupaciones_" + DateUtils.getDateStr(new java.util.Date(), "ddMMyyyyHHmmss") + ".pdf";

		// Usar el nuevo reporte de listado de ocupaciones
		String fileReport = "listado_ocupaciones.jrxml";

		return informeService.imprimirInformeConDataSource(parametros, fileNameToExport, fileReport, dataSource);
	}

}
