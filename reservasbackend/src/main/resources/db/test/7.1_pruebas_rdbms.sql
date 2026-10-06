-- =====================================================================
-- EDT 7.1 - Pruebas del RDBMS (PostgreSQL 16)
-- Script de ejecucion y verificacion automatizada
-- Valida: Funciones, Procedimientos y Triggers
-- =====================================================================

DO $$
DECLARE
    v_id_sucursal INT;
    v_id_espacio INT;
    v_id_cliente INT;
    v_id_empleado INT;
    v_id_servicio INT;
    v_id_reserva_1 INT;
    v_id_reserva_2 INT;
    v_id_factura INT;
    v_id_pago INT;
    v_disponible BOOLEAN;
    v_costo_calculado DECIMAL(12,2);
    v_cruce_bloqueado BOOLEAN := FALSE;
    v_factura_creada BOOLEAN := FALSE;
    v_estado_pago_res VARCHAR(20);
    v_fecha_base TIMESTAMP := DATE_TRUNC('hour', CURRENT_TIMESTAMP + INTERVAL '5 days');
BEGIN
    RAISE NOTICE '=====================================================';
    RAISE NOTICE 'INICIANDO SUITE DE PRUEBAS DEL RDBMS (EDT 7.1)';
    RAISE NOTICE '=====================================================';

    -- -----------------------------------------------------------------
    -- 0. PREPARACION DE DATOS DE PRUEBA
    -- -----------------------------------------------------------------
    INSERT INTO sucursales (nombre, direccion, ciudad, telefono)
    VALUES ('Sucursal Norte Test', 'Calle 100 # 15-20', 'Bogota', '3001234567')
    RETURNING id_sucursal INTO v_id_sucursal;

    INSERT INTO espacios (id_sucursal, nombre, tipo, capacidad, tarifa_por_hora, estado)
    VALUES (v_id_sucursal, 'Salon Imperial Test', 'Auditorio', 100, 150000.00, 'disponible')
    RETURNING id_espacio INTO v_id_espacio;

    INSERT INTO clientes (tipo_cliente, identificacion, nombre_razon_social, email, telefono)
    VALUES ('natural', 'CC-TEST-' || EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::INT, 'Cliente Prueba RDBMS', 'test@rdbms.com', '3109876543')
    RETURNING id_cliente INTO v_id_cliente;

    INSERT INTO empleados (nombre, cargo, email)
    VALUES ('Empleado Prueba RDBMS', 'Coordinador', 'emp_test@rdbms.com')
    RETURNING id_empleado INTO v_id_empleado;

    INSERT INTO servicios_adicionales (nombre, descripcion, precio_unitario)
    VALUES ('Sonido Profesional Test', 'Microfonia y consola', 200000.00)
    RETURNING id_servicio INTO v_id_servicio;

    -- -----------------------------------------------------------------
    -- PRUEBA 1: Procedimiento sp_crear_reserva y fn_validar_disponibilidad
    -- -----------------------------------------------------------------
    CALL sp_crear_reserva(
        v_id_cliente,
        v_id_espacio,
        v_id_empleado,
        v_fecha_base + INTERVAL '10 hours',
        v_fecha_base + INTERVAL '14 hours',
        v_id_reserva_1
    );

    IF v_id_reserva_1 IS NULL THEN
        RAISE EXCEPTION 'FALLO: sp_crear_reserva no retorno id_reserva';
    END IF;
    RAISE NOTICE '[OK] Procedimiento sp_crear_reserva: Reserva #% creada con exito.', v_id_reserva_1;

    -- Validar que la funcion reporte ocupado para ese mismo rango
    v_disponible := fn_validar_disponibilidad(v_id_espacio, v_fecha_base + INTERVAL '11 hours', v_fecha_base + INTERVAL '13 hours');
    IF v_disponible = FALSE THEN
        RAISE NOTICE '[OK] Funcion fn_validar_disponibilidad: Detecto ocupacion correctamente (Retorno FALSE).';
    ELSE
        RAISE EXCEPTION 'FALLO: fn_validar_disponibilidad reporto disponible cuando existe cruce.';
    END IF;

    -- -----------------------------------------------------------------
    -- PRUEBA 2: Trigger trg_validar_cruce_horario (RF-08)
    -- -----------------------------------------------------------------
    BEGIN
        -- Intentar insertar segunda reserva que se solapa (12:00 a 16:00)
        INSERT INTO reservas (id_cliente, id_espacio, id_empleado, fecha_inicio, fecha_fin, estado)
        VALUES (v_id_cliente, v_id_espacio, v_id_empleado, v_fecha_base + INTERVAL '12 hours', v_fecha_base + INTERVAL '16 hours', 'pendiente');
    EXCEPTION
        WHEN OTHERS THEN
            v_cruce_bloqueado := TRUE;
            RAISE NOTICE '[OK] Trigger trg_validar_cruce_horario: Bloqueo exitosamente el cruce con mensaje: %', SQLERRM;
    END;

    IF NOT v_cruce_bloqueado THEN
        RAISE EXCEPTION 'FALLO: El trigger no impidio la insercion con cruce de horario.';
    END IF;

    -- -----------------------------------------------------------------
    -- PRUEBA 3: Trigger trg_recalcular_costo_servicios y Funcion fn_calcular_costo_total (RF-12)
    -- -----------------------------------------------------------------
    -- Costo base: 4 horas * 150.000 = 600.000
    INSERT INTO reserva_servicios (id_reserva, id_servicio, cantidad)
    VALUES (v_id_reserva_1, v_id_servicio, 2); -- 2 * 200.000 = 400.000 -> Total esperado: 1.000.000

    SELECT costo_total INTO v_costo_calculado FROM reservas WHERE id_reserva = v_id_reserva_1;
    IF v_costo_calculado = 1000000.00 THEN
        RAISE NOTICE '[OK] Trigger trg_recalcular_costo_servicios y Funcion fn_calcular_costo_total: Recalculo exacto (Total = %)', v_costo_calculado;
    ELSE
        RAISE EXCEPTION 'FALLO: Recalculo incorrecto. Esperado 1000000.00, obtenido %', v_costo_calculado;
    END IF;

    -- -----------------------------------------------------------------
    -- PRUEBA 4: Trigger trg_generar_factura_al_confirmar (RF-14)
    -- -----------------------------------------------------------------
    UPDATE reservas SET estado = 'confirmada' WHERE id_reserva = v_id_reserva_1;

    SELECT id_factura INTO v_id_factura FROM facturas WHERE id_reserva = v_id_reserva_1;
    IF v_id_factura IS NOT NULL THEN
        RAISE NOTICE '[OK] Trigger trg_generar_factura_al_confirmar: Factura #% generada automaticamente al confirmar reserva.', v_id_factura;
    ELSE
        RAISE EXCEPTION 'FALLO: No se genero la factura automatica al cambiar estado a confirmada.';
    END IF;

    -- -----------------------------------------------------------------
    -- PRUEBA 5: Procedimiento sp_registrar_pago y Funcion fn_estado_pago (RF-13, RF-15)
    -- -----------------------------------------------------------------
    CALL sp_registrar_pago(v_id_factura, 'anticipo', 500000.00, 'transferencia', v_id_pago);
    IF v_id_pago IS NULL THEN
        RAISE EXCEPTION 'FALLO: sp_registrar_pago no retorno id_pago';
    END IF;

    v_estado_pago_res := fn_estado_pago(v_id_reserva_1);
    IF v_estado_pago_res = 'parcial' THEN
        RAISE NOTICE '[OK] Procedimiento sp_registrar_pago y Funcion fn_estado_pago: Pago registrado (Estado: %)', v_estado_pago_res;
    ELSE
        RAISE EXCEPTION 'FALLO: fn_estado_pago retorno %, esperado parcial', v_estado_pago_res;
    END IF;

    -- -----------------------------------------------------------------
    -- PRUEBA 6: Procedimiento sp_cancelar_reserva (RF-09, RNF-03)
    -- -----------------------------------------------------------------
    -- Crear otra reserva para probar cancelacion
    CALL sp_crear_reserva(
        v_id_cliente,
        v_id_espacio,
        v_id_empleado,
        v_fecha_base + INTERVAL '20 hours',
        v_fecha_base + INTERVAL '23 hours',
        v_id_reserva_2
    );

    CALL sp_cancelar_reserva(v_id_reserva_2);

    IF (SELECT estado FROM reservas WHERE id_reserva = v_id_reserva_2) = 'cancelada' THEN
        RAISE NOTICE '[OK] Procedimiento sp_cancelar_reserva: Reserva #% cancelada correctamente.', v_id_reserva_2;
    ELSE
        RAISE EXCEPTION 'FALLO: sp_cancelar_reserva no cambio el estado a cancelada.';
    END IF;

    -- Limpieza de datos de prueba
    DELETE FROM reservas WHERE id_reserva IN (v_id_reserva_1, v_id_reserva_2);
    DELETE FROM servicios_adicionales WHERE id_servicio = v_id_servicio;
    DELETE FROM espacios WHERE id_espacio = v_id_espacio;
    DELETE FROM sucursales WHERE id_sucursal = v_id_sucursal;
    DELETE FROM clientes WHERE id_cliente = v_id_cliente;
    DELETE FROM empleados WHERE id_empleado = v_id_empleado;

    RAISE NOTICE '=====================================================';
    RAISE NOTICE 'TODAS LAS PRUEBAS DEL RDBMS PASARON CON EXITO (6/6)';
    RAISE NOTICE '=====================================================';
END;
$$;
