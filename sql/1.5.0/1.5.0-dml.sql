-- =============================================================================-- v1.5.0 — Agregar permiso COMUNICACIONES para el módulo de comunicaciones masivas-- =============================================================================
-- Insertar el nuevo permiso si no existe (evita duplicados en re-ejecuciones)
INSERT INTO gestmusica.permiso (codigo, descripcion, tipo_permiso)
SELECT 'COMUNICACIONES', 'Acceso al módulo de comunicaciones masivas', 1
WHERE NOT EXISTS (
    SELECT 1 FROM gestmusica.permiso WHERE codigo = 'COMUNICACIONES'
);

-- Vincular el permiso al rol ADMIN si no existe ya la relación
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id)
SELECT 
    (SELECT id FROM gestmusica.rol WHERE codigo = 'ADMIN'),
    (SELECT id FROM gestmusica.permiso WHERE codigo = 'COMUNICACIONES')
WHERE NOT EXISTS (
    SELECT 1 
    FROM gestmusica.rol_permisos rp
    JOIN gestmusica.rol r ON rp.rol_id = r.id
    JOIN gestmusica.permiso p ON rp.permiso_id = p.id
    WHERE r.codigo = 'ADMIN' AND p.codigo = 'COMUNICACIONES'
);

-- Comentario para documentación
COMMENT ON COLUMN gestmusica.permiso.codigo IS 'Código único del permiso, debe coincidir con PermisoGeneralEnum';
