package es.musicalia.gestmusica.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sincronizacion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sincronización", description = "API para la sincronización de datos")
public class SincronizacionController {

    private final SincronizacionService sincronizacionService;

    @PostMapping(value = "/sincronizar", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sincroniza los datos", description = "Ejecuta el proceso de sincronización de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sincronización completada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> sincronizar() {
        Map<String, Object> response = new HashMap<>();
        try {


            SincronizacionResult sincronizacionResult = sincronizacionService.sincronizarOcupacionesDesde(LocalDate.now());

            response.put("ocupacionesCreadas", sincronizacionResult.getCreadas());
            response.put("ocupacionesActualizadas", sincronizacionResult.getActualizadas());
            response.put("ocupacionesErrores", sincronizacionResult.getErrores());

            response.put("mensaje", "Sincronización completada");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en la sincronización", e);
            response.put("error", "Error en la sincronización: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping(value = "/sincronizar-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sincroniza todas las fechas desde 2020", description = "Ejecuta el proceso de sincronización de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sincronización completada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> sincronizarAll() {
        Map<String, Object> response = new HashMap<>();
        try {


            SincronizacionResult sincronizacionResult = sincronizacionService.sincronizarOcupacionesDesde(LocalDate.of(2024, 1, 1));
            response.put("ocupacionesCreadas", sincronizacionResult.getCreadas());
            response.put("ocupacionesActualizadas", sincronizacionResult.getActualizadas());
            response.put("ocupacionesErrores", sincronizacionResult.getErrores());

            response.put("mensaje", "Sincronización completada");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en la sincronización", e);
            response.put("error", "Error en la sincronización: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(response);
        }
    }



}