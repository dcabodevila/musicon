package es.musicalia.gestmusica.orquestasdegalicia;

import es.musicalia.gestmusica.ocupacion.Ocupacion;
import es.musicalia.gestmusica.ocupacion.OcupacionEstadoEnum;
import es.musicalia.gestmusica.ocupacion.OcupacionRepository;
import es.musicalia.gestmusica.ocupacion.OcupacionService;
import es.musicalia.gestmusica.util.DefaultResponseBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdgSincronizacionService {

    private static final int MAX_LEN_MENSAJE = 1000;

    private final OcupacionRepository ocupacionRepository;
    private final OcupacionService ocupacionService;
    private final OdgSincronizacionTrackingRepository trackingRepository;

    public void sincronizarDiarioAsync() {
        final String idEjecucion = UUID.randomUUID().toString();
        final LocalDateTime fechaEjecucion = LocalDateTime.now();
        final LocalDateTime fechaDesde = LocalDate.now().atStartOfDay();
        final LocalDateTime fechaHasta = LocalDate.now().plusMonths(3).atTime(23, 59, 59);

        log.info("Iniciando sincronización ODG. Ejecución={}, desde={}, hasta={}", idEjecucion, fechaDesde, fechaHasta);

        sincronizarAltas(idEjecucion, fechaEjecucion, fechaDesde, fechaHasta, OcupacionEstadoEnum.OCUPADO.getId());
        sincronizarModificaciones(idEjecucion, fechaEjecucion, fechaDesde, fechaHasta, OcupacionEstadoEnum.OCUPADO.getId());
        sincronizarBorrados(idEjecucion, fechaEjecucion, fechaDesde, fechaHasta, OcupacionEstadoEnum.ANULADO.getId());

        log.info("Finalizada sincronización ODG. Ejecución={}", idEjecucion);
        CompletableFuture.completedFuture(null);
    }

    private void sincronizarAltas(String idEjecucion, LocalDateTime fechaEjecucion, LocalDateTime fechaDesde, LocalDateTime fechaHasta, Long idEstadoOcupado) {
        final List<Ocupacion> ocupaciones = ocupacionRepository.findPendientesPublicarOdg(fechaDesde, fechaHasta, idEstadoOcupado);
        for (Ocupacion ocupacion : ocupaciones) {
            ejecutarOperacionConTracking(idEjecucion, fechaEjecucion, ocupacion, "CREAR",
                    () -> ocupacionService.publicarOcupacionOrquestasDeGalicia(ocupacion.getId()));
        }
    }

    private void sincronizarModificaciones(String idEjecucion, LocalDateTime fechaEjecucion, LocalDateTime fechaDesde, LocalDateTime fechaHasta, Long idEstadoOcupado) {
        final List<Ocupacion> ocupaciones = ocupacionRepository.findPendientesActualizarOdg(fechaDesde, fechaHasta, idEstadoOcupado);
        for (Ocupacion ocupacion : ocupaciones) {
            ejecutarOperacionConTracking(idEjecucion, fechaEjecucion, ocupacion, "ACTUALIZAR",
                    () -> ocupacionService.actualizarOcupacionOrquestasDeGalicia(ocupacion.getId()));
        }
    }

    private void sincronizarBorrados(String idEjecucion, LocalDateTime fechaEjecucion, LocalDateTime fechaDesde, LocalDateTime fechaHasta, Long idEstadoAnulado) {
        final List<Ocupacion> ocupaciones = ocupacionRepository.findPendientesEliminarOdg(fechaDesde, fechaHasta, idEstadoAnulado);
        for (Ocupacion ocupacion : ocupaciones) {
            ejecutarOperacionConTracking(idEjecucion, fechaEjecucion, ocupacion, "ELIMINAR",
                    () -> ocupacionService.eliminarOcupacionOrquestasDeGalicia(ocupacion.getId()));
        }
    }

    private void ejecutarOperacionConTracking(String idEjecucion, LocalDateTime fechaEjecucion, Ocupacion ocupacion, String accion, OperacionOdg operacion) {
        DefaultResponseBody response;
        String resultado;
        String messageType;
        String mensaje;

        try {
            response = operacion.ejecutar();
            if (response == null) {
                resultado = "ERROR";
                messageType = "error";
                mensaje = "Respuesta nula de la operación ODG";
            } else {
                resultado = response.isSuccess() ? "OK" : "ERROR";
                messageType = response.getMessageType();
                mensaje = truncar(response.getMessage());
            }
        } catch (Exception e) {
            resultado = "ERROR";
            messageType = "error";
            mensaje = truncar(e.getMessage() != null ? e.getMessage() : "Error inesperado en sincronización ODG");
            log.error("Error en sincronización ODG acción {} ocupación {}", accion, ocupacion.getId(), e);
        }

        trackingRepository.save(OdgSincronizacionTracking.builder()
                .idEjecucion(idEjecucion)
                .fechaEjecucion(fechaEjecucion)
                .ocupacionId(ocupacion.getId())
                .artistaId(ocupacion.getArtista().getId())
                .fechaEvento(ocupacion.getFecha())
                .accion(accion)
                .resultado(resultado)
                .messageType(messageType)
                .mensaje(mensaje)
                .build());
    }

    private String truncar(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_LEN_MENSAJE ? value.substring(0, MAX_LEN_MENSAJE) : value;
    }

    @FunctionalInterface
    private interface OperacionOdg {
        DefaultResponseBody ejecutar() throws Exception;
    }
}
