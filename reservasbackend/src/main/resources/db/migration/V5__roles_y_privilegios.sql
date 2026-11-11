-- =====================================================================
-- Creación de Roles y Asignación de Privilegios
-- Script Flyway V5
-- =====================================================================

DO $$
BEGIN
    -- Crear o actualizar rol app_reservas inyectando la contraseña desde Flyway placeholders (.env)
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_reservas') THEN
        EXECUTE format('CREATE ROLE app_reservas WITH LOGIN PASSWORD %L', '${app_db_password}');
    ELSE
        EXECUTE format('ALTER ROLE app_reservas WITH LOGIN PASSWORD %L', '${app_db_password}');
    END IF;

    -- Crear o actualizar rol reportes_reservas inyectando la contraseña desde Flyway placeholders (.env)
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'reportes_reservas') THEN
        EXECUTE format('CREATE ROLE reportes_reservas WITH LOGIN PASSWORD %L', '${reportes_db_password}');
    ELSE
        EXECUTE format('ALTER ROLE reportes_reservas WITH LOGIN PASSWORD %L', '${reportes_db_password}');
    END IF;
END
$$;

-- Revocar privilegios excesivos por defecto
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- Revocar y conceder conexión dinámicamente sobre la base de datos actual (portable)
DO $$
BEGIN
    EXECUTE format('REVOKE ALL ON DATABASE %I FROM PUBLIC', current_database());
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO app_reservas, reportes_reservas', current_database());
END
$$;

-- Permisos de uso de esquema
GRANT USAGE ON SCHEMA public TO app_reservas, reportes_reservas;

-- Permisos DML para app_reservas en TODAS las tablas necesarias
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_reservas;

-- Permisos sobre secuencias para que los INSERT funcionen
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO app_reservas;

-- Permisos para ejecutar rutinas (procedimientos y funciones)
GRANT EXECUTE ON ALL ROUTINES IN SCHEMA public TO app_reservas;

-- Asegurar que futuras tablas y secuencias también reciban los permisos
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_reservas;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO app_reservas;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT EXECUTE ON ROUTINES TO app_reservas;

-- Privilegios de solo lectura para reportes_reservas
GRANT SELECT ON ALL TABLES IN SCHEMA public TO reportes_reservas;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO reportes_reservas;
