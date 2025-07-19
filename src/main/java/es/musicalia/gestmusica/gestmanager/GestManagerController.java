package es.musicalia.gestmusica.gestmanager;

import es.musicalia.gestmusica.api.DatosGestmanagerDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gestmanager")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestmanager", description = "API de integración para Gestmanager")
public class GestManagerController {

    private final GestmanagerService gestmanagerService;


    @Operation(
            summary = "Recibe datos desde Gestmanager vía PHP",
            description = "Endpoint que recibe información enviada desde el script sincronizacion.php, normalmente procedente de la aplicación Gestmanager. Requiere datos en formato JSON."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Datos recibidos correctamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud mal formada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
    })
    @PostMapping("/publicar")
    public ResponseEntity<String> recibirDatos(@RequestBody DatosGestmanagerDTO datos) {
        log.info("📥 Recibido desde PHP: {}", datos);

        gestmanagerService.procesarDatosGestmanager(datos);

        // Confirmas recepción
        return ResponseEntity.ok("Datos recibidos correctamente");
    }

}
