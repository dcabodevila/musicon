package es.musicalia.gestmusica.orquestasdegalicia;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/odg")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orquestas de Galicia", description = "API para operaciones con ODG")
public class OdgController {

    private final OdgSincronizacionService odgSincronizacionService;

    @PostMapping(value = "/sincronizar", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ejecuta el job de sincronización de ODG", description = "Lanza manualmente la sincronización de Orquestas de Galicia")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sincronización ODG lanzada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> sincronizar() {
        Map<String, Object> response = new HashMap<>();
        try {
            odgSincronizacionService.sincronizarDiarioAsync();
            response.put("mensaje", "Sincronización ODG completada");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en la sincronización ODG", e);
            response.put("error", "Error en la sincronización ODG: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
