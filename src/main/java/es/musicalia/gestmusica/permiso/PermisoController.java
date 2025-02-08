package es.musicalia.gestmusica.permiso;


import es.musicalia.gestmusica.generic.CodigoNombreRecord;
import es.musicalia.gestmusica.localizacion.LocalizacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value="permisos")
public class PermisoController {



    private final PermissionEvaluator permissionEvaluator;
    private final PermisoService permisoService;

    public PermisoController(PermissionEvaluator permissionEvaluator, PermisoService permisoService) {
        this.permissionEvaluator = permissionEvaluator;
        this.permisoService = permisoService;
    }

    @GetMapping("/hasPermission")
    public ResponseEntity<Boolean> hasPermission(
            @RequestParam Long targetId,
            @RequestParam String targetType,
            @RequestParam String permission) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasPermission = permissionEvaluator.hasPermission(authentication, targetId, targetType, permission);

        return ResponseEntity.ok(hasPermission);
    }

    @GetMapping("/rol/{idRol}")
    public ResponseEntity<List<PermisoRecord>> obtenerPermisosByIdRol(@PathVariable Long idRol) {
        List<PermisoRecord> permisos = permisoService.obtenerPermisosByIdRol(idRol);
        if (permisos.isEmpty()) {
            return ResponseEntity.noContent().build(); // Devuelve 204 si no hay permisos
        }
        return ResponseEntity.ok(permisos);
    }


}
