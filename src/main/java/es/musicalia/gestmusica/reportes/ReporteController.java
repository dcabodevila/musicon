package es.musicalia.gestmusica.reportes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteMensualAgenciaJob reporteMensualAgenciaJob;
    private final ReporteService reporteService;

    @PostMapping("/mensual-agencias/ejecutar")
    public ResponseEntity<Map<String, String>> ejecutarReporteMensual() {
        log.info("Ejecución manual del reporte mensual de agencias solicitada via API");

        try {
            reporteMensualAgenciaJob.enviarReportesMensuales();

            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Reporte mensual ejecutado exitosamente");
            response.put("estado", "COMPLETADO");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al ejecutar reporte mensual manualmente", e);

            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al ejecutar el reporte: " + e.getMessage());
            response.put("estado", "ERROR");

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/mensual-agencias/ejecutar/{idAgencia}")
    public ResponseEntity<Map<String, String>> ejecutarReporteMensualPorAgencia(@PathVariable Long idAgencia) {
        log.info("Ejecución manual del reporte mensual para agencia {} solicitada via API", idAgencia);

        try {
            reporteService.enviarReportePorIdAgencia(idAgencia);

            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Reporte mensual enviado exitosamente para agencia con ID: " + idAgencia);
            response.put("estado", "COMPLETADO");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al ejecutar reporte mensual para agencia {}", idAgencia, e);

            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Error al ejecutar el reporte: " + e.getMessage());
            response.put("estado", "ERROR");

            return ResponseEntity.internalServerError().body(response);
        }
    }
}