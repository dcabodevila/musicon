
-- Añadir columna excluir_sincronizacion_odg a la tabla ocupacion
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'gestmusica'
          AND table_name = 'ocupacion'
          AND column_name = 'excluir_sincronizacion_odg'
    ) THEN
        ALTER TABLE gestmusica.ocupacion ADD COLUMN excluir_sincronizacion_odg BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;