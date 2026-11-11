-- ==============================================================================
-- UNIVERSIDAD EL BOSQUE - BASES DE DATOS 2
-- SISTEMA DE GESTIÓN DE RESERVAS DE ESPACIOS PARA EVENTOS
-- SCRIPT 03: SEGURIDAD, ROLES Y ASIGNACIÓN DE PRIVILEGIOS
-- ==============================================================================
-- Contenido:
--   1. Creación idempotente de roles del sistema (app_reservas, reportes_reservas)
--   2. Revocación de privilegios públicos por defecto (Principio de Menor Privilegio)
--   3. Concesión dinámica de conexión y uso de esquema
--   4. Privilegios DML granulares para la aplicación
--   5. Privilegios de solo lectura para el rol de auditoría y reportería
--   6. Privilegios por defecto para futuros objetos
-- ==============================================================================

-- ==============================================================================
-- 1. CREACIÓN IDEMPOTENTE DE ROLES
-- ==============================================================================
DO $$
BEGIN
    -- Rol operativo para la aplicación backend (Spring Boot / JPA / JDBC)
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_reservas') THEN
        CREATE ROLE app_reservas WITH LOGIN PASSWORD 'app123';
    END IF;

    -- Rol de solo lectura para analítica gerencial y consultas de auditoría
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'reportes_reservas') THEN
        CREATE ROLE reportes_reservas WITH LOGIN PASSWORD 'rep123';
    END IF;
END
$$;

-- ==============================================================================
-- 2. REVOCACIÓN DE PRIVILEGIOS PÚBLICOS POR DEFECTO
-- ==============================================================================
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- Revocación y concesión dinámica sobre la base de datos actual (portable)
DO $$
BEGIN
    EXECUTE format('REVOKE ALL ON DATABASE %I FROM PUBLIC', current_database());
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO app_reservas, reportes_reservas', current_database());
END
$$;

-- ==============================================================================
-- 3. PERMISOS PARA EL ROL OPERATIVO (app_reservas)
-- ==============================================================================
GRANT USAGE ON SCHEMA public TO app_reservas;

-- Permisos DML sobre todas las tablas y vistas del esquema public
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_reservas;

-- Permisos de secuencia para permitir generación de llaves subrogadas (SERIAL)
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO app_reservas;

-- Permisos para ejecución de procedimientos y funciones almacenadas
GRANT EXECUTE ON ALL ROUTINES IN SCHEMA public TO app_reservas;

-- Asignación de privilegios por defecto para nuevos objetos creados a futuro
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_reservas;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO app_reservas;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT EXECUTE ON ROUTINES TO app_reservas;

-- ==============================================================================
-- 4. PERMISOS PARA EL ROL DE REPORTERÍA (reportes_reservas)
-- ==============================================================================
GRANT USAGE ON SCHEMA public TO reportes_reservas;

-- Acceso de solo lectura a tablas y vistas
GRANT SELECT ON ALL TABLES IN SCHEMA public TO reportes_reservas;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO reportes_reservas;

-- Permiso para ejecutar funciones analíticas (ej. fn_estado_pago)
GRANT EXECUTE ON ALL ROUTINES IN SCHEMA public TO reportes_reservas;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT EXECUTE ON ROUTINES TO reportes_reservas;

