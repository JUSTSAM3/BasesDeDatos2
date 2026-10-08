-- =====================================================================
-- Creación de Roles y Asignación de Privilegios
-- Script Flyway V5
-- =====================================================================

DO $$
BEGIN
    -- Crear rol app_reservas de forma idempotente
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_reservas') THEN
        CREATE ROLE app_reservas WITH LOGIN PASSWORD 'app123';
    END IF;

    -- Crear rol reportes_reservas de forma idempotente
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'reportes_reservas') THEN
        CREATE ROLE reportes_reservas WITH LOGIN PASSWORD 'rep123';
    END IF;
END
$$;

-- Revocar privilegios excesivos por defecto
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON DATABASE reservas_db FROM PUBLIC;

-- Permisos de conexión y uso de esquema
GRANT CONNECT ON DATABASE reservas_db TO app_reservas, reportes_reservas;
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
