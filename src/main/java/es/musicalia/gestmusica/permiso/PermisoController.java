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


    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @GetMapping("/hasPermission")
    public ResponseEntity<Boolean> hasPermission(
            @RequestParam Long targetId,
            @RequestParam String targetType,
            @RequestParam String permission) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasPermission = permissionEvaluator.hasPermission(authentication, targetId, targetType, permission);

        return ResponseEntity.ok(hasPermission);
    }

}
