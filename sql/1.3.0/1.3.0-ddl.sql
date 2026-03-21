-- Migration 1.3.0: Add TipoUsuario field to Usuario table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'gestmusica'
          AND table_name = 'usuario'
          AND column_name = 'tipo_usuario'
    ) THEN
        ALTER TABLE gestmusica.usuario
            ADD COLUMN tipo_usuario VARCHAR(20) NULL;
    END IF;
END $$;
