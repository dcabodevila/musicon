-- Renombrar columna hora_actuacion a hora_actuacion_desde
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'gestmusica'
          AND table_name = 'ocupacion'
          AND column_name = 'hora_actuacion'
    ) THEN
        ALTER TABLE gestmusica.ocupacion RENAME COLUMN hora_actuacion TO hora_actuacion_desde;
    END IF;
END $$;

-- Añadir columna hora_actuacion_hasta
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'gestmusica'
          AND table_name = 'ocupacion'
          AND column_name = 'hora_actuacion_hasta'
    ) THEN
        ALTER TABLE gestmusica.ocupacion ADD COLUMN hora_actuacion_hasta TIME;
    END IF;
END $$;
