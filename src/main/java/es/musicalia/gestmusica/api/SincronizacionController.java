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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sincronizacion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sincronización", description = "API para la sincronización de datos")
public class SincronizacionController {

    private final SincronizacionRepository sincronizacionRepository;

    @GetMapping("/health")
    @Operation(summary = "Verificar estado del servicio", description = "Endpoint para verificar que el servicio está funcionando")
    @ApiResponse(responseCode = "200", description = "Servicio funcionando correctamente")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping
    @Operation(summary = "Recibir datos de sincronización en formato JSON", 
               description = "Endpoint para recibir y procesar datos de sincronización en formato JSON")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos procesados correctamente",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Map<String, Object>> recibirSincronizacionJSON(
            @RequestBody @Parameter(description = "Datos de sincronización") SincronizacionRequest request) {

        log.info("recibirSincronizacion Json");

        Map<String, Object> response = new HashMap<>();

        try {
            Sincronizacion entidad = Sincronizacion.builder()
                    .idArtista(request.getIdArtista())
                    .descripcion(request.getDescripcion())
                    .fecha(request.getFecha())
                    .poblacion(request.getPoblacion())
                    .municipio(request.getMunicipio())
                    .provincia(request.getProvincia())
                    .pais(request.getPais())
                    .nombreLocal(request.getNombreLocal())
                    .accion(request.getAccion())
                    .estado(request.getEstado())
                    .indicadores(request.getIndicadores())
                    .fechaRecepcion(LocalDateTime.now())
                    .build();

            sincronizacionRepository.save(entidad);
            response.put("fechaRecepcion", entidad.getFechaRecepcion());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }

        response.put("status", "ok");
        response.put("message", "Datos guardados correctamente");

        return ResponseEntity.ok(response);
    }

//    @PostMapping(value = "/cadena-datos", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
//    @Operation(summary = "Recibir cadena de datos",
//            description = "Endpoint para recibir una cadena de datos en formato form-urlencoded")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Datos procesados correctamente"),
//            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
//    })
//    public ResponseEntity<String> recibirDatosCadDatos(
//            @RequestParam(name = "CadDatos") @Parameter(description = "Cadena de datos a procesar", example = "cadena_ejemplo") String CadDatos) {
//        try {
//            Sincronizacion entidad = Sincronizacion.builder().cadDatos(CadDatos).build();
//            sincronizacionRepository.save(entidad);
//
//            return ResponseEntity.ok("OK");
//
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Error: " + e.getMessage());
//        }
//    }
}