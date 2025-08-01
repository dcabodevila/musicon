package es.musicalia.gestmusica.config;

import es.musicalia.gestmusica.permiso.PermisoService;
import es.musicalia.gestmusica.permiso.TipoPermisoEnum;
import es.musicalia.gestmusica.rol.RolEnum;
import es.musicalia.gestmusica.usuario.UserService;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    private org.slf4j.Logger logger = LoggerFactory.getLogger(CustomPermissionEvaluator.class);


    private final PermisoService permisoService;
    private final UserService userService;


    public CustomPermissionEvaluator(PermisoService permisoService, UserService userService){
        this.permisoService = permisoService;
        this.userService = userService;
    }
    /**
     * Verifica permisos con parámetros:
     *   - targetDomainObject: e.g. la entidad o el ID de la entidad
     *   - permission: e.g. "read" o "write"
     */
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission
    ) {
        logger.warn("hasPermission con targetDomainObject");
        return false;
    }

    /**
     * Este método se usa para tipos de permisos más avanzados,
     * donde 'targetType' es una clase y no un ID. Para simplificar, retorna false.
     */
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission
    ) {

        if (this.userService.obtenerUsuarioAutenticado().orElseThrow().getRolGeneral() !=null && RolEnum.ROL_ADMINISTRADOR.getCodigo().equals(this.userService.obtenerUsuarioAutenticado().orElseThrow().getRolGeneral().getCodigo()) ){
            return true;
        }

        if (TipoPermisoEnum.GENERAL.getDescripcion().equalsIgnoreCase(targetType)){
            return this.permisoService.existePermisoGeneral(permission.toString());
        }
        else if (TipoPermisoEnum.AGENCIA.getDescripcion().equalsIgnoreCase(targetType)){
            final Long idAgencia = getIdTargetPermiso(targetId);
            if (idAgencia == null) return false;
            return this.permisoService.existePermisoUsuarioAgencia(idAgencia,permission.toString());
        }
        else if (TipoPermisoEnum.ARTISTA.getDescripcion().equalsIgnoreCase(targetType)){
            final Long idArtista = getIdTargetPermiso(targetId);
            if (idArtista == null) return false;
            return this.permisoService.existePermisoUsuarioArtista(idArtista,permission.toString());
        }


        return false;
    }

    private Long getIdTargetPermiso(Serializable targetId) {
        try {
            return Long.valueOf(targetId.toString());
        } catch (NumberFormatException e) {
            logger.error("NumberFormatException evaluando permiso de {}", targetId.toString());
            return null;
        }
    }
}
