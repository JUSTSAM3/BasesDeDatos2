-- =====================================================================
-- Este scrip es el encargado de crear las tablas necesarias en la base de datos al levantar la aplicacion
-- Script Inicial de Migracion Flyway V1
-- =====================================================================

CREATE TABLE IF NOT EXISTS sucursales (
    id_sucursal SERIAL CONSTRAINT pk_sucursales PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(150),
    ciudad VARCHAR(80),
    telefono VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS espacios (
    id_espacio SERIAL CONSTRAINT pk_espacios PRIMARY KEY,
    id_sucursal INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(50),
    capacidad INT NOT NULL DEFAULT 0,
    tarifa_por_hora DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(20) DEFAULT 'disponible',
    CONSTRAINT fk_espacios_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal) ON DELETE CASCADE,
    CONSTRAINT chk_espacios_capacidad CHECK (capacidad >= 0),
    CONSTRAINT chk_espacios_tarifa CHECK (tarifa_por_hora >= 0),
    CONSTRAINT chk_espacios_estado CHECK (estado IN ('disponible', 'mantenimiento', 'inactivo'))
);

CREATE TABLE IF NOT EXISTS clientes (
    id_cliente SERIAL CONSTRAINT pk_clientes PRIMARY KEY,
    tipo_cliente VARCHAR(20) NOT NULL DEFAULT 'natural',
    identificacion VARCHAR(30) NOT NULL CONSTRAINT uq_clientes_identificacion UNIQUE,
    nombre_razon_social VARCHAR(150) NOT NULL,
    email VARCHAR(100),
    telefono VARCHAR(20),
    CONSTRAINT chk_clientes_tipo CHECK (tipo_cliente IN ('natural', 'juridico'))
);

CREATE TABLE IF NOT EXISTS empleados (
    id_empleado SERIAL CONSTRAINT pk_empleados PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    cargo VARCHAR(50),
    email VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS reservas (
    id_reserva SERIAL CONSTRAINT pk_reservas PRIMARY KEY,
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
    CONSTRAINT fk_reservas_empleado FOREIGN KEY (id_empleado) REFERENCES empleados(id_empleado),
    CONSTRAINT chk_reservas_estado CHECK (estado IN ('pendiente', 'confirmada', 'cancelada', 'finalizada')),
    CONSTRAINT chk_reservas_costo CHECK (costo_total >= 0)
);

CREATE TABLE IF NOT EXISTS servicios_adicionales (
    id_servicio SERIAL CONSTRAINT pk_servicios_adicionales PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    precio_unitario DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT chk_servicios_precio CHECK (precio_unitario >= 0)
);

CREATE TABLE IF NOT EXISTS reserva_servicios (
    id_reserva_servicio SERIAL CONSTRAINT pk_reserva_servicios PRIMARY KEY,
    id_reserva INT NOT NULL,
    id_servicio INT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_reserva_servicios_reserva FOREIGN KEY (id_reserva) REFERENCES reservas(id_reserva) ON DELETE CASCADE,
    CONSTRAINT fk_reserva_servicios_servicio FOREIGN KEY (id_servicio) REFERENCES servicios_adicionales(id_servicio),
    CONSTRAINT chk_reserva_serv_subtotal CHECK (subtotal >= 0),
    CONSTRAINT uq_reserva_servicios UNIQUE (id_reserva, id_servicio)
);

CREATE TABLE IF NOT EXISTS facturas (
    id_factura SERIAL CONSTRAINT pk_facturas PRIMARY KEY,
    id_reserva INT NOT NULL,
    numero_factura VARCHAR(50) NOT NULL CONSTRAINT uq_facturas_numero UNIQUE,
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_facturas_reserva FOREIGN KEY (id_reserva) REFERENCES reservas(id_reserva),
    CONSTRAINT chk_facturas_total CHECK (total >= 0)
);

CREATE TABLE IF NOT EXISTS pagos (
    id_pago SERIAL CONSTRAINT pk_pagos PRIMARY KEY,
    id_factura INT NOT NULL,
    tipo_pago VARCHAR(20) DEFAULT 'anticipo',
    monto DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metodo_pago VARCHAR(50),
    CONSTRAINT fk_pagos_factura FOREIGN KEY (id_factura) REFERENCES facturas(id_factura),
    CONSTRAINT chk_pagos_tipo CHECK (tipo_pago IN ('anticipo', 'saldo', 'total', 'reembolso'))
);

-- Indices para optimizacion de consultas frecuentes
-- Nota: la busqueda por id_espacio la cubre el indice compuesto ix_reservas_rango_fechas (V3),
-- cuyo primer campo es id_espacio, por eso no se crea un indice simple adicional.
CREATE INDEX IF NOT EXISTS idx_reservas_cliente ON reservas(id_cliente);
CREATE INDEX IF NOT EXISTS idx_reservas_fechas ON reservas(fecha_inicio, fecha_fin);
CREATE INDEX IF NOT EXISTS idx_espacios_sucursal ON espacios(id_sucursal);

-- Indices sobre llaves foraneas (PostgreSQL no los crea automaticamente):
-- aceleran los JOIN de los reportes y evitan bloqueos/escaneos completos al borrar o actualizar el padre.
CREATE INDEX IF NOT EXISTS idx_reservas_empleado ON reservas(id_empleado);
CREATE INDEX IF NOT EXISTS idx_reserva_servicios_servicio ON reserva_servicios(id_servicio);
CREATE INDEX IF NOT EXISTS idx_pagos_factura ON pagos(id_factura);
