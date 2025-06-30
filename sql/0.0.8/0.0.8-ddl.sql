CREATE TABLE gestmusica.codigo_verificacion (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    codigo VARCHAR(4) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('REGISTRO', 'RECUPERACION_PASSWORD', 'CAMBIO_EMAIL'))
);

-- Crear índices para optimizar consultas
CREATE INDEX idx_codigo_verificacion_email_tipo
ON gestmusica.codigo_verificacion(email, tipo);

CREATE INDEX idx_codigo_verificacion_email_tipo_activo
ON gestmusica.codigo_verificacion(email, tipo, activo)
WHERE activo = true AND usado = false;

CREATE INDEX idx_codigo_verificacion_expiracion
ON gestmusica.codigo_verificacion(fecha_expiracion);

CREATE INDEX idx_codigo_verificacion_codigo_email
ON gestmusica.codigo_verificacion(codigo, email);

-- Crear índice compuesto para consultas de verificación
CREATE INDEX idx_codigo_verificacion_consulta_principal
ON gestmusica.codigo_verificacion(email, codigo, tipo, activo, usado, fecha_expiracion);

-- Agregar comentarios a la tabla y columnas
COMMENT ON TABLE gestmusica.codigo_verificacion IS 'Tabla para almacenar códigos de verificación de email';
COMMENT ON COLUMN gestmusica.codigo_verificacion.id IS 'Identificador único autoincremental';
COMMENT ON COLUMN gestmusica.codigo_verificacion.email IS 'Email del usuario al que se envía el código';
COMMENT ON COLUMN gestmusica.codigo_verificacion.codigo IS 'Código de verificación de 4 dígitos';
COMMENT ON COLUMN gestmusica.codigo_verificacion.fecha_creacion IS 'Fecha y hora de creación del código';
COMMENT ON COLUMN gestmusica.codigo_verificacion.fecha_expiracion IS 'Fecha y hora de expiración del código';
COMMENT ON COLUMN gestmusica.codigo_verificacion.usado IS 'Indica si el código ya fue utilizado';
COMMENT ON COLUMN gestmusica.codigo_verificacion.activo IS 'Indica si el código está activo';
COMMENT ON COLUMN gestmusica.codigo_verificacion.tipo IS 'Tipo de verificación: REGISTRO, RECUPERACION_PASSWORD, CAMBIO_EMAIL';

CREATE UNIQUE INDEX uk_usuario_email
ON gestmusica.usuario (email);

