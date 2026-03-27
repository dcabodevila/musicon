package es.musicalia.gestmusica.reactivacion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reactivacion")
@RequiredArgsConstructor
public class ReactivacionController {

    private final ReactivacionEmailService reactivacionEmailService;

    @PostMapping("/enviar")
    public ResponseEntity<Map<String, Object>> enviarEmailsReactivacion() {
        log.info("Ejecución manual del job de reactivación solicitada vía API");

        try {
            int enviados = reactivacionEmailService.enviarEmailsReactivacion();

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Job de reactivación ejecutado correctamente");
            response.put("estado", "COMPLETADO");
            response.put("enviados", enviados);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error ejecutando job de reactivación manualmente", e);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Error al ejecutar reactivación: " + e.getMessage());
            response.put("estado", "ERROR");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
