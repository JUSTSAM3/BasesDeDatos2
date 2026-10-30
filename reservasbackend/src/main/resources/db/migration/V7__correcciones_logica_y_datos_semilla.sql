-- =====================================================================
-- Script Migracion Flyway V7:
-- 1. Correccion de fn_validar_horario (permitir cierre exacto a las 22:00:00)
-- 2. Soporte seguro de limpieza de pruebas en fn_trg_evitar_borrado_facturas
-- 3. Mejoras de validacion y bloques EXCEPTION en SP_CREAR_RESERVA,
--    SP_REGISTRAR_PAGO y SP_CANCELAR_RESERVA
-- 4. Datos semilla (Seed Data) idempotentes para catalogos y operacion inicial
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. CORRECCION: fn_validar_horario (H-07)
-- Permite reservas entre las 08:00:00 y las 22:00:00 exactas del mismo dia
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_horario(vd_fecha_inicio TIMESTAMP, vd_fecha_fin TIMESTAMP)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    vt_hora_inicio TIME;
    vt_hora_fin TIME;
BEGIN
    IF vd_fecha_fin <= vd_fecha_inicio THEN
        RETURN FALSE;
    END IF;

    -- Validar que el evento inicie y termine el mismo dia dentro de la jornada
    IF vd_fecha_inicio::DATE <> vd_fecha_fin::DATE THEN
        RETURN FALSE;
    END IF;

    vt_hora_inicio := vd_fecha_inicio::TIME;
    vt_hora_fin := vd_fecha_fin::TIME;

    -- Ventana operativa: 08:00:00 a 22:00:00 (inclusive a las 22:00:00)
    IF vt_hora_inicio < TIME '08:00:00' OR vt_hora_fin > TIME '22:00:00' THEN
        RETURN FALSE;
    END IF;

    RETURN TRUE;
END;
$$;

-- ---------------------------------------------------------------------
-- 2. MEJORA: fn_trg_evitar_borrado_facturas (H-03)
-- Protege las facturas contra DELETE, permitiendo limpieza controlada
-- unicamente cuando la variable local de transaccion reservas.test_cleanup = 'on'
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_evitar_borrado_facturas()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF COALESCE(current_setting('reservas.test_cleanup', true), 'off') = 'on' THEN
        RETURN OLD;
    END IF;

    RAISE EXCEPTION 'e_borrado_facturas: Las facturas no pueden ser eliminadas del sistema. Utilice anulacion logica.';
    RETURN OLD;
END;
$$;

-- ---------------------------------------------------------------------
-- 3. MEJORA: SP_CREAR_RESERVA (H-09, H-10)
-- Valida existencia de cada servicio en JSONB, evita doble UPDATE
-- y agrega bloque EXCEPTION para manejo formal de errores en BD
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SP_CREAR_RESERVA(
    IN p_id_cliente INT,
    IN p_id_espacio INT,
    IN p_id_empleado INT,
    IN p_fecha_inicio TIMESTAMP,
    IN p_fecha_fin TIMESTAMP,
    IN p_servicios_json JSONB DEFAULT NULL,
    INOUT p_id_reserva INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    vd_horas NUMERIC;
    v_tarifa DECIMAL(10,2);
    vv_estado_espacio VARCHAR(20);
    v_costo_inicial DECIMAL(12,2);
    r_servicio RECORD;
    v_precio_unitario DECIMAL(10,2);
    v_subtotal DECIMAL(10,2);
BEGIN
    -- 1. Validar que el espacio exista y este disponible
    SELECT tarifa_por_hora, estado
    INTO v_tarifa, vv_estado_espacio
    FROM espacios
    WHERE id_espacio = p_id_espacio;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El espacio % no existe', p_id_espacio
            USING ERRCODE = '23503';
    END IF;

    IF COALESCE(vv_estado_espacio, 'disponible') <> 'disponible' THEN
        RAISE EXCEPTION 'e_espacio_no_disponible: El espacio % se encuentra en estado %', p_id_espacio, vv_estado_espacio;
    END IF;

    -- 2. Validar Disponibilidad de Horario
    IF NOT fn_validar_disponibilidad(p_id_espacio, p_fecha_inicio, p_fecha_fin) THEN
        RAISE EXCEPTION 'e_espacio_ocupado: El espacio % no tiene disponibilidad en las fechas dadas', p_id_espacio;
    END IF;

    -- 3. Validar Horario Operativo (08:00 - 22:00)
    IF NOT fn_validar_horario(p_fecha_inicio, p_fecha_fin) THEN
        RAISE EXCEPTION 'e_horario_invalido: La reserva debe estar dentro del horario operativo (08:00 - 22:00)';
    END IF;

    -- 4. Calcular tarifa base por horas
    vd_horas := EXTRACT(EPOCH FROM (p_fecha_fin - p_fecha_inicio)) / 3600.0;
    IF vd_horas < 1 THEN
        vd_horas := 1;
    END IF;
    v_costo_inicial := ROUND((vd_horas * COALESCE(v_tarifa, 0))::numeric, 2);

    -- 5. Insertar el registro maestro de la reserva
    INSERT INTO reservas (id_cliente, id_espacio, id_empleado, fecha_reserva, fecha_inicio, fecha_fin, estado, costo_total)
    VALUES (p_id_cliente, p_id_espacio, p_id_empleado, CURRENT_TIMESTAMP, p_fecha_inicio, p_fecha_fin, 'pendiente', v_costo_inicial)
    RETURNING id_reserva INTO p_id_reserva;

    -- 6. Procesar arreglo JSONB de servicios adicionales
    -- Nota: El trigger trg_recalcular_costo_servicios actualiza automaticamente reservas.costo_total
    IF p_servicios_json IS NOT NULL AND jsonb_typeof(p_servicios_json) = 'array' AND jsonb_array_length(p_servicios_json) > 0 THEN
        FOR r_servicio IN
            SELECT * FROM jsonb_to_recordset(p_servicios_json) AS x(id_servicio INT, cantidad INT)
        LOOP
            SELECT precio_unitario
            INTO v_precio_unitario
            FROM servicios_adicionales
            WHERE id_servicio = r_servicio.id_servicio;

            IF NOT FOUND THEN
                RAISE EXCEPTION 'e_servicio_no_existe: El servicio adicional % no existe', r_servicio.id_servicio
                    USING ERRCODE = '23503';
            END IF;

            v_subtotal := v_precio_unitario * r_servicio.cantidad;

            INSERT INTO reserva_servicios (id_reserva, id_servicio, cantidad, precio_unitario, subtotal)
            VALUES (p_id_reserva, r_servicio.id_servicio, r_servicio.cantidad, v_precio_unitario, v_subtotal);
        END LOOP;
    END IF;

EXCEPTION
    WHEN exclusion_violation THEN
        RAISE NOTICE '[SP_CREAR_RESERVA] Conflicto de concurrencia detectado para el espacio %', p_id_espacio;
        RAISE;
    WHEN foreign_key_violation THEN
        RAISE NOTICE '[SP_CREAR_RESERVA] Error de llave foranea al crear la reserva: %', SQLERRM;
        RAISE;
    WHEN OTHERS THEN
        RAISE NOTICE '[SP_CREAR_RESERVA] Rollback automatico de transaccion por error: % (SQLSTATE: %)', SQLERRM, SQLSTATE;
        RAISE;
END;
$$;

-- ---------------------------------------------------------------------
-- 4. MEJORA: SP_REGISTRAR_PAGO (H-08, H-10)
-- Impide pagar reservas canceladas, controla sobrepagos sobre el total
-- de la factura y agrega bloque EXCEPTION
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SP_REGISTRAR_PAGO(
    IN p_id_reserva INT,
    IN p_tipo_pago VARCHAR(20),
    IN p_monto DECIMAL(12,2),
    IN p_metodo_pago VARCHAR(50),
    INOUT p_id_pago INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_factura INT;
    vv_num_factura VARCHAR(50);
    v_costo_total DECIMAL(12,2);
    v_total_factura DECIMAL(12,2);
    vv_estado_reserva VARCHAR(20);
    v_pagado_previo DECIMAL(12,2);
    v_pagado_acumulado DECIMAL(12,2);
BEGIN
    IF p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto del pago debe ser mayor a 0';
    END IF;

    SELECT costo_total, estado
    INTO v_costo_total, vv_estado_reserva
    FROM reservas
    WHERE id_reserva = p_id_reserva;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La reserva % no existe', p_id_reserva;
    END IF;

    IF vv_estado_reserva = 'cancelada' THEN
        RAISE EXCEPTION 'e_reserva_cancelada: No se pueden registrar pagos sobre la reserva % porque esta cancelada', p_id_reserva;
    END IF;

    -- Obtener o generar la factura asociada (Relacion 1:1)
    SELECT id_factura, total
    INTO v_id_factura, v_total_factura
    FROM facturas
    WHERE id_reserva = p_id_reserva;

    IF NOT FOUND THEN
        vv_num_factura := 'FAC-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || LPAD(p_id_reserva::TEXT, 5, '0');
        v_total_factura := v_costo_total;

        INSERT INTO facturas (id_reserva, numero_factura, fecha_emision, total)
        VALUES (p_id_reserva, vv_num_factura, CURRENT_TIMESTAMP, v_total_factura)
        RETURNING id_factura INTO v_id_factura;
    END IF;

    SELECT COALESCE(SUM(monto), 0.00)
    INTO v_pagado_previo
    FROM pagos
    WHERE id_factura = v_id_factura;

    -- Validar que el pago no exceda el saldo pendiente de la factura
    IF COALESCE(p_tipo_pago, 'anticipo') <> 'reembolso' AND v_total_factura > 0 AND (v_pagado_previo + p_monto) > v_total_factura THEN
        RAISE EXCEPTION 'e_monto_excede_total: El pago de % excede el saldo pendiente de la factura (Total: %, Pagado: %)',
            p_monto, v_total_factura, v_pagado_previo;
    END IF;

    INSERT INTO pagos (id_factura, tipo_pago, monto, fecha_pago, metodo_pago)
    VALUES (v_id_factura, p_tipo_pago, p_monto, CURRENT_TIMESTAMP, p_metodo_pago)
    RETURNING id_pago INTO p_id_pago;

    v_pagado_acumulado := v_pagado_previo + p_monto;

    -- Confirmar reserva si se cubre al menos el 30% de anticipo
    IF v_pagado_acumulado >= (v_total_factura * 0.30) THEN
        UPDATE reservas
        SET estado = 'confirmada'
        WHERE id_reserva = p_id_reserva
          AND estado = 'pendiente';
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE '[SP_REGISTRAR_PAGO] Error al procesar pago para reserva %: % (SQLSTATE: %)', p_id_reserva, SQLERRM, SQLSTATE;
        RAISE;
END;
$$;

-- ---------------------------------------------------------------------
-- 5. MEJORA: SP_CANCELAR_RESERVA (H-10)
-- Agrega bloque EXCEPTION formal
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SP_CANCELAR_RESERVA(
    IN p_id_reserva INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    vd_inicio TIMESTAMP;
    vv_estado VARCHAR(20);
BEGIN
    SELECT fecha_inicio, estado
    INTO vd_inicio, vv_estado
    FROM reservas
    WHERE id_reserva = p_id_reserva;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Reserva % no encontrada', p_id_reserva;
    END IF;

    IF vd_inicio <= CURRENT_TIMESTAMP THEN
        RAISE EXCEPTION 'No es posible cancelar una reserva cuyo evento ya ha iniciado o finalizado';
    END IF;

    IF vv_estado = 'cancelada' THEN
        RAISE EXCEPTION 'La reserva % ya se encuentra cancelada', p_id_reserva;
    END IF;

    UPDATE reservas
    SET estado = 'cancelada'
    WHERE id_reserva = p_id_reserva;

EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE '[SP_CANCELAR_RESERVA] Error al cancelar reserva %: %', p_id_reserva, SQLERRM;
        RAISE;
END;
$$;

-- Asegurar permisos de ejecucion sobre las rutinas actualizadas
GRANT EXECUTE ON ALL ROUTINES IN SCHEMA public TO app_reservas;

-- ---------------------------------------------------------------------
-- 6. DATOS SEMILLA (SEED DATA IDEMPOTENTE) (H-11)
-- ---------------------------------------------------------------------

-- 6.1 Tipos de espacio adicionales
INSERT INTO tipos_espacio (nombre, descripcion)
SELECT 'Salón de Eventos Sociales', 'Salones elegantes para bodas, grados, cumpleaños y recepciones.'
WHERE NOT EXISTS (SELECT 1 FROM tipos_espacio WHERE nombre = 'Salón de Eventos Sociales');

INSERT INTO tipos_espacio (nombre, descripcion)
SELECT 'Auditorio y Conferencias', 'Espacios con acústica y proyección para congresos, seminarios y charlas.'
WHERE NOT EXISTS (SELECT 1 FROM tipos_espacio WHERE nombre = 'Auditorio y Conferencias');

INSERT INTO tipos_espacio (nombre, descripcion)
SELECT 'Terraza Lounge', 'Espacios al aire libre o semiabiertos ideales para cócteles y eventos empresariales.'
WHERE NOT EXISTS (SELECT 1 FROM tipos_espacio WHERE nombre = 'Terraza Lounge');

INSERT INTO tipos_espacio (nombre, descripcion)
SELECT 'Sala Ejecutiva VIP', 'Salas corporativas para juntas directivas, talleres y networking.'
WHERE NOT EXISTS (SELECT 1 FROM tipos_espacio WHERE nombre = 'Sala Ejecutiva VIP');

-- 6.2 Sucursales
INSERT INTO sucursales (nombre, direccion, ciudad, telefono)
SELECT 'Sede Usaquén Norte', 'Carrera 7 # 119-45', 'Bogotá', '6017428000'
WHERE NOT EXISTS (SELECT 1 FROM sucursales WHERE nombre = 'Sede Usaquén Norte');

INSERT INTO sucursales (nombre, direccion, ciudad, telefono)
SELECT 'Sede Parque de la 93', 'Calle 93A # 13-24', 'Bogotá', '6017428001'
WHERE NOT EXISTS (SELECT 1 FROM sucursales WHERE nombre = 'Sede Parque de la 93');

INSERT INTO sucursales (nombre, direccion, ciudad, telefono)
SELECT 'Sede Chapinero Cultural', 'Calle 67 # 8-32', 'Bogotá', '6017428002'
WHERE NOT EXISTS (SELECT 1 FROM sucursales WHERE nombre = 'Sede Chapinero Cultural');

-- 6.3 Espacios
INSERT INTO espacios (id_sucursal, id_tipo_espacio, nombre, capacidad, tarifa_por_hora, estado)
SELECT
    (SELECT id_sucursal FROM sucursales WHERE nombre = 'Sede Usaquén Norte' LIMIT 1),
    (SELECT id_tipo_espacio FROM tipos_espacio WHERE nombre = 'Salón de Eventos Sociales' LIMIT 1),
    'Salón Imperial Aura', 180, 350000.00, 'disponible'
WHERE NOT EXISTS (SELECT 1 FROM espacios WHERE nombre = 'Salón Imperial Aura');

INSERT INTO espacios (id_sucursal, id_tipo_espacio, nombre, capacidad, tarifa_por_hora, estado)
SELECT
    (SELECT id_sucursal FROM sucursales WHERE nombre = 'Sede Parque de la 93' LIMIT 1),
    (SELECT id_tipo_espacio FROM tipos_espacio WHERE nombre = 'Terraza Lounge' LIMIT 1),
    'Terraza Nómada 93', 100, 260000.00, 'disponible'
WHERE NOT EXISTS (SELECT 1 FROM espacios WHERE nombre = 'Terraza Nómada 93');

INSERT INTO espacios (id_sucursal, id_tipo_espacio, nombre, capacidad, tarifa_por_hora, estado)
SELECT
    (SELECT id_sucursal FROM sucursales WHERE nombre = 'Sede Chapinero Cultural' LIMIT 1),
    (SELECT id_tipo_espacio FROM tipos_espacio WHERE nombre = 'Auditorio y Conferencias' LIMIT 1),
    'Auditorio Magno El Bosque', 250, 420000.00, 'disponible'
WHERE NOT EXISTS (SELECT 1 FROM espacios WHERE nombre = 'Auditorio Magno El Bosque');

INSERT INTO espacios (id_sucursal, id_tipo_espacio, nombre, capacidad, tarifa_por_hora, estado)
SELECT
    (SELECT id_sucursal FROM sucursales WHERE nombre = 'Sede Parque de la 93' LIMIT 1),
    (SELECT id_tipo_espacio FROM tipos_espacio WHERE nombre = 'Sala Ejecutiva VIP' LIMIT 1),
    'Studio Ejecutivo Cumbre', 40, 150000.00, 'disponible'
WHERE NOT EXISTS (SELECT 1 FROM espacios WHERE nombre = 'Studio Ejecutivo Cumbre');

-- 6.4 Servicios Adicionales
INSERT INTO servicios_adicionales (nombre, descripcion, precio_unitario)
SELECT 'Catering Gourmet (Por Persona)', 'Menú de 3 tiempos, bebidas suaves y estación de café.', 65000.00
WHERE NOT EXISTS (SELECT 1 FROM servicios_adicionales WHERE nombre = 'Catering Gourmet (Por Persona)');

INSERT INTO servicios_adicionales (nombre, descripcion, precio_unitario)
SELECT 'Sonido e Iluminación Profesional', 'Consola digital, micrófonos inalámbricos, cabina DJ y luces arquitectónicas.', 450000.00
WHERE NOT EXISTS (SELECT 1 FROM servicios_adicionales WHERE nombre = 'Sonido e Iluminación Profesional');

INSERT INTO servicios_adicionales (nombre, descripcion, precio_unitario)
SELECT 'Decoración Floral y Mobiliario', 'Centros de mesa, arco principal y ambientación temática.', 380000.00
WHERE NOT EXISTS (SELECT 1 FROM servicios_adicionales WHERE nombre = 'Decoración Floral y Mobiliario');

INSERT INTO servicios_adicionales (nombre, descripcion, precio_unitario)
SELECT 'Fotografía y Video Cobertura Total', 'Registro fotográfico digital y aftermovie en 4K del evento.', 520000.00
WHERE NOT EXISTS (SELECT 1 FROM servicios_adicionales WHERE nombre = 'Fotografía y Video Cobertura Total');

INSERT INTO servicios_adicionales (nombre, descripcion, precio_unitario)
SELECT 'Streaming y Grabación HD', 'Transmisión en vivo multicámara y soporte técnico en sala.', 320000.00
WHERE NOT EXISTS (SELECT 1 FROM servicios_adicionales WHERE nombre = 'Streaming y Grabación HD');

-- 6.5 Empleados Coordinadores
INSERT INTO empleados (nombre, cargo, email)
SELECT 'Laura Valentina Gómez', 'Coordinadora Senior de Eventos', 'laura.gomez@reservasbosque.edu.co'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email = 'laura.gomez@reservasbosque.edu.co');

INSERT INTO empleados (nombre, cargo, email)
SELECT 'Carlos Andrés Mendoza', 'Coordinador Logístico y Corporativo', 'carlos.mendoza@reservasbosque.edu.co'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email = 'carlos.mendoza@reservasbosque.edu.co');

INSERT INTO empleados (nombre, cargo, email)
SELECT 'Diana Carolina Ríos', 'Especialista de Bodas y Protocolo', 'diana.rios@reservasbosque.edu.co'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email = 'diana.rios@reservasbosque.edu.co');

-- 6.6 Clientes Iniciales
INSERT INTO clientes (tipo_cliente, identificacion, nombre_razon_social, email, telefono)
SELECT 'natural', '1020754321', 'Alejandro Rodríguez Pérez', 'alejandro.rodriguez@gmail.com', '3154892031'
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE identificacion = '1020754321');

INSERT INTO clientes (tipo_cliente, identificacion, nombre_razon_social, email, telefono)
SELECT 'juridico', '900456789-1', 'TechNova Soluciones S.A.S.', 'eventos@technova.co', '3108529630'
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE identificacion = '900456789-1');

INSERT INTO clientes (tipo_cliente, identificacion, nombre_razon_social, email, telefono)
SELECT 'natural', '52987654', 'María Fernanda Torres', 'mafe.torres@outlook.com', '3207418529'
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE identificacion = '52987654');

