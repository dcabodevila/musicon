package es.musicalia.gestmusica.orquestasdegalicia;

import es.musicalia.gestmusica.artista.Artista;
import es.musicalia.gestmusica.ocupacion.Ocupacion;
import es.musicalia.gestmusica.ocupacion.OcupacionEstadoEnum;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OdgSincronizacionServiceTest {

    @Mock
    private OcupacionRepository ocupacionRepository;

    @Mock
    private OcupacionService ocupacionService;

    @Mock
    private OdgSincronizacionTrackingRepository trackingRepository;

    @InjectMocks
    private OdgSincronizacionService odgSincronizacionService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static final Long ID_ESTADO_OCUPADO = OcupacionEstadoEnum.OCUPADO.getId();   // 1L
    private static final Long ID_ESTADO_ANULADO = OcupacionEstadoEnum.ANULADO.getId();   // 4L

    private Ocupacion crearOcupacion(Long id, LocalDateTime fechaModificacion) {
        Artista artista = new Artista();
        artista.setId(10L);

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setId(id);
        ocupacion.setArtista(artista);
        ocupacion.setFecha(LocalDate.now().plusMonths(1).atStartOfDay());
        ocupacion.setFechaModificacion(fechaModificacion);
        return ocupacion;
    }

    private DefaultResponseBody responseOk(String messageType) {
        return DefaultResponseBody.builder()
                .success(true)
                .message("OK")
                .messageType(messageType)
                .build();
    }

    private DefaultResponseBody responseError(String messageType) {
        return DefaultResponseBody.builder()
                .success(false)
                .message("Error de validacion")
                .messageType(messageType)
                .build();
    }

    // -------------------------------------------------------------------------
    // Tests de debeSkipear — lógica de skip por error de validación previo
    // -------------------------------------------------------------------------

    @Nested
    class DebeSkipear {

        @Test
        void ocupacion_sin_error_previo_no_debe_skipear() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(1L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(1L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(1L))
                    .thenReturn(responseOk("success"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN: se ejecuta la operación, no se registra skip
            verify(ocupacionService).publicarOcupacionOrquestasDeGalicia(1L);
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getResultado()).isEqualTo("OK");
        }

        @Test
        void ocupacion_con_error_previo_y_sin_modificaciones_debe_skipear() throws Exception {
            // GIVEN: el error de validación ocurrió ayer, y fechaModificacion también es ayer (no hay cambios nuevos)
            LocalDateTime ayer = LocalDateTime.now().minusDays(1);
            Ocupacion ocupacion = crearOcupacion(2L, ayer);
            LocalDateTime fechaError = LocalDateTime.now().minusHours(1); // error más reciente que la modificación

            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(2L))
                    .thenReturn(Optional.of(fechaError));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN: se registra SKIPPED, no se llama a publicar
            verify(ocupacionService, never()).publicarOcupacionOrquestasDeGalicia(any());
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            OdgSincronizacionTracking tracking = captor.getValue();
            assertThat(tracking.getResultado()).isEqualTo("SKIPPED");
            assertThat(tracking.getMessageType()).isEqualTo("info");
            assertThat(tracking.getAccion()).isEqualTo("CREAR");
            assertThat(tracking.getOcupacionId()).isEqualTo(2L);
            assertThat(tracking.getArtistaId()).isEqualTo(10L);
        }

        @Test
        void ocupacion_con_error_previo_y_modificada_despues_no_debe_skipear() throws Exception {
            // GIVEN: el error ocurrió antes de la última modificación → datos actualizados, hay que reintentar
            LocalDateTime fechaError = LocalDateTime.now().minusDays(2);
            LocalDateTime fechaModificacion = LocalDateTime.now().minusDays(1); // modificada DESPUÉS del error
            Ocupacion ocupacion = crearOcupacion(3L, fechaModificacion);

            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(3L))
                    .thenReturn(Optional.of(fechaError));
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(3L))
                    .thenReturn(responseOk("success"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN: se procesa normalmente
            verify(ocupacionService).publicarOcupacionOrquestasDeGalicia(3L);
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getResultado()).isEqualTo("OK");
        }

        @Test
        void ocupacion_con_fechaModificacion_null_y_error_previo_debe_skipear() throws Exception {
            // GIVEN: fechaModificacion es null → no se puede saber si se actualizó → skip conservador
            Ocupacion ocupacion = crearOcupacion(4L, null); // null fechaModificacion
            LocalDateTime fechaError = LocalDateTime.now().minusHours(3);

            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(4L))
                    .thenReturn(Optional.of(fechaError));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN: skip porque fechaModificacion == null → condición `return fechaModificacion == null || ...`
            verify(ocupacionService, never()).publicarOcupacionOrquestasDeGalicia(any());
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getResultado()).isEqualTo("SKIPPED");
        }

        @Test
        void ocupacion_con_error_transitorio_tipo_error_no_aplica_skip() throws Exception {
            // GIVEN: findUltimoErrorValidacion busca messageType='danger' (errores de validación ODG).
            // Un error de tipo 'error' genérico no se almacena en esa query, así que retorna vacío.
            Ocupacion ocupacion = crearOcupacion(5L, LocalDateTime.now());

            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            // El repositorio no encuentra errores de validación (messageType='danger') para este id
            when(trackingRepository.findUltimoErrorValidacion(5L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(5L))
                    .thenReturn(responseOk("success"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN: se procesa sin skip
            verify(ocupacionService).publicarOcupacionOrquestasDeGalicia(5L);
        }
    }

    // -------------------------------------------------------------------------
    // Tests de sincronizarAltas
    // -------------------------------------------------------------------------

    @Nested
    class SincronizarAltas {

        @Test
        void alta_exitosa_registra_tracking_OK() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(10L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(10L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(10L))
                    .thenReturn(responseOk("success"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            OdgSincronizacionTracking t = captor.getValue();
            assertThat(t.getResultado()).isEqualTo("OK");
            assertThat(t.getAccion()).isEqualTo("CREAR");
            assertThat(t.getOcupacionId()).isEqualTo(10L);
            assertThat(t.getArtistaId()).isEqualTo(10L);
        }

        @Test
        void alta_con_respuesta_error_registra_tracking_ERROR() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(11L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(11L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(11L))
                    .thenReturn(responseError("danger"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            OdgSincronizacionTracking t = captor.getValue();
            assertThat(t.getResultado()).isEqualTo("ERROR");
            assertThat(t.getMessageType()).isEqualTo("danger");
            assertThat(t.getMensaje()).isEqualTo("Error de validacion");
        }

        @Test
        void alta_con_respuesta_nula_registra_tracking_ERROR() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(12L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(12L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(12L))
                    .thenReturn(null);

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getResultado()).isEqualTo("ERROR");
            assertThat(captor.getValue().getMensaje()).isEqualTo("Respuesta nula de la operación ODG");
        }

        @Test
        void alta_que_lanza_excepcion_registra_tracking_ERROR() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(13L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(13L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(13L))
                    .thenThrow(new RuntimeException("Timeout de conexión"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN: no propaga la excepción y registra ERROR
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            OdgSincronizacionTracking t = captor.getValue();
            assertThat(t.getResultado()).isEqualTo("ERROR");
            assertThat(t.getMessageType()).isEqualTo("error");
            assertThat(t.getMensaje()).isEqualTo("Timeout de conexión");
        }

        @Test
        void lista_vacia_no_invoca_servicio_ni_tracking() {
            // GIVEN
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            verifyNoInteractions(ocupacionService);
            verify(trackingRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // Tests de sincronizarModificaciones
    // -------------------------------------------------------------------------

    @Nested
    class SincronizarModificaciones {

        @Test
        void modificacion_exitosa_registra_tracking_OK() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(20L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(20L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.actualizarOcupacionOrquestasDeGalicia(20L))
                    .thenReturn(responseOk("success"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            verify(ocupacionService).actualizarOcupacionOrquestasDeGalicia(20L);
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            OdgSincronizacionTracking t = captor.getValue();
            assertThat(t.getResultado()).isEqualTo("OK");
            assertThat(t.getAccion()).isEqualTo("ACTUALIZAR");
        }

        @Test
        void modificacion_con_error_previo_sin_cambios_registra_skip() throws Exception {
            // GIVEN
            LocalDateTime ayer = LocalDateTime.now().minusDays(1);
            LocalDateTime fechaError = LocalDateTime.now().minusHours(2);
            Ocupacion ocupacion = crearOcupacion(21L, ayer);

            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(21L))
                    .thenReturn(Optional.of(fechaError));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            verify(ocupacionService, never()).actualizarOcupacionOrquestasDeGalicia(any());
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getResultado()).isEqualTo("SKIPPED");
            assertThat(captor.getValue().getAccion()).isEqualTo("ACTUALIZAR");
        }

        @Test
        void modificacion_con_excepcion_registra_tracking_ERROR() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(22L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(22L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.actualizarOcupacionOrquestasDeGalicia(22L))
                    .thenThrow(new RuntimeException("Error inesperado"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getResultado()).isEqualTo("ERROR");
            assertThat(captor.getValue().getAccion()).isEqualTo("ACTUALIZAR");
        }
    }

    // -------------------------------------------------------------------------
    // Tests de sincronizarBorrados
    // -------------------------------------------------------------------------

    @Nested
    class SincronizarBorrados {

        @Test
        void borrado_exitoso_registra_tracking_OK() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(30L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of(ocupacion));
            when(trackingRepository.findUltimoErrorValidacion(30L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.eliminarOcupacionOrquestasDeGalicia(30L))
                    .thenReturn(responseOk("success"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            verify(ocupacionService).eliminarOcupacionOrquestasDeGalicia(30L);
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            OdgSincronizacionTracking t = captor.getValue();
            assertThat(t.getResultado()).isEqualTo("OK");
            assertThat(t.getAccion()).isEqualTo("ELIMINAR");
        }

        @Test
        void borrado_con_error_previo_sin_cambios_registra_skip() throws Exception {
            // GIVEN
            LocalDateTime ayer = LocalDateTime.now().minusDays(1);
            LocalDateTime fechaError = LocalDateTime.now().minusHours(1);
            Ocupacion ocupacion = crearOcupacion(31L, ayer);

            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of(ocupacion));
            when(trackingRepository.findUltimoErrorValidacion(31L))
                    .thenReturn(Optional.of(fechaError));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            verify(ocupacionService, never()).eliminarOcupacionOrquestasDeGalicia(any());
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getResultado()).isEqualTo("SKIPPED");
            assertThat(captor.getValue().getAccion()).isEqualTo("ELIMINAR");
        }

        @Test
        void borrado_con_respuesta_error_registra_tracking_ERROR() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(32L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of(ocupacion));
            when(trackingRepository.findUltimoErrorValidacion(32L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.eliminarOcupacionOrquestasDeGalicia(32L))
                    .thenReturn(responseError("error"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getResultado()).isEqualTo("ERROR");
            assertThat(captor.getValue().getMessageType()).isEqualTo("error");
        }
    }

    // -------------------------------------------------------------------------
    // Tests de contenido del tracking
    // -------------------------------------------------------------------------

    @Nested
    class ContenidoTracking {

        @Test
        void tracking_contiene_idEjecucion_y_fechaEjecucion_no_nulos() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(40L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(40L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(40L))
                    .thenReturn(responseOk("success"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            OdgSincronizacionTracking t = captor.getValue();
            assertThat(t.getIdEjecucion()).isNotNull().isNotEmpty();
            assertThat(t.getFechaEjecucion()).isNotNull();
            assertThat(t.getFechaEvento()).isNotNull();
        }

        @Test
        void mensaje_largo_se_trunca_a_1000_caracteres() throws Exception {
            // GIVEN
            String mensajeLargo = "x".repeat(1500);
            Ocupacion ocupacion = crearOcupacion(41L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(41L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(41L))
                    .thenReturn(DefaultResponseBody.builder()
                            .success(false)
                            .message(mensajeLargo)
                            .messageType("danger")
                            .build());

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getMensaje()).hasSize(1000);
        }

        @Test
        void mensaje_exactamente_1000_no_se_trunca() throws Exception {
            // GIVEN
            String mensajeExacto = "y".repeat(1000);
            Ocupacion ocupacion = crearOcupacion(42L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(42L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(42L))
                    .thenReturn(DefaultResponseBody.builder()
                            .success(false)
                            .message(mensajeExacto)
                            .messageType("danger")
                            .build());

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getMensaje()).hasSize(1000);
        }

        @Test
        void excepcion_con_mensaje_null_usa_mensaje_por_defecto() throws Exception {
            // GIVEN
            Ocupacion ocupacion = crearOcupacion(43L, LocalDateTime.now());
            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(ocupacion));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(43L))
                    .thenReturn(Optional.empty());
            // RuntimeException sin mensaje → getMessage() retorna null
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(43L))
                    .thenThrow(new RuntimeException((String) null));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN
            ArgumentCaptor<OdgSincronizacionTracking> captor =
                    ArgumentCaptor.forClass(OdgSincronizacionTracking.class);
            verify(trackingRepository).save(captor.capture());
            assertThat(captor.getValue().getMensaje()).isEqualTo("Error inesperado en sincronización ODG");
        }
    }

    // -------------------------------------------------------------------------
    // Tests de procesamiento de múltiples ocupaciones
    // -------------------------------------------------------------------------

    @Nested
    class ProcesamientoMultiple {

        @Test
        void procesa_multiples_ocupaciones_independientemente() throws Exception {
            // GIVEN: dos altas, una sin error previo y otra con error
            LocalDateTime ayer = LocalDateTime.now().minusDays(1);
            LocalDateTime fechaError = LocalDateTime.now().minusHours(1);

            Ocupacion oc1 = crearOcupacion(50L, LocalDateTime.now()); // se procesará
            Ocupacion oc2 = crearOcupacion(51L, ayer);                // se skipeará

            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(oc1, oc2));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(50L))
                    .thenReturn(Optional.empty());
            when(trackingRepository.findUltimoErrorValidacion(51L))
                    .thenReturn(Optional.of(fechaError));
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(50L))
                    .thenReturn(responseOk("success"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN: oc1 se procesa, oc2 se skipea → 2 saves al tracking
            verify(ocupacionService, times(1)).publicarOcupacionOrquestasDeGalicia(50L);
            verify(ocupacionService, never()).publicarOcupacionOrquestasDeGalicia(51L);
            verify(trackingRepository, times(2)).save(any(OdgSincronizacionTracking.class));
        }

        @Test
        void un_error_en_una_ocupacion_no_interrumpe_el_resto() throws Exception {
            // GIVEN
            Ocupacion oc1 = crearOcupacion(60L, LocalDateTime.now());
            Ocupacion oc2 = crearOcupacion(61L, LocalDateTime.now());

            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(oc1, oc2));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of());
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of());
            when(trackingRepository.findUltimoErrorValidacion(60L))
                    .thenReturn(Optional.empty());
            when(trackingRepository.findUltimoErrorValidacion(61L))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(60L))
                    .thenThrow(new RuntimeException("Fallo de red"));
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(61L))
                    .thenReturn(responseOk("success"));

            // WHEN: no debe lanzar excepción
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN: se guarda tracking de las dos ocupaciones
            verify(trackingRepository, times(2)).save(any(OdgSincronizacionTracking.class));
        }

        @Test
        void sincroniza_altas_modificaciones_y_borrados_en_el_mismo_ciclo() throws Exception {
            // GIVEN
            Ocupacion alta = crearOcupacion(70L, LocalDateTime.now());
            Ocupacion mod = crearOcupacion(71L, LocalDateTime.now());
            Ocupacion borrado = crearOcupacion(72L, LocalDateTime.now());

            when(ocupacionRepository.findPendientesPublicarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(alta));
            when(ocupacionRepository.findPendientesActualizarOdg(any(), any(), eq(ID_ESTADO_OCUPADO)))
                    .thenReturn(List.of(mod));
            when(ocupacionRepository.findPendientesEliminarOdg(any(), any(), eq(ID_ESTADO_ANULADO)))
                    .thenReturn(List.of(borrado));
            when(trackingRepository.findUltimoErrorValidacion(anyLong()))
                    .thenReturn(Optional.empty());
            when(ocupacionService.publicarOcupacionOrquestasDeGalicia(70L))
                    .thenReturn(responseOk("success"));
            when(ocupacionService.actualizarOcupacionOrquestasDeGalicia(71L))
                    .thenReturn(responseOk("success"));
            when(ocupacionService.eliminarOcupacionOrquestasDeGalicia(72L))
                    .thenReturn(responseOk("success"));

            // WHEN
            odgSincronizacionService.sincronizarDiarioAsync();

            // THEN: las tres operaciones se ejecutan y se guardan 3 trackings
            verify(ocupacionService).publicarOcupacionOrquestasDeGalicia(70L);
            verify(ocupacionService).actualizarOcupacionOrquestasDeGalicia(71L);
            verify(ocupacionService).eliminarOcupacionOrquestasDeGalicia(72L);
            verify(trackingRepository, times(3)).save(any(OdgSincronizacionTracking.class));
        }
    }
}
