package es.musicalia.gestmusica.orquestasdegalicia;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "orquestas.galicia.sincronizacion.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OdgSincronizacionJob {

    private final OdgSincronizacionService odgSincronizacionService;

    @Scheduled(cron = "${orquestas.galicia.sincronizacion.cron.expression:0 0 20 * * *}")
    public void ejecutarSincronizacionDiaria() {
        log.info("Lanzando task diaria de sincronización ODG");
        odgSincronizacionService.sincronizarDiarioAsync();
    }
}
