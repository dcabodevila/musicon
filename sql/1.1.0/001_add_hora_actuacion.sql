-- Añadir campo hora_actuacion a la tabla ocupacion
-- Versión 1.1.0 - Hora específica de actuación

ALTER TABLE gestmusica.ocupacion
ADD COLUMN IF NOT EXISTS hora_actuacion TIME NULL;

COMMENT ON COLUMN gestmusica.ocupacion.hora_actuacion IS 'Hora específica de la actuación. Si es NULL, se muestra "Horario por confirmar"';
