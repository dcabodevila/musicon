package es.musicalia.gestmusica.sincronizacion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SincronizacionJob {

    private final SincronizacionService sincronizacionService;

    @Scheduled(cron = "${sincronizacion.cron.expression}")
    public void ejecutarSincronizacion() {
        try {
            log.info("Iniciando sincronizacion programada");
            Instant inicio = Instant.now();
            
            SincronizacionResult resultado = sincronizacionService.sincronizarOcupacionesDesde(LocalDate.now(), LocalDateTime.now().minusHours(2));
            
            Duration tiempoEjecucion = Duration.between(inicio, Instant.now());
            log.info("Sincronizacion completada en {} segundos. Creadas: {}, Actualizadas: {}, Eliminadas: {}, Errores: {}",
                tiempoEjecucion.toSeconds(),
                resultado.getCreadas(), 
                resultado.getActualizadas(),
                    resultado.getEliminadas(),
                    resultado.getErrores());
        } catch (Exception e) {
            log.error("Error durante la sincronizacion programada", e);
        }
    }
}