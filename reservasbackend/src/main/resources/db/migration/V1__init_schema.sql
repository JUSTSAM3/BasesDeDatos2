-- ==============================================================================
-- Proyecto: Sistema de Información Web para Gestión de Reservas
-- Descripción: Script DDL migrado y optimizado desde Oracle
-- ==============================================================================


CREATE TABLE clientes ( 
    id_cliente INTEGER GENERATED ALWAYS AS IDENTITY, 
    tipo_cliente VARCHAR(20) NOT NULL, 
    identificacion VARCHAR(30) NOT NULL, 
    nombre_razon_social VARCHAR(150) NOT NULL, 
    email VARCHAR(100) NOT NULL, 
    telefono VARCHAR(20),
    CONSTRAINT pk_clientes PRIMARY KEY (id_cliente),
    CONSTRAINT uq_clientes_identificacion UNIQUE (identificacion)
);

CREATE TABLE empleados ( 
    id_empleado INTEGER GENERATED ALWAYS AS IDENTITY, 
    nombre VARCHAR(100) NOT NULL, 
    cargo VARCHAR(50) NOT NULL, 
    email VARCHAR(100) NOT NULL,
    CONSTRAINT pk_empleados PRIMARY KEY (id_empleado)
);

CREATE TABLE sucursales ( 
    id_sucursal INTEGER GENERATED ALWAYS AS IDENTITY, 
    nombre VARCHAR(100) NOT NULL, 
    direccion VARCHAR(150) NOT NULL, 
    ciudad VARCHAR(80) NOT NULL, 
    telefono VARCHAR(20),
    CONSTRAINT pk_sucursales PRIMARY KEY (id_sucursal)
);

CREATE TABLE servicios_adicionales ( 
    id_servicio INTEGER GENERATED ALWAYS AS IDENTITY, 
    nombre VARCHAR(100) NOT NULL, 
    descripcion TEXT, 
    precio_unitario NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_servicios_adicionales PRIMARY KEY (id_servicio)
);



CREATE TABLE espacios ( 
    id_espacio INTEGER GENERATED ALWAYS AS IDENTITY, 
    id_sucursal INTEGER NOT NULL, 
    nombre VARCHAR(100) NOT NULL, 
    tipo VARCHAR(50) NOT NULL, 
    capacidad INTEGER NOT NULL, 
    tarifa_por_hora NUMERIC(10,2) NOT NULL, 
    estado VARCHAR(20),
    CONSTRAINT pk_espacios PRIMARY KEY (id_espacio)
);

CREATE TABLE reservas ( 
    id_reserva INTEGER GENERATED ALWAYS AS IDENTITY, 
    id_cliente INTEGER NOT NULL, 
    id_espacio INTEGER NOT NULL, 
    id_empleado INTEGER NOT NULL, 
    fecha_reserva TIMESTAMP NOT NULL, 
    fecha_inicio TIMESTAMP NOT NULL, 
    fecha_fin TIMESTAMP NOT NULL, 
    estado VARCHAR(20), 
    costo_total NUMERIC(12,2),
    CONSTRAINT pk_reservas PRIMARY KEY (id_reserva),
    CONSTRAINT chk_reserva_fechas CHECK (fecha_fin > fecha_inicio)
);

CREATE TABLE reserva_servicios ( 
    id_reserva_servicio INTEGER GENERATED ALWAYS AS IDENTITY, 
    id_reserva INTEGER NOT NULL, 
    id_servicio INTEGER NOT NULL, 
    cantidad INTEGER NOT NULL, 
    subtotal NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_reserva_servicios PRIMARY KEY (id_reserva_servicio),
    CONSTRAINT chk_reserva_serv_cantidad CHECK (cantidad > 0)
);

CREATE TABLE facturas ( 
    id_factura INTEGER GENERATED ALWAYS AS IDENTITY, 
    id_reserva INTEGER NOT NULL, 
    numero_factura VARCHAR(50) NOT NULL, 
    fecha_emision TIMESTAMP NOT NULL, 
    total NUMERIC(12,2) NOT NULL,
    CONSTRAINT pk_facturas PRIMARY KEY (id_factura),
    CONSTRAINT uq_facturas_numero UNIQUE (numero_factura)
);

CREATE TABLE pagos ( 
    id_pago INTEGER GENERATED ALWAYS AS IDENTITY, 
    id_factura INTEGER NOT NULL, 
    tipo_pago VARCHAR(20) NOT NULL, 
    monto NUMERIC(12,2) NOT NULL, 
    fecha_pago TIMESTAMP NOT NULL, 
    metodo_pago VARCHAR(50) NOT NULL,
    CONSTRAINT pk_pagos PRIMARY KEY (id_pago)
);

ALTER TABLE espacios 
    ADD CONSTRAINT fk_espacios_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales (id_sucursal);

ALTER TABLE reservas 
    ADD CONSTRAINT fk_reservas_cliente FOREIGN KEY (id_cliente) REFERENCES clientes (id_cliente),
    ADD CONSTRAINT fk_reservas_empleado FOREIGN KEY (id_empleado) REFERENCES empleados (id_empleado),
    ADD CONSTRAINT fk_reservas_espacio FOREIGN KEY (id_espacio) REFERENCES espacios (id_espacio);

ALTER TABLE reserva_servicios 
    ADD CONSTRAINT fk_reserva_serv_reserva FOREIGN KEY (id_reserva) REFERENCES reservas (id_reserva),
    ADD CONSTRAINT fk_reserva_serv_servicio FOREIGN KEY (id_servicio) REFERENCES servicios_adicionales (id_servicio);

ALTER TABLE facturas 
    ADD CONSTRAINT fk_facturas_reserva FOREIGN KEY (id_reserva) REFERENCES reservas (id_reserva);

ALTER TABLE pagos 
    ADD CONSTRAINT fk_pagos_factura FOREIGN KEY (id_factura) REFERENCES facturas (id_factura);