-- =====================================================================
-- Este scrip es el encargado de crear las tablas necesarias en la base de datos al levantar la aplicacion
-- Script Inicial de Migracion Flyway V1
-- =====================================================================

CREATE TABLE IF NOT EXISTS sucursales (
    id_sucursal SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(150),
    ciudad VARCHAR(80),
    telefono VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS espacios (
    id_espacio SERIAL PRIMARY KEY,
    id_sucursal INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(50),
    capacidad INT NOT NULL DEFAULT 0,
    tarifa_por_hora DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(20) DEFAULT 'disponible',
    CONSTRAINT fk_espacios_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS clientes (
    id_cliente SERIAL PRIMARY KEY,
    tipo_cliente VARCHAR(20),
    identificacion VARCHAR(30) UNIQUE NOT NULL,
    nombre_razon_social VARCHAR(150) NOT NULL,
    email VARCHAR(100),
    telefono VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS empleados (
    id_empleado SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    cargo VARCHAR(50),
    email VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS reservas (
    id_reserva SERIAL PRIMARY KEY,
    id_cliente INT NOT NULL,
    id_espacio INT NOT NULL,
    id_empleado INT,
    fecha_reserva TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    estado VARCHAR(20) DEFAULT 'pendiente',
    costo_total DECIMAL(12,2) DEFAULT 0.00,
    CONSTRAINT fk_reservas_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
    CONSTRAINT fk_reservas_espacio FOREIGN KEY (id_espacio) REFERENCES espacios(id_espacio),
    CONSTRAINT fk_reservas_empleado FOREIGN KEY (id_empleado) REFERENCES empleados(id_empleado)
);

CREATE TABLE IF NOT EXISTS servicios_adicionales (
    id_servicio SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    precio_unitario DECIMAL(10,2) NOT NULL DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS reserva_servicios (
    id_reserva_servicio SERIAL PRIMARY KEY,
    id_reserva INT NOT NULL,
    id_servicio INT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_reserva_servicios_reserva FOREIGN KEY (id_reserva) REFERENCES reservas(id_reserva) ON DELETE CASCADE,
    CONSTRAINT fk_reserva_servicios_servicio FOREIGN KEY (id_servicio) REFERENCES servicios_adicionales(id_servicio)
);

CREATE TABLE IF NOT EXISTS facturas (
    id_factura SERIAL PRIMARY KEY,
    id_reserva INT NOT NULL,
    numero_factura VARCHAR(50) UNIQUE NOT NULL,
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_facturas_reserva FOREIGN KEY (id_reserva) REFERENCES reservas(id_reserva)
);

CREATE TABLE IF NOT EXISTS pagos (
    id_pago SERIAL PRIMARY KEY,
    id_factura INT NOT NULL,
    tipo_pago VARCHAR(20),
    monto DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metodo_pago VARCHAR(50),
    CONSTRAINT fk_pagos_factura FOREIGN KEY (id_factura) REFERENCES facturas(id_factura)
);

-- Indices para optimizacion de consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_reservas_cliente ON reservas(id_cliente);
CREATE INDEX IF NOT EXISTS idx_reservas_espacio ON reservas(id_espacio);
CREATE INDEX IF NOT EXISTS idx_reservas_fechas ON reservas(fecha_inicio, fecha_fin);
CREATE INDEX IF NOT EXISTS idx_espacios_sucursal ON espacios(id_sucursal);
