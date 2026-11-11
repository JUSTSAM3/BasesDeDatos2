-- ==============================================================================
-- UNIVERSIDAD EL BOSQUE - BASES DE DATOS 2
-- SISTEMA DE GESTIÓN DE RESERVAS DE ESPACIOS PARA EVENTOS
-- SCRIPT 01: DEFINICIÓN DE ESTRUCTURA DE DATOS (DDL)
-- ==============================================================================
-- Contenido:
--   1. Extensión btree_gist para exclusión temporal
--   2. Tablas base (catálogos y entidades maestras)
--   3. Tablas transaccionales y operativas
--   4. Llaves foráneas con integridad referencial
--   5. Restricciones de dominio (CHECK, UNIQUE, NOT NULL)
--   6. Índices para optimización de consultas
--   7. Restricción de exclusión GiST contra solapamiento de horarios
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 0. EXTENSIONES DEL SISTEMA
-- ------------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ==============================================================================
-- 1. TABLAS BASE Y CATÁLOGOS
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- Tabla: SUCURSALES (Sedes físicas del negocio)
-- ------------------------------------------------------------------------------
CREATE TABLE sucursales (
    id_sucursal             SERIAL,
    nombre                  VARCHAR(100) NOT NULL,
    direccion               VARCHAR(150),
    ciudad                  VARCHAR(80),
    telefono                VARCHAR(20),
    CONSTRAINT pk_sucursales PRIMARY KEY (id_sucursal)
);

-- ------------------------------------------------------------------------------
-- Tabla: TIPOS_ESPACIO (Catálogo normalizado de tipos de salón / recinto)
-- ------------------------------------------------------------------------------
CREATE TABLE tipos_espacio (
    id_tipo_espacio         SERIAL,
    nombre                  VARCHAR(100) NOT NULL,
    descripcion             TEXT,
    CONSTRAINT pk_tipos_espacio PRIMARY KEY (id_tipo_espacio)
);

-- ------------------------------------------------------------------------------
-- Tabla: ESPACIOS (Salones o recintos físicos reservables)
-- ------------------------------------------------------------------------------
CREATE TABLE espacios (
    id_espacio              SERIAL,
    id_sucursal             INTEGER NOT NULL,
    id_tipo_espacio         INTEGER DEFAULT 1 NOT NULL,
    nombre                  VARCHAR(100) NOT NULL,
    capacidad               INTEGER DEFAULT 0 NOT NULL,
    tarifa_por_hora         NUMERIC(10,2) DEFAULT 0.00 NOT NULL,
    estado                  VARCHAR(20) DEFAULT 'disponible',
    CONSTRAINT pk_espacios PRIMARY KEY (id_espacio),
    CONSTRAINT chk_espacios_capacidad CHECK (capacidad >= 0),
    CONSTRAINT chk_espacios_tarifa CHECK (tarifa_por_hora >= 0),
    CONSTRAINT chk_espacios_estado CHECK (estado IN ('disponible', 'mantenimiento', 'inactivo'))
);

-- ------------------------------------------------------------------------------
-- Tabla: CLIENTES (Personas naturales o jurídicas solicitantes)
-- ------------------------------------------------------------------------------
CREATE TABLE clientes (
    id_cliente              SERIAL,
    tipo_cliente            VARCHAR(20) DEFAULT 'natural' NOT NULL,
    identificacion          VARCHAR(30) NOT NULL,
    nombre_razon_social     VARCHAR(150) NOT NULL,
    email                   VARCHAR(100),
    telefono                VARCHAR(20),
    CONSTRAINT pk_clientes PRIMARY KEY (id_cliente),
    CONSTRAINT uq_clientes_identificacion UNIQUE (identificacion),
    CONSTRAINT chk_clientes_tipo CHECK (tipo_cliente IN ('natural', 'juridico'))
);

-- ------------------------------------------------------------------------------
-- Tabla: EMPLEADOS (Personal coordinador y logístico)
-- ------------------------------------------------------------------------------
CREATE TABLE empleados (
    id_empleado             SERIAL,
    nombre                  VARCHAR(100) NOT NULL,
    cargo                   VARCHAR(50),
    email                   VARCHAR(100),
    CONSTRAINT pk_empleados PRIMARY KEY (id_empleado)
);

-- ------------------------------------------------------------------------------
-- Tabla: SERVICIOS_ADICIONALES (Catálogo de complementos ofrecidos)
-- ------------------------------------------------------------------------------
CREATE TABLE servicios_adicionales (
    id_servicio             SERIAL,
    nombre                  VARCHAR(100) NOT NULL,
    descripcion             TEXT,
    precio_unitario         NUMERIC(10,2) DEFAULT 0.00 NOT NULL,
    CONSTRAINT pk_servicios_adicionales PRIMARY KEY (id_servicio),
    CONSTRAINT chk_servicios_precio CHECK (precio_unitario >= 0)
);

-- ==============================================================================
-- 2. TABLAS TRANSACCIONALES
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- Tabla: RESERVAS (Núcleo del negocio)
-- ------------------------------------------------------------------------------
CREATE TABLE reservas (
    id_reserva              SERIAL,
    id_cliente              INTEGER NOT NULL,
    id_espacio              INTEGER NOT NULL,
    id_empleado             INTEGER,
    fecha_reserva           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio            TIMESTAMP NOT NULL,
    fecha_fin               TIMESTAMP NOT NULL,
    estado                  VARCHAR(20) DEFAULT 'pendiente',
    costo_total             NUMERIC(12,2) DEFAULT 0.00,
    CONSTRAINT pk_reservas PRIMARY KEY (id_reserva),
    CONSTRAINT chk_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT chk_reservas_estado CHECK (estado IN ('pendiente', 'confirmada', 'cancelada', 'finalizada')),
    CONSTRAINT chk_reservas_costo CHECK (costo_total >= 0)
);

-- ------------------------------------------------------------------------------
-- Tabla: RESERVA_SERVICIOS (Detalle N:M entre reservas y servicios contratados)
-- ------------------------------------------------------------------------------
CREATE TABLE reserva_servicios (
    id_reserva_servicio     SERIAL,
    id_reserva              INTEGER NOT NULL,
    id_servicio             INTEGER NOT NULL,
    cantidad                INTEGER DEFAULT 1 NOT NULL,
    precio_unitario         NUMERIC(10,2) DEFAULT 0.00 NOT NULL,
    subtotal                NUMERIC(10,2) DEFAULT 0.00 NOT NULL,
    CONSTRAINT pk_reserva_servicios PRIMARY KEY (id_reserva_servicio),
    CONSTRAINT chk_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_reserva_serv_subtotal CHECK (subtotal >= 0),
    CONSTRAINT uq_reserva_servicios UNIQUE (id_reserva, id_servicio)
);

-- ------------------------------------------------------------------------------
-- Tabla: FACTURAS (Comprobante fiscal con relación 1:1 estricta hacia Reservas)
-- ------------------------------------------------------------------------------
CREATE TABLE facturas (
    id_factura              SERIAL,
    id_reserva              INTEGER NOT NULL,
    numero_factura          VARCHAR(50) NOT NULL,
    fecha_emision           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total                   NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    CONSTRAINT pk_facturas PRIMARY KEY (id_factura),
    CONSTRAINT uq_facturas_numero UNIQUE (numero_factura),
    CONSTRAINT unq_facturas_id_reserva UNIQUE (id_reserva),
    CONSTRAINT chk_facturas_total CHECK (total >= 0)
);

-- ------------------------------------------------------------------------------
-- Tabla: PAGOS (Abonos financieros aplicados a una factura, relación 1:N)
-- ------------------------------------------------------------------------------
CREATE TABLE pagos (
    id_pago                 SERIAL,
    id_factura              INTEGER NOT NULL,
    tipo_pago               VARCHAR(20) DEFAULT 'anticipo',
    monto                   NUMERIC(12,2) DEFAULT 0.00 NOT NULL,
    fecha_pago              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metodo_pago             VARCHAR(50),
    CONSTRAINT pk_pagos PRIMARY KEY (id_pago),
    CONSTRAINT chk_monto CHECK (monto > 0),
    CONSTRAINT chk_pagos_tipo CHECK (tipo_pago IN ('anticipo', 'saldo', 'total', 'reembolso'))
);

-- ==============================================================================
-- 3. LLAVES FORÁNEAS (INTEGRIDAD REFERENCIAL)
-- ==============================================================================

-- Espacios -> Sucursales (1:N) & Tipos_Espacio (1:N)
ALTER TABLE espacios 
    ADD CONSTRAINT fk_espacios_sucursal 
        FOREIGN KEY (id_sucursal) REFERENCES sucursales (id_sucursal) 
        ON DELETE CASCADE;

ALTER TABLE espacios 
    ADD CONSTRAINT fk_espacios_tipo 
        FOREIGN KEY (id_tipo_espacio) REFERENCES tipos_espacio (id_tipo_espacio);

-- Reservas -> Clientes (1:N), Espacios (1:N), Empleados (0..1:N)
ALTER TABLE reservas 
    ADD CONSTRAINT fk_reservas_cliente 
        FOREIGN KEY (id_cliente) REFERENCES clientes (id_cliente);

ALTER TABLE reservas 
    ADD CONSTRAINT fk_reservas_espacio 
        FOREIGN KEY (id_espacio) REFERENCES espacios (id_espacio);

ALTER TABLE reservas 
    ADD CONSTRAINT fk_reservas_empleado 
        FOREIGN KEY (id_empleado) REFERENCES empleados (id_empleado);

-- Reserva_Servicios -> Reservas (1:N) & Servicios_Adicionales (1:N)
ALTER TABLE reserva_servicios 
    ADD CONSTRAINT fk_reserva_servicios_reserva 
        FOREIGN KEY (id_reserva) REFERENCES reservas (id_reserva) 
        ON DELETE CASCADE;

ALTER TABLE reserva_servicios 
    ADD CONSTRAINT fk_reserva_servicios_servicio 
        FOREIGN KEY (id_servicio) REFERENCES servicios_adicionales (id_servicio);

-- Facturas -> Reservas (1:1 gracias a la restricción UNIQUE en id_reserva)
ALTER TABLE facturas 
    ADD CONSTRAINT fk_facturas_reserva 
        FOREIGN KEY (id_reserva) REFERENCES reservas (id_reserva);

-- Pagos -> Facturas (1:N)
ALTER TABLE pagos 
    ADD CONSTRAINT fk_pagos_factura 
        FOREIGN KEY (id_factura) REFERENCES facturas (id_factura);

-- ==============================================================================
-- 4. ÍNDICES DE RENDIMIENTO Y OPTIMIZACIÓN
-- ==============================================================================

-- Índices sobre llaves foráneas y predicados frecuentes
CREATE INDEX idx_reservas_cliente ON reservas(id_cliente);
CREATE INDEX idx_reservas_empleado ON reservas(id_empleado);
CREATE INDEX idx_reservas_fechas ON reservas(fecha_inicio, fecha_fin);
CREATE INDEX ix_reservas_rango_fechas ON reservas(id_espacio, fecha_inicio, fecha_fin);
CREATE INDEX idx_espacios_sucursal ON espacios(id_sucursal);
CREATE INDEX idx_espacios_tipo ON espacios(id_tipo_espacio);
CREATE INDEX idx_reserva_servicios_servicio ON reserva_servicios(id_servicio);
CREATE INDEX idx_pagos_factura ON pagos(id_factura);
CREATE INDEX ix_facturas_id_reserva ON facturas(id_reserva);
CREATE INDEX ix_clientes_identificacion ON clientes(identificacion);

-- ==============================================================================
-- 5. CONTROL DE CONCURRENCIA: EXCLUSIÓN DE SOLAPAMIENTO (GiST)
-- ==============================================================================
ALTER TABLE reservas
    DROP CONSTRAINT IF EXISTS no_solapamiento,
    ADD CONSTRAINT no_solapamiento
    EXCLUDE USING gist (
        id_espacio WITH =,
        tsrange(fecha_inicio, fecha_fin) WITH &&
    )
    WHERE (estado IN ('pendiente', 'confirmada'));

