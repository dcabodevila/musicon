package es.musicalia.gestmusica.ocupacion;

import es.musicalia.gestmusica.acceso.Acceso;
import es.musicalia.gestmusica.acceso.AccesoRepository;
import es.musicalia.gestmusica.agencia.Agencia;
import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.artista.ArtistaRepository;
import es.musicalia.gestmusica.auth.model.CustomAuthenticatedUser;
import es.musicalia.gestmusica.excel.ExcelExportService;
import es.musicalia.gestmusica.informe.InformeService;
import es.musicalia.gestmusica.localizacion.Municipio;
import es.musicalia.gestmusica.localizacion.MunicipioRepository;
import es.musicalia.gestmusica.localizacion.Provincia;
import es.musicalia.gestmusica.localizacion.ProvinciaRepository;
import es.musicalia.gestmusica.mail.EmailService;
import es.musicalia.gestmusica.mail.EmailTemplateEnum;
import es.musicalia.gestmusica.mensaje.Mensaje;
import es.musicalia.gestmusica.mensaje.MensajeService;
import es.musicalia.gestmusica.orquestasdegalicia.ActuacionExterna;
import es.musicalia.gestmusica.orquestasdegalicia.OrquestasDeGaliciaService;
import es.musicalia.gestmusica.permiso.PermisoAgenciaEnum;
import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.tarifa.Tarifa;
import es.musicalia.gestmusica.tarifa.TarifaRepository;
import es.musicalia.gestmusica.usuario.EnvioEmailException;
import es.musicalia.gestmusica.usuario.UserService;
import es.musicalia.gestmusica.usuario.Usuario;
import es.musicalia.gestmusica.util.ConstantsGestmusica;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Characterization tests of OcupacionServiceImpl that complement OcupacionServiceImplTest:
 * estado transitions on save, ownership rules, notifications, ODG payload and branches,
 * overlap detection on edit and the listado mapping/permission filter.
 */
@ExtendWith(MockitoExtension.class)
class OcupacionServiceEstadosTest {

    @Mock private OcupacionRepository ocupacionRepository;
    @Mock private ArtistaRepository artistaRepository;
    @Mock private ProvinciaRepository provinciaRepository;
    @Mock private MunicipioRepository municipioRepository;
    @Mock private TipoOcupacionRepository tipoOcupacionRepository;
    @Mock private OcupacionEstadoRepository ocupacionEstadoRepository;
    @Mock private TarifaRepository tarifaRepository;
    @Mock private UserService userService;
    @Mock private PermisoService permisoService;
    @Mock private AccesoRepository accesoRepository;
    @Mock private EmailService emailService;
    @Mock private OcupacionMapper ocupacionMapper;
    @Mock private MensajeService mensajeService;
    @Mock private OrquestasDeGaliciaService orquestasDeGaliciaService;
    @Mock private ExcelExportService excelExportService;
    @Mock private InformeService informeService;

    @InjectMocks
    private OcupacionServiceImpl service;

    private static final LocalDateTime FECHA = LocalDateTime.of(2025, 8, 15, 22, 0);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("guardarOcupacion - estado transitions")
    class TransicionesEstado {

        @ParameterizedTest(name = "nueva={0}, permisoConfirmar={1}, tipo={2}, estadoActual={3} -> estado {4}")
        @CsvSource({
                "true,  true,  1, ,          1",
                "true,  true,  2, ,          2",
                "true,  false, 1, ,          3",
                "true,  false, 2, ,          2",
                "false, true,  1, Pendiente, 3",
                "false, true,  2, Ocupado,   2",
                "false, true,  1, Reservado, 1",
                "false, false, 1, Ocupado,   3",
                "false, false, 2, Ocupado,   2"
        })
        void estadoSegunPermisoTipoYEstadoActual(boolean nueva, boolean permisoConfirmar, long idTipo, String estadoActual,
                                                 long estadoEsperado) throws Exception {
            Usuario autor = usuario(1L, "autor");
            Artista artista = artista();
            OcupacionSaveDto dto = dto(nueva ? null : 50L, idTipo);
            if (!nueva) {
                Ocupacion existente = ocupacion(50L, artista, autor);
                existente.setOcupacionEstado(estado(0L, estadoActual));
                when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(existente));
            }
            stubGuardado(artista, autor);

            Ocupacion result = service.guardarOcupacion(dto, permisoConfirmar, false);

            assertThat(result.getOcupacionEstado().getId()).isEqualTo(estadoEsperado);
            assertThat(result.getTipoOcupacion().getId()).isEqualTo(idTipo);
            verify(ocupacionRepository).save(result);
        }
    }

    @Nested
    @DisplayName("guardarOcupacion - ownership and defaults")
    class OwnershipYDefaults {

        @Test
        @DisplayName("Edit of another user's ocupación WITH MODIFICAR_OCUPACION_OTROS keeps the owner and records the modifier")
        void modificacionAjenaConPermiso_permitida() throws Exception {
            Usuario propietario = usuario(1L, "propietario");
            Usuario editor = usuario(2L, "editor");
            Artista artista = artista();
            Ocupacion existente = ocupacion(50L, artista, propietario);
            existente.setOcupacionEstado(estado(1L, "Ocupado"));
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(existente));
            when(permisoService.existePermisoUsuarioAgencia(10L, PermisoAgenciaEnum.MODIFICAR_OCUPACION_OTROS.name())).thenReturn(true);
            stubGuardado(artista, editor);

            Ocupacion result = service.guardarOcupacion(dto(50L, 1L), true, false);

            assertThat(result.getUsuario()).isSameAs(propietario);
            assertThat(result.getUsuarioModificacion()).isEqualTo("editor");
            assertThat(result.getFechaModificacion()).isNotNull();
        }

        @Test
        @DisplayName("Edit of another user's ocupación WITHOUT permission saves neither ocupación nor tarifa")
        void modificacionAjenaSinPermiso_noPersisteNada() {
            Artista artista = artista();
            Ocupacion existente = ocupacion(50L, artista, usuario(1L, "propietario"));
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(existente));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario(2L, "editor")));
            when(permisoService.existePermisoUsuarioAgencia(10L, PermisoAgenciaEnum.MODIFICAR_OCUPACION_OTROS.name())).thenReturn(false);

            assertThatThrownBy(() -> service.guardarOcupacion(dto(50L, 1L), true, false))
                    .isInstanceOf(ModificacionOcupacionException.class)
                    .hasMessage("No tiene permisos para modificar ocupaciones de otros usuarios");
            verify(ocupacionRepository, never()).save(any());
            verifyNoInteractions(tarifaRepository);
        }

        @Test
        @DisplayName("Creation with idUsuario assigns that user as owner and creator")
        void creacionConIdUsuario_asignaPropietario() throws Exception {
            Usuario representante = usuario(7L, "representante");
            Artista artista = artista();
            OcupacionSaveDto dto = dto(null, 1L);
            dto.setIdUsuario(7L);
            when(userService.findUsuarioById(7L)).thenReturn(representante);
            stubGuardado(artista, usuario(1L, "autenticado"));

            Ocupacion result = service.guardarOcupacion(dto, true, false);

            assertThat(result.getUsuario()).isSameAs(representante);
            assertThat(result.getUsuarioCreacion()).isEqualTo("representante");
            assertThat(result.getFechaCreacion()).isNotNull();
        }

        @Test
        @DisplayName("Sync edit records usuario_sincronizacion as modifier without ownership check")
        void sincronizacionModificacion_sinComprobarPropietario() throws Exception {
            Usuario propietario = usuario(1L, "propietario");
            Artista artista = artista();
            Ocupacion existente = ocupacion(50L, artista, propietario);
            existente.setOcupacionEstado(estado(1L, "Ocupado"));
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(existente));
            stubRepositoriosGuardado(artista);

            Ocupacion result = service.guardarOcupacion(dto(50L, 1L), true, true);

            assertThat(result.getUsuario()).isSameAs(propietario);
            assertThat(result.getUsuarioModificacion()).isEqualTo(ConstantsGestmusica.USUARIO_SINCRONIZACION);
            assertThat(result.getTarifa().getUsuarioModificacion()).isEqualTo(ConstantsGestmusica.USUARIO_SINCRONIZACION);
            verifyNoInteractions(permisoService);
            verify(userService, never()).obtenerUsuarioAutenticado();
        }

        @Test
        @DisplayName("Null amounts become zero, null localidad becomes provisional, legacy id and visibility are preserved")
        void valoresPorDefecto() throws Exception {
            Usuario autor = usuario(1L, "autor");
            Artista artista = artista();
            Ocupacion existente = ocupacion(50L, artista, autor);
            existente.setOcupacionEstado(estado(1L, "Ocupado"));
            existente.setIdOcupacionLegacy(777);
            existente.setEventoVisible(false);
            existente.setActivo(false);
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(existente));
            OcupacionSaveDto dto = dto(50L, 1L);
            dto.setImporte(null);
            dto.setPorcentajeRepre(null);
            dto.setIva(null);
            dto.setLocalidad(null);
            dto.setIdOcupacionLegacy(888);
            dto.setEventoVisible(null);
            stubGuardado(artista, autor);

            Ocupacion result = service.guardarOcupacion(dto, true, false);

            assertThat(result.getImporte()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPorcentajeRepre()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getIva()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPoblacion()).isEqualTo(ConstantsGestmusica.LOCALIDAD_PROVISIONAL);
            assertThat(result.getIdOcupacionLegacy()).isEqualTo(777);
            assertThat(result.isEventoVisible()).isFalse();
            assertThat(result.isActivo()).isTrue();
        }
    }

    @Nested
    @DisplayName("saveOcupacion - notifications")
    class SaveNotificaciones {

        @Test
        @DisplayName("Edit without CONFIRMAR_OCUPACION notifies the agency owner of a pending modification")
        void modificacionSinPermiso_notificaModificacionPendiente() throws Exception {
            Usuario autor = usuario(1L, "autor");
            Usuario duenoAgencia = usuario(10L, "agencia");
            Artista artista = artista();
            artista.getAgencia().setUsuario(duenoAgencia);
            Ocupacion existente = ocupacion(50L, artista, autor);
            existente.setOcupacionEstado(estado(1L, "Ocupado"));
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(existente));
            when(permisoService.existePermisoUsuarioAgencia(10L, PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name())).thenReturn(false);
            stubGuardado(artista, autor);

            DefaultResponseBody response = service.saveOcupacion(dto(50L, 1L));

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("success");
            assertThat(response.getIdEntidad()).isEqualTo(50L);
            assertThat(existente.getOcupacionEstado().getId()).isEqualTo(OcupacionEstadoEnum.PENDIENTE.getId());
            ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeService).enviarMensaje(mensaje.capture(), eq(10L));
            assertThat(mensaje.getValue().getAsunto()).isEqualTo("Modificación ocupación pendiente de confirmación");
            assertThat(mensaje.getValue().getMensaje())
                    .isEqualTo("Modificación ocupación pendiente para Orquesta Test el 15/08/2025 en Conxo, Santiago, A Coruña");
            assertThat(mensaje.getValue().getUrlEnlace()).isEqualTo("/ocupacion/50");
            assertThat(mensaje.getValue().getUsuarioReceptor()).isSameAs(duenoAgencia);
            verify(emailService).enviarMensajePorEmail("agencia@test.com", EmailTemplateEnum.EMAIL_NOTIFICACION_CONFIRMACION_PENDIENTE);
        }

        @Test
        @DisplayName("Unexpected notification error keeps the save and returns warning with the id")
        void errorInesperadoNotificando_warningConId() throws Exception {
            Usuario autor = usuario(1L, "autor");
            Artista artista = artista();
            artista.getAgencia().setUsuario(usuario(10L, "agencia"));
            when(permisoService.existePermisoUsuarioAgencia(10L, PermisoAgenciaEnum.CONFIRMAR_OCUPACION.name())).thenReturn(false);
            stubGuardado(artista, autor);
            when(ocupacionRepository.save(any(Ocupacion.class))).thenAnswer(inv -> {
                Ocupacion o = inv.getArgument(0);
                o.setId(60L);
                return o;
            });
            doThrow(new IllegalStateException("smtp")).when(mensajeService).enviarMensaje(any(), anyLong());

            DefaultResponseBody response = service.saveOcupacion(dto(null, 1L));

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("warning");
            assertThat(response.getMessage()).isEqualTo("Ocupacion guardada correctamente, pero ha habido un error enviando la notificación por correo");
            assertThat(response.getIdEntidad()).isEqualTo(60L);
            verify(ocupacionRepository).save(any(Ocupacion.class));
            verifyNoInteractions(emailService);
        }
    }

    @Nested
    @DisplayName("confirmarOcupacion / anularOcupacion")
    class ConfirmarAnular {

        @Test
        @DisplayName("Confirm turns a RESERVADO into estado and tipo OCUPADO and notifies the owner")
        void confirmar_pasaAOcupadoYNotifica() throws Exception {
            Usuario autor = usuario(1L, "autor");
            Usuario confirmador = usuario(2L, "confirmador");
            Ocupacion ocupacion = ocupacion(50L, artista(), autor);
            ocupacion.setOcupacionEstado(estado(2L, "Reservado"));
            ocupacion.setTipoOcupacion(tipo(2L, "Reservado"));
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(ocupacion));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.OCUPADO.getId())).thenReturn(Optional.of(estado(1L, "Ocupado")));
            when(tipoOcupacionRepository.findById(TipoOcupacionEnum.OCUPADO.getId())).thenReturn(Optional.of(tipo(1L, "Ocupado")));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(confirmador));

            DefaultResponseBody response = service.confirmarOcupacion(50L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Ocupacion confirmada correctamente");
            assertThat(ocupacion.getOcupacionEstado().getId()).isEqualTo(1L);
            assertThat(ocupacion.getTipoOcupacion().getId()).isEqualTo(1L);
            assertThat(ocupacion.getUsuarioConfirmacion()).isSameAs(confirmador);
            verify(ocupacionRepository).save(ocupacion);
            ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeService).enviarMensaje(mensaje.capture(), eq(1L));
            assertThat(mensaje.getValue().getAsunto()).isEqualTo("Ocupación confirmada");
            assertThat(mensaje.getValue().getUsuarioRemite()).isSameAs(confirmador);
            verify(emailService).enviarMensajePorEmail("autor@test.com", EmailTemplateEnum.EMAIL_NOTIFICACION_CONFIRMACION);
            verifyNoInteractions(orquestasDeGaliciaService);
        }

        @Test
        @DisplayName("Confirm with unexpected notification error still confirms and returns warning")
        void confirmar_errorInesperado_warning() {
            Ocupacion ocupacion = ocupacion(50L, artista(), usuario(1L, "autor"));
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(ocupacion));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.OCUPADO.getId())).thenReturn(Optional.of(estado(1L, "Ocupado")));
            when(tipoOcupacionRepository.findById(TipoOcupacionEnum.OCUPADO.getId())).thenReturn(Optional.of(tipo(1L, "Ocupado")));
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario(2L, "confirmador")));
            doThrow(new IllegalStateException("mensajes")).when(mensajeService).enviarMensaje(any(), anyLong());

            DefaultResponseBody response = service.confirmarOcupacion(50L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("warning");
            assertThat(response.getMessage()).isEqualTo("Ocupacion confirmada correctamente, pero ha habido un error enviando la notificación por correo");
            assertThat(ocupacion.getOcupacionEstado().getId()).isEqualTo(1L);
            verify(ocupacionRepository).save(ocupacion);
        }

        @Test
        @DisplayName("Anular unpublished: estado ANULADO, same tarifa, owner notified, ODG untouched")
        void anular_noPublicada_notificaSinOdg() throws Exception {
            Usuario autor = usuario(1L, "autor");
            Ocupacion ocupacion = ocupacion(50L, artista(), autor);
            Tarifa tarifa = ocupacion.getTarifa();
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(ocupacion));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.ANULADO.getId())).thenReturn(Optional.of(estado(4L, "Anulado")));
            when(ocupacionRepository.save(ocupacion)).thenReturn(ocupacion);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario(2L, "gestor")));

            DefaultResponseBody response = service.anularOcupacion(50L);

            assertThat(response.getMessage()).isEqualTo("Ocupacion anulada correctamente");
            assertThat(response.getMessageType()).isEqualTo("success");
            assertThat(ocupacion.getOcupacionEstado().getId()).isEqualTo(4L);
            assertThat(ocupacion.getFechaModificacion()).isNotNull();
            assertThat(ocupacion.getTarifa()).isSameAs(tarifa);
            verifyNoInteractions(orquestasDeGaliciaService, tarifaRepository);
            ArgumentCaptor<Mensaje> mensaje = ArgumentCaptor.forClass(Mensaje.class);
            verify(mensajeService).enviarMensaje(mensaje.capture(), eq(1L));
            assertThat(mensaje.getValue().getAsunto()).isEqualTo("Ocupación anulada");
            verify(emailService).enviarMensajePorEmail("autor@test.com", EmailTemplateEnum.EMAIL_NOTIFICACION_ANULACION);
        }

        @Test
        @DisplayName("Anular with ODG and email failures returns the combined warning")
        void anular_falloOdgYEmail_warningCombinado() throws Exception {
            Ocupacion ocupacion = ocupacion(50L, artista(), usuario(1L, "autor"));
            ocupacion.setPublicadoOdg(true);
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(ocupacion));
            when(ocupacionEstadoRepository.findById(OcupacionEstadoEnum.ANULADO.getId())).thenReturn(Optional.of(estado(4L, "Anulado")));
            when(ocupacionRepository.save(ocupacion)).thenReturn(ocupacion);
            when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(usuario(2L, "gestor")));
            when(orquestasDeGaliciaService.eliminarActuacion(50)).thenReturn(null);
            doThrow(new EnvioEmailException("smtp")).when(emailService).enviarMensajePorEmail(anyString(), any());

            DefaultResponseBody response = service.anularOcupacion(50L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessageType()).isEqualTo("warning");
            assertThat(response.getMessage()).isEqualTo("Ocupacion anulada correctamente, pero no se ha podido actualizar en Orquestas de Galicia ni enviar la notificación por correo");
            assertThat(ocupacion.isPublicadoOdg()).isTrue();
            assertThat(ocupacion.getOcupacionEstado().getId()).isEqualTo(4L);
            verify(ocupacionRepository, times(1)).save(ocupacion);
        }
    }

    @Nested
    @DisplayName("existeOcupacionFecha - overlap")
    class Solapamiento {

        @Test
        @DisplayName("Edit checks the whole day excluding its own id")
        void edicion_excluyePropioIdYUsaDiaCompleto() {
            OcupacionSaveDto dto = dto(50L, 1L);
            when(ocupacionRepository.findOcupacionesDtoByArtistaIdAndDatesNotInId(5L,
                    LocalDateTime.of(2025, 8, 15, 0, 0, 0), LocalDateTime.of(2025, 8, 15, 23, 59, 59), 50L))
                    .thenReturn(List.of());

            assertThat(service.existeOcupacionFecha(dto)).isFalse();
            verify(ocupacionRepository, never()).findOcupacionesDtoByArtistaIdAndDates(anyLong(), any(), any());
        }

        @Test
        @DisplayName("More than one ocupación that day is always an overlap")
        void variasOcupaciones_siempreSolapa() {
            OcupacionSaveDto dto = dto(null, 1L);
            dto.setMatinal(true);
            dto.setSoloMatinal(true);
            when(ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(eq(5L), any(), any()))
                    .thenReturn(List.of(record(false, false), record(false, false)));

            assertThat(service.existeOcupacionFecha(dto)).isTrue();
        }

        @Test
        @DisplayName("Two solo-matinal ocupaciones on the same day overlap")
        void dosSoloMatinal_solapan() {
            OcupacionSaveDto dto = dto(null, 1L);
            dto.setMatinal(false);
            dto.setSoloMatinal(true);
            when(ocupacionRepository.findOcupacionesDtoByArtistaIdAndDates(eq(5L), any(), any()))
                    .thenReturn(List.of(record(true, true)));

            assertThat(service.existeOcupacionFecha(dto)).isTrue();
        }
    }

    @Nested
    @DisplayName("Orquestas de Galicia")
    class OrquestasDeGalicia {

        @Test
        @DisplayName("Publish sends the mapped ActuacionExterna: ids, date, normalized place, ODG province and time slots")
        void publicar_enviaActuacionMapeada() {
            Ocupacion ocupacion = ocupacion(50L, artista(), usuario(1L, "autor"));
            ocupacion.setTipoOcupacion(tipo(1L, "Ocupado"));
            ocupacion.getMunicipio().setNombre("SANTIAGO DE COMPOSTELA");
            ocupacion.setPoblacion("conxo");
            ocupacion.getProvincia().setNombreOrquestasdegalicia("A Coruña ODG");
            ocupacion.setMatinal(true);
            ocupacion.setSoloMatinal(false);
            ocupacion.setTextoOrquestasDeGalicia("Fiestas");
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(ocupacion));
            when(orquestasDeGaliciaService.crearActuacion(any())).thenReturn(DefaultResponseBody.builder().success(true).build());

            service.publicarOcupacionOrquestasDeGalicia(50L);

            ArgumentCaptor<ActuacionExterna> captor = ArgumentCaptor.forClass(ActuacionExterna.class);
            verify(orquestasDeGaliciaService).crearActuacion(captor.capture());
            ActuacionExterna actuacion = captor.getValue();
            assertThat(actuacion.getIdActuacionExterno()).isEqualTo(50);
            assertThat(actuacion.getIdFormacionExterno()).isEqualTo(5);
            assertThat(actuacion.getFecha()).isEqualTo(LocalDate.of(2025, 8, 15));
            assertThat(actuacion.getLugar()).isEqualTo("Conxo, Santiago de Compostela");
            assertThat(actuacion.getProvincia()).isEqualTo("A Coruña ODG");
            assertThat(actuacion.getVermu()).isTrue();
            assertThat(actuacion.getTarde()).isFalse();
            assertThat(actuacion.getNoche()).isTrue();
            assertThat(actuacion.getInformacion()).isEqualTo("Fiestas");
            assertThat(ocupacion.isPublicadoOdg()).isTrue();
            assertThat(ocupacion.getFechaPublicacionOdg()).isNotNull();
        }

        @Test
        @DisplayName("Publish with provisional población and 'Coruña, A' municipio sends only the reordered municipio")
        void publicar_poblacionProvisional_soloMunicipioReordenado() {
            Ocupacion ocupacion = ocupacion(50L, artista(), usuario(1L, "autor"));
            ocupacion.setTipoOcupacion(tipo(1L, "Ocupado"));
            ocupacion.getMunicipio().setNombre("Coruña, A");
            ocupacion.setPoblacion("PROVISIONAL");
            ocupacion.setSoloMatinal(true);
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(ocupacion));
            when(orquestasDeGaliciaService.crearActuacion(any())).thenReturn(DefaultResponseBody.builder().success(true).build());

            service.publicarOcupacionOrquestasDeGalicia(50L);

            ArgumentCaptor<ActuacionExterna> captor = ArgumentCaptor.forClass(ActuacionExterna.class);
            verify(orquestasDeGaliciaService).crearActuacion(captor.capture());
            assertThat(captor.getValue().getLugar()).isEqualTo("A Coruña");
            assertThat(captor.getValue().getVermu()).isTrue();
            assertThat(captor.getValue().getNoche()).isFalse();
        }

        @Test
        @DisplayName("Update rejects non-OCUPADO, provisional provincia and provisional municipio without calling ODG")
        void actualizar_validaciones() {
            Ocupacion reservada = ocupacion(51L, artista(), usuario(1L, "autor"));
            reservada.setTipoOcupacion(tipo(2L, "Reservado"));
            Ocupacion provinciaProvisional = ocupacion(52L, artista(), usuario(1L, "autor"));
            provinciaProvisional.setTipoOcupacion(tipo(1L, "Ocupado"));
            provinciaProvisional.setProvincia(provincia(ConstantsGestmusica.ID_PROVINCIA_PROVISIONAL));
            Ocupacion municipioProvisional = ocupacion(53L, artista(), usuario(1L, "autor"));
            municipioProvisional.setTipoOcupacion(tipo(1L, "Ocupado"));
            municipioProvisional.getMunicipio().setId(ConstantsGestmusica.ID_MUNICIPIO_PROVISIONAL);
            when(ocupacionRepository.findById(51L)).thenReturn(Optional.of(reservada));
            when(ocupacionRepository.findById(52L)).thenReturn(Optional.of(provinciaProvisional));
            when(ocupacionRepository.findById(53L)).thenReturn(Optional.of(municipioProvisional));

            assertRechazo(service.actualizarOcupacionOrquestasDeGalicia(51L), "Solo se pueden actualizar en estado ocupada");
            assertRechazo(service.actualizarOcupacionOrquestasDeGalicia(52L), "Es necesario especificar la provincia de la ocupación");
            assertRechazo(service.actualizarOcupacionOrquestasDeGalicia(53L), "Es necesario especificar el municipio de la ocupación");
            verifyNoInteractions(orquestasDeGaliciaService);
            verify(ocupacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Update with an unexpected error returns the generic error and keeps the flag")
        void actualizar_errorInesperado() throws Exception {
            Ocupacion ocupacion = ocupacion(50L, artista(), usuario(1L, "autor"));
            ocupacion.setTipoOcupacion(tipo(1L, "Ocupado"));
            ocupacion.setPublicadoOdg(true);
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(ocupacion));
            when(orquestasDeGaliciaService.obtenerActuacion(50)).thenThrow(new IllegalStateException("json"));

            DefaultResponseBody response = service.actualizarOcupacionOrquestasDeGalicia(50L);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessageType()).isEqualTo("error");
            assertThat(response.getMessage()).isEqualTo("Error al actualizar la actuación en OrquestasDeGalicia");
            assertThat(ocupacion.isPublicadoOdg()).isTrue();
            verify(ocupacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Delete rejected by ODG keeps the publication flags and does not save")
        void eliminar_rechazado_noModifica() {
            Ocupacion ocupacion = ocupacion(50L, artista(), usuario(1L, "autor"));
            ocupacion.setPublicadoOdg(true);
            DefaultResponseBody odg = DefaultResponseBody.builder().success(false).message("404").build();
            when(ocupacionRepository.findById(50L)).thenReturn(Optional.of(ocupacion));
            when(orquestasDeGaliciaService.eliminarActuacion(anyInt())).thenReturn(odg);

            DefaultResponseBody response = service.eliminarOcupacionOrquestasDeGalicia(50L);

            assertThat(response).isSameAs(odg);
            assertThat(ocupacion.isPublicadoOdg()).isTrue();
            assertThat(ocupacion.isExcluirSincronizacionOdg()).isFalse();
            verify(ocupacionRepository, never()).save(any());
        }

        private void assertRechazo(DefaultResponseBody response, String message) {
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessageType()).isEqualTo("danger");
            assertThat(response.getMessage()).isEqualTo(message);
        }
    }

    @Nested
    @DisplayName("findOcupacionesByArtistasListAndDatesActivoPaginado - listado")
    class Listado {

        @Test
        @DisplayName("Maps entities to list records and restricts to the principal's artistas with OCUPACIONES")
        void mapeaRegistrosYFiltraArtistasConPermiso() {
            CustomAuthenticatedUser principal = principal(Map.of(5L, Set.of("OCUPACIONES"), 6L, Set.of("VER_TARIFAS")));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, List.of()));
            Usuario confirmador = usuario(3L, "conf");
            Ocupacion ocupacion = ocupacion(50L, artista(), usuario(1L, "autor"));
            ocupacion.setTipoOcupacion(tipo(1L, "Ocupado"));
            ocupacion.setOcupacionEstado(estado(1L, "Ocupado"));
            ocupacion.setImporte(new BigDecimal("1500"));
            ocupacion.setUsuarioConfirmacion(confirmador);
            ocupacion.setFechaCreacion(LocalDateTime.of(2025, 6, 1, 10, 0));
            Pageable pageable = PageRequest.of(0, 10);
            when(ocupacionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(ocupacion)));
            OcupacionListFilterDto filtro = OcupacionListFilterDto.builder().fechaDesde(LocalDate.of(2025, 8, 1)).build();

            Page<OcupacionListRecord> page = service.findOcupacionesByArtistasListAndDatesActivoPaginado(principal, filtro, null, pageable);

            OcupacionListRecord record = page.getContent().get(0);
            assertThat(record.id()).isEqualTo(50L);
            assertThat(record.start()).isEqualTo(FECHA);
            assertThat(record.idArtista()).isEqualTo(5L);
            assertThat(record.artista()).isEqualTo("Orquesta Test");
            assertThat(record.importe()).isEqualTo("1500");
            assertThat(record.tipoOcupacion()).isEqualTo("Ocupado");
            assertThat(record.provincia()).isEqualTo("A Coruña");
            assertThat(record.municipio()).isEqualTo("Santiago");
            assertThat(record.localidad()).isEqualTo("Conxo");
            assertThat(record.estado()).isEqualTo("Ocupado");
            assertThat(record.idUsuario()).isEqualTo(1L);
            assertThat(record.nombreUsuario()).isEqualTo("Nombre autor Apellidos");
            assertThat(record.idUsuarioConfirmacion()).isEqualTo(3L);
            assertThat(record.usuarioConfirmacion()).isEqualTo("Nombre conf Apellidos");
            assertThat(record.fechaCreacion()).isEqualTo(LocalDateTime.of(2025, 6, 1, 10, 0));
            verifyNoInteractions(accesoRepository);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Specification<Ocupacion>> spec = ArgumentCaptor.forClass(Specification.class);
            verify(ocupacionRepository).findAll(spec.capture(), eq(pageable));
            @SuppressWarnings("unchecked")
            Root<Ocupacion> root = mock(Root.class, RETURNS_DEEP_STUBS);
            spec.getValue().toPredicate(root, mock(CriteriaQuery.class), mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS));
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<?>> ids = ArgumentCaptor.forClass(Collection.class);
            verify(root.get("artista").get("id")).in(ids.capture());
            assertThat(ids.getValue()).isEqualTo(Set.of(5L));
        }

        @Test
        @DisplayName("With idAgencia, the representante role of the requesting user is looked up")
        void conAgencia_consultaRolRepresentante() {
            CustomAuthenticatedUser principal = principal(Map.of(5L, Set.of("OCUPACIONES")));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, List.of()));
            when(ocupacionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
            when(accesoRepository.findAccesoByIdUsuarioAndIdAgenciaAndCodigoRol(1L, 10L, RolEnum.ROL_REPRESENTANTE.getCodigo()))
                    .thenReturn(Optional.of(new Acceso()));
            OcupacionListFilterDto filtro = OcupacionListFilterDto.builder().idAgencia(10L).fechaDesde(LocalDate.of(2025, 8, 1)).build();

            List<OcupacionListRecord> result = service.findOcupacionesByArtistasListAndDatesActivo(principal, filtro);

            assertThat(result).isEmpty();
            verify(accesoRepository).findAccesoByIdUsuarioAndIdAgenciaAndCodigoRol(1L, 10L, RolEnum.ROL_REPRESENTANTE.getCodigo());
            verify(ocupacionRepository).findAll(any(Specification.class), org.mockito.ArgumentMatchers.<Pageable>argThat(p -> !p.isPaged()));
        }
    }

    // ---------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------

    private void stubGuardado(Artista artista, Usuario autenticado) {
        lenient().when(userService.obtenerUsuarioAutenticado()).thenReturn(Optional.of(autenticado));
        stubRepositoriosGuardado(artista);
    }

    private void stubRepositoriosGuardado(Artista artista) {
        lenient().when(artistaRepository.findById(5L)).thenReturn(Optional.of(artista));
        lenient().when(tipoOcupacionRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(tipo(inv.getArgument(0), "tipo")));
        lenient().when(ocupacionEstadoRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(estado(inv.getArgument(0), "estado")));
        lenient().when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provincia(1L)));
        lenient().when(municipioRepository.findById(1L)).thenReturn(Optional.of(municipio(1L)));
        lenient().when(tarifaRepository.findTarifasByArtistaIdAndDates(anyLong(), any(), any())).thenReturn(List.of());
        lenient().when(tarifaRepository.save(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(ocupacionRepository.save(any(Ocupacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static OcupacionSaveDto dto(Long id, long idTipo) {
        OcupacionSaveDto dto = new OcupacionSaveDto();
        dto.setId(id);
        dto.setIdArtista(5L);
        dto.setIdTipoOcupacion(idTipo);
        dto.setIdCcaa(1L);
        dto.setIdProvincia(1L);
        dto.setIdMunicipio(1L);
        dto.setLocalidad("Conxo");
        dto.setImporte(new BigDecimal("1000"));
        dto.setPorcentajeRepre(new BigDecimal("10"));
        dto.setIva(new BigDecimal("21"));
        dto.setFecha(FECHA);
        // Ocupacion.setProvisional takes a primitive boolean: a null here would fail with NPE
        dto.setProvisional(false);
        return dto;
    }

    private static Artista artista() {
        Agencia agencia = new Agencia();
        agencia.setId(10L);
        Artista artista = new Artista();
        artista.setId(5L);
        artista.setNombre("Orquesta Test");
        artista.setAgencia(agencia);
        return artista;
    }

    private static Usuario usuario(long id, String username) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setEmail(username + "@test.com");
        usuario.setNombre("Nombre " + username);
        usuario.setApellidos("Apellidos");
        usuario.setPassword("secret");
        return usuario;
    }

    private static Ocupacion ocupacion(long id, Artista artista, Usuario propietario) {
        Tarifa tarifa = new Tarifa();
        tarifa.setId(90L);
        tarifa.setImporte(new BigDecimal("1000"));
        tarifa.setFecha(FECHA);
        tarifa.setFechaCreacion(LocalDateTime.of(2025, 1, 1, 0, 0));
        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setId(id);
        ocupacion.setArtista(artista);
        ocupacion.setUsuario(propietario);
        ocupacion.setFecha(FECHA);
        ocupacion.setImporte(new BigDecimal("1000"));
        ocupacion.setProvincia(provincia(1L));
        ocupacion.setMunicipio(municipio(1L));
        ocupacion.setPoblacion("Conxo");
        ocupacion.setTarifa(tarifa);
        ocupacion.setActivo(true);
        return ocupacion;
    }

    private static Provincia provincia(long id) {
        Provincia provincia = new Provincia();
        provincia.setId(id);
        provincia.setNombre("A Coruña");
        provincia.setNombreOrquestasdegalicia("A Coruña");
        return provincia;
    }

    private static Municipio municipio(long id) {
        Municipio municipio = new Municipio();
        municipio.setId(id);
        municipio.setNombre("Santiago");
        return municipio;
    }

    private static TipoOcupacion tipo(long id, String nombre) {
        TipoOcupacion tipo = new TipoOcupacion();
        tipo.setId(id);
        tipo.setNombre(nombre);
        return tipo;
    }

    private static OcupacionEstado estado(long id, String nombre) {
        OcupacionEstado estado = new OcupacionEstado();
        estado.setId(id);
        estado.setNombre(nombre);
        return estado;
    }

    private static OcupacionRecord record(boolean matinal, boolean soloMatinal) {
        return new OcupacionRecord(1L, FECHA, 5L, "Orquesta Test", "1000", true, "Ocupado", "A Coruña", "Santiago",
                "Conxo", matinal, soloMatinal, "Ocupado", 1L, "Usuario");
    }

    private static CustomAuthenticatedUser principal(Map<Long, Set<String>> permisosArtista) {
        return new CustomAuthenticatedUser(usuario(1L, "autor"), true, true, true, true, List.of(), permisosArtista, Map.of());
    }
}
