//package es.musicalia.gestmusica.gestmanager;
//
//import es.musicalia.gestmusica.api.DatosGestmanagerDTO;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/gestmanager")
//@RequiredArgsConstructor
//@Slf4j
//@Tag(name = "Gestmanager", description = "API de integración para Gestmanager")
//public class GestManagerController {
//
//    private final GestmanagerService gestmanagerService;
//
//
//    @Operation(
//            summary = "Recibe datos desde Gestmanager vía PHP",
//            description = "Endpoint que recibe información enviada desde el script sincronizacion.php, normalmente procedente de la aplicación Gestmanager. Requiere datos en formato JSON."
//    )
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Datos recibidos correctamente"),
//            @ApiResponse(responseCode = "400", description = "Solicitud mal formada", content = @Content),
//            @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content)
//    })
//    @PostMapping("/publicar")
//    public ResponseEntity<String> recibirDatos(@RequestBody DatosGestmanagerDTO datos) {
//        log.info("📥 Recibido desde PHP: {}", datos);
//
//        gestmanagerService.procesarDatosGestmanager(datos);
//
//        // Confirmas recepción
//        return ResponseEntity.ok("Datos recibidos correctamente");
//    }
//
//    @PostMapping(value = "/sincronizacion.php", consumes = "application/x-www-form-urlencoded")
//    public ResponseEntity<String> redirigirPhpLegacy(
//            @RequestParam Map<String, String> params
//    ) {
//
//        log.info("Recibida sincronizacion: "+ params.toString());
//
//        try {
//            DatosGestmanagerDTO datos = new DatosGestmanagerDTO(
//                    params.get("id_artista"),
//                    params.get("accion"),
//                    params.get("fecha"),
//                    params.get("descripcion"),
//                    params.get("poblacion"),
//                    params.get("municipio"),
//                    params.get("provincia"),
//                    params.get("pais"),
//                    params.get("nombre_local"),
//                    params.get("estado"),
//                    params.get("indicadores")
//            );
//
//            gestmanagerService.procesarDatosGestmanager(datos);
//
//            return ResponseEntity.ok("CORRECTO: El evento ha sido a&ntilde;adido correctamente");
//        } catch (Exception e) {
//            log.error("Error procesando datos de Gestmanager", e);
//            return ResponseEntity.ok("ERROR:No existe ning&uacute;n evento con esas caracter&iacute;sticas");
//        }
//    }
//
//}
