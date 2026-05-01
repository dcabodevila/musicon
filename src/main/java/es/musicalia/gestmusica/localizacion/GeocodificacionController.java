package es.musicalia.gestmusica.localizacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@PreAuthorize("hasAuthority('ACCESO_PANEL_ADMIN')")
@Tag(name = "Localización", description = "API para operaciones de geocodificación de municipios")
public class GeocodificacionController {

    private final MunicipioGeocodificacionService geocodificacionService;

    public GeocodificacionController(MunicipioGeocodificacionService geocodificacionService) {
        this.geocodificacionService = geocodificacionService;
    }

    @Operation(
            summary = "Geocodifica municipios pendientes (todos)",
            description = "Consulta Nominatim (OpenStreetMap) para obtener latitud/longitud de los municipios que aún no tienen coordenadas. Respete el rate limit de 1 req/seg."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado del proceso de geocodificación", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado. Requiere rol de administrador.", content = @Content)
    })
    @PostMapping("/api/municipios/geocodificar")
    public ResponseEntity<Map<String, Object>> geocodificarPendientes(
            @Parameter(description = "Cantidad máxima de municipios a geocodificar en esta petición", example = "50")
            @RequestParam(defaultValue = "50") int limite) {

        MunicipioGeocodificacionService.GeocodificacionResultado resultado =
                geocodificacionService.geocodificarPendientes(limite);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "total", resultado.total(),
                "exitosos", resultado.exitosos(),
                "fallidos", resultado.fallidos(),
                "mensaje", String.format("Procesados %d municipios. Éxitos: %d, Fallos: %d",
                        resultado.total(), resultado.exitosos(), resultado.fallidos())
        ));
    }

    @Operation(
            summary = "Geocodifica municipios pendientes de una provincia",
            description = "Consulta Nominatim (OpenStreetMap) para obtener latitud/longitud de los municipios SIN COORDENADAS de una provincia específica."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado del proceso de geocodificación por provincia", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acceso denegado. Requiere rol de administrador.", content = @Content)
    })
    @PostMapping("/api/municipios/geocodificar/provincia/{idProvincia}")
    public ResponseEntity<Map<String, Object>> geocodificarPendientesPorProvincia(
            @Parameter(description = "ID de la provincia", example = "28")
            @PathVariable Long idProvincia,
            @Parameter(description = "Cantidad máxima de municipios a geocodificar", example = "50")
            @RequestParam(defaultValue = "50") int limite) {

        MunicipioGeocodificacionService.GeocodificacionResultado resultado =
                geocodificacionService.geocodificarPendientesPorProvincia(idProvincia, limite);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "idProvincia", idProvincia,
                "total", resultado.total(),
                "exitosos", resultado.exitosos(),
                "fallidos", resultado.fallidos(),
                "mensaje", String.format("Provincia %d: Procesados %d municipios. Éxitos: %d, Fallos: %d",
                        idProvincia, resultado.total(), resultado.exitosos(), resultado.fallidos())
        ));
    }
}
