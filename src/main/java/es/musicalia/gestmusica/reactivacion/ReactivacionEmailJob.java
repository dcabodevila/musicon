package es.musicalia.gestmusica.reactivacion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job semanal que envía emails de reactivación a representantes inactivos.
 * Se ejecuta los lunes a las 10:00.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactivacionEmailJob {

    private final ReactivacionEmailService reactivacionEmailService;

    // Lunes (1) a las 10:00 — configurable vía properties
    @Scheduled(cron = "${reactivacion.email.cron.expression:0 0 10 * * MON}")
    public void ejecutar() {
        log.info("Iniciando job de reactivación de usuarios inactivos");
        try {
            int enviados = reactivacionEmailService.enviarEmailsReactivacion();
            log.info("Job de reactivación finalizado. Total enviados: {}", enviados);
        } catch (Exception e) {
            log.error("Error durante la ejecución del job de reactivación", e);
        }
    }
}
