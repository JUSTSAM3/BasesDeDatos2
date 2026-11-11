-- ==============================================================================
-- UNIVERSIDAD EL BOSQUE - BASES DE DATOS 2
-- SISTEMA DE GESTIÓN DE RESERVAS DE ESPACIOS PARA EVENTOS
-- SCRIPT 02: LÓGICA DE NEGOCIO, FUNCIONES, DISPARADORES Y PROCEDIMIENTOS
-- ==============================================================================
-- Contenido:
--   1. Funciones de cálculo y validación
--   2. Funciones y disparadores (Triggers) de integridad y recálculo
--   3. Vistas analíticas consolidadas
--   4. Procedimientos almacenados transaccionales (SPs)
-- ==============================================================================

-- ==============================================================================
-- 1. FUNCIONES DE CÁLCULO Y VALIDACIÓN
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1.1 fn_validar_disponibilidad
-- Verifica si un espacio está disponible en un rango horario determinado.
-- Excluye opcionalmente una reserva existente (útil para reprogramaciones).
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_disponibilidad(
    p_id_espacio INT,
    p_fecha_inicio TIMESTAMP,
    p_fecha_fin TIMESTAMP,
    p_id_reserva_excluir INT DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_cruce_count INT;
BEGIN
    IF p_fecha_fin <= p_fecha_inicio THEN
        RAISE EXCEPTION 'La fecha de fin debe ser posterior a la fecha de inicio';
    END IF;

    SELECT COUNT(*)
    INTO v_cruce_count
    FROM reservas r
    WHERE r.id_espacio = p_id_espacio
      AND r.estado IN ('pendiente', 'confirmada')
      AND (p_id_reserva_excluir IS NULL OR r.id_reserva <> p_id_reserva_excluir)
      AND (r.fecha_inicio < p_fecha_fin AND r.fecha_fin > p_fecha_inicio);

    RETURN (v_cruce_count = 0);
END;
$$;

-- ------------------------------------------------------------------------------
-- 1.2 fn_validar_horario
-- Garantiza que el evento se programe dentro del horario operativo (08:00 a 22:00).
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_horario(
    p_fecha_inicio TIMESTAMP,
    p_fecha_fin TIMESTAMP
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_hora_inicio TIME;
    v_hora_fin TIME;
BEGIN
    IF p_fecha_fin <= p_fecha_inicio THEN
        RETURN FALSE;
    END IF;

    v_hora_inicio := p_fecha_inicio::TIME;
    v_hora_fin := p_fecha_fin::TIME;

    IF v_hora_inicio < TIME '08:00:00' OR v_hora_inicio > TIME '22:00:00' THEN
        RETURN FALSE;
    END IF;

    IF v_hora_fin < TIME '08:00:00' OR v_hora_fin > TIME '22:00:00' THEN
        RETURN FALSE;
    END IF;

    RETURN TRUE;
END;
$$;

-- ------------------------------------------------------------------------------
-- 1.3 fn_calcular_costo_total
-- Calcula el costo total de una reserva: (horas * tarifa_por_hora) + suma de servicios.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_calcular_costo_total(p_id_reserva INT)
RETURNS DECIMAL(12,2)
LANGUAGE plpgsql
AS $$
DECLARE
    v_costo_espacio DECIMAL(12,2) := 0.00;
    v_costo_servicios DECIMAL(12,2) := 0.00;
    v_horas NUMERIC;
    v_tarifa DECIMAL(10,2);
    vd_inicio TIMESTAMP;
    vd_fin TIMESTAMP;
    v_id_espacio INT;
BEGIN
    SELECT r.fecha_inicio, r.fecha_fin, r.id_espacio
    INTO vd_inicio, vd_fin, v_id_espacio
    FROM reservas r
    WHERE r.id_reserva = p_id_reserva;

    IF NOT FOUND THEN
        RETURN 0.00;
    END IF;

    SELECT tarifa_por_hora
    INTO v_tarifa
    FROM espacios
    WHERE id_espacio = v_id_espacio;

    v_horas := EXTRACT(EPOCH FROM (vd_fin - vd_inicio)) / 3600.0;
    IF v_horas < 1 THEN
        v_horas := 1;
    END IF;
    v_costo_espacio := ROUND((v_horas * COALESCE(v_tarifa, 0.00))::numeric, 2);

    SELECT COALESCE(SUM(subtotal), 0.00)
    INTO v_costo_servicios
    FROM reserva_servicios
    WHERE id_reserva = p_id_reserva;

    RETURN v_costo_espacio + v_costo_servicios;
END;
$$;

-- ------------------------------------------------------------------------------
-- 1.4 fn_estado_pago
-- Determina el estado de pago de una reserva: sin_factura, pendiente, parcial o pagado.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_estado_pago(p_id_reserva INT)
RETURNS VARCHAR(20)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_factura INT;
    v_total DECIMAL(12,2);
    v_pagado DECIMAL(12,2);
BEGIN
    SELECT id_factura, total INTO v_id_factura, v_total
    FROM facturas WHERE id_reserva = p_id_reserva;

    IF NOT FOUND THEN
        RETURN 'sin_factura';
    END IF;

    SELECT COALESCE(SUM(monto), 0) INTO v_pagado
    FROM pagos WHERE id_factura = v_id_factura;

    IF v_pagado = 0 THEN
        RETURN 'pendiente';
    ELSIF v_pagado < v_total THEN
        RETURN 'parcial';
    ELSE
        RETURN 'pagado';
    END IF;
END;
$$;

-- ==============================================================================
-- 2. DISPARADORES (TRIGGERS) Y FUNCIONES DE DISPARADOR
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 2.1 Validación de fechas lógicas en reservas
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_validar_fechas_coherentes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.fecha_fin <= NEW.fecha_inicio THEN
        RAISE EXCEPTION 'e_fechas_incoherentes: La fecha de fin debe ser posterior a la fecha de inicio';
    END IF;

    IF TG_OP = 'INSERT' OR NEW.fecha_inicio <> OLD.fecha_inicio THEN
        IF NEW.fecha_inicio < CURRENT_TIMESTAMP THEN
            RAISE EXCEPTION 'e_fechas_incoherentes: No se pueden realizar reservas en el pasado';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tg_validar_fechas_coherentes ON reservas;
CREATE TRIGGER tg_validar_fechas_coherentes
BEFORE INSERT OR UPDATE ON reservas
FOR EACH ROW
EXECUTE FUNCTION fn_trg_validar_fechas_coherentes();

-- ------------------------------------------------------------------------------
-- 2.2 Validación de disponibilidad en reservas (capa PL/pgSQL previa a GiST)
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_validar_cruce_horario()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.estado IN ('pendiente', 'confirmada') THEN
        IF NOT fn_validar_disponibilidad(NEW.id_espacio, NEW.fecha_inicio, NEW.fecha_fin, NEW.id_reserva) THEN
            RAISE EXCEPTION 'Cruce de horario: El espacio % no esta disponible en el rango solicitado (% a %)',
                NEW.id_espacio, NEW.fecha_inicio, NEW.fecha_fin
                USING ERRCODE = '23P01';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_cruce_horario ON reservas;
CREATE TRIGGER trg_validar_cruce_horario
BEFORE INSERT OR UPDATE ON reservas
FOR EACH ROW
EXECUTE FUNCTION fn_trg_validar_cruce_horario();

-- ------------------------------------------------------------------------------
-- 2.3 Recálculo de costo de reservas al modificar fechas o salón
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_recalcular_costo_reserva()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_tarifa DECIMAL(10,2);
    v_horas NUMERIC;
    v_costo_espacio DECIMAL(12,2) := 0.00;
    v_costo_servicios DECIMAL(12,2) := 0.00;
    v_nuevo_costo DECIMAL(12,2);
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.costo_total IS NULL OR NEW.costo_total = 0 THEN
            SELECT tarifa_por_hora INTO v_tarifa
            FROM espacios
            WHERE id_espacio = NEW.id_espacio;

            IF NEW.fecha_fin > NEW.fecha_inicio THEN
                v_horas := EXTRACT(EPOCH FROM (NEW.fecha_fin - NEW.fecha_inicio)) / 3600.0;
                IF v_horas < 1 THEN
                    v_horas := 1;
                END IF;
            ELSE
                v_horas := 1;
            END IF;

            NEW.costo_total := ROUND((v_horas * COALESCE(v_tarifa, 0.00))::numeric, 2);
        END IF;

    ELSIF TG_OP = 'UPDATE' THEN
        IF (NEW.fecha_inicio IS DISTINCT FROM OLD.fecha_inicio) OR
           (NEW.fecha_fin IS DISTINCT FROM OLD.fecha_fin) OR
           (NEW.id_espacio IS DISTINCT FROM OLD.id_espacio) THEN

            SELECT tarifa_por_hora INTO v_tarifa
            FROM espacios
            WHERE id_espacio = NEW.id_espacio;

            IF NOT FOUND THEN
                RAISE EXCEPTION 'e_espacio_no_existe: El espacio % no existe', NEW.id_espacio;
            END IF;

            IF NEW.fecha_fin > NEW.fecha_inicio THEN
                v_horas := EXTRACT(EPOCH FROM (NEW.fecha_fin - NEW.fecha_inicio)) / 3600.0;
                IF v_horas < 1 THEN
                    v_horas := 1;
                END IF;
            ELSE
                v_horas := 1;
            END IF;

            v_costo_espacio := ROUND((v_horas * COALESCE(v_tarifa, 0.00))::numeric, 2);

            SELECT COALESCE(SUM(subtotal), 0.00)
            INTO v_costo_servicios
            FROM reserva_servicios
            WHERE id_reserva = NEW.id_reserva;

            v_nuevo_costo := v_costo_espacio + v_costo_servicios;
            NEW.costo_total := v_nuevo_costo;

            -- Sincronizar factura asociada (relación 1:1) si ya fue emitida
            UPDATE facturas
            SET total = v_nuevo_costo
            WHERE id_reserva = NEW.id_reserva
              AND total IS DISTINCT FROM v_nuevo_costo;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_recalcular_costo_reserva ON reservas;
CREATE TRIGGER trg_recalcular_costo_reserva
BEFORE INSERT OR UPDATE ON reservas
FOR EACH ROW
EXECUTE FUNCTION fn_trg_recalcular_costo_reserva();

-- ------------------------------------------------------------------------------
-- 2.4 Cálculo automático de subtotal en servicios adicionales
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_calcular_subtotal()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.subtotal IS NULL OR NEW.subtotal = 0 THEN
        IF NEW.precio_unitario IS NULL OR NEW.precio_unitario = 0 THEN
            SELECT precio_unitario INTO NEW.precio_unitario
            FROM servicios_adicionales
            WHERE id_servicio = NEW.id_servicio;
        END IF;
        NEW.subtotal := NEW.cantidad * COALESCE(NEW.precio_unitario, 0);
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_calcular_subtotal ON reserva_servicios;
CREATE TRIGGER trg_calcular_subtotal
BEFORE INSERT OR UPDATE ON reserva_servicios
FOR EACH ROW
EXECUTE FUNCTION fn_trg_calcular_subtotal();

-- ------------------------------------------------------------------------------
-- 2.5 Actualización del total de reserva y factura al modificar servicios
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_actualizar_total_reserva()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_reserva_nueva INT;
    v_reserva_anterior INT;
    v_reserva_id INT;
    v_nuevo_total DECIMAL(12,2);
BEGIN
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        v_reserva_nueva := NEW.id_reserva;
    END IF;
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_reserva_anterior := OLD.id_reserva;
    END IF;

    FOR v_reserva_id IN
        SELECT DISTINCT x.id_reserva
        FROM (VALUES (v_reserva_anterior), (v_reserva_nueva)) AS x(id_reserva)
        WHERE x.id_reserva IS NOT NULL
    LOOP
        v_nuevo_total := COALESCE(fn_calcular_costo_total(v_reserva_id), 0.00);

        UPDATE reservas
        SET costo_total = v_nuevo_total
        WHERE id_reserva = v_reserva_id;

        UPDATE facturas
        SET total = v_nuevo_total
        WHERE id_reserva = v_reserva_id
          AND total IS DISTINCT FROM v_nuevo_total;
    END LOOP;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_recalcular_costo_servicios ON reserva_servicios;
CREATE TRIGGER trg_recalcular_costo_servicios
AFTER INSERT OR UPDATE OR DELETE ON reserva_servicios
FOR EACH ROW
EXECUTE FUNCTION fn_trg_actualizar_total_reserva();

-- ------------------------------------------------------------------------------
-- 2.6 Protección contra borrado de facturas (Inmutabilidad fiscal)
-- ------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_evitar_borrado_facturas()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Operación no permitida: Las facturas emitidas no pueden ser eliminadas físicamente por trazabilidad fiscal.';
    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS tg_evitar_borrado_facturas ON facturas;
CREATE TRIGGER tg_evitar_borrado_facturas
BEFORE DELETE ON facturas
FOR EACH ROW
EXECUTE FUNCTION fn_trg_evitar_borrado_facturas();

-- ==============================================================================
-- 3. VISTAS ANALÍTICAS
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 3.1 Vista: vw_historial_cliente
-- Consolida el historial de reservas de clientes con sus facturas, pagos,
-- saldos pendientes y estado de pago calculado.
-- ------------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_historial_cliente AS
SELECT
    c.id_cliente,
    c.tipo_cliente,
    c.identificacion,
    c.nombre_razon_social,
    r.id_reserva,
    e.nombre AS espacio,
    s.nombre AS sucursal,
    r.fecha_inicio,
    r.fecha_fin,
    r.estado AS estado_reserva,
    r.costo_total,
    COALESCE(f.numero_factura, 'SIN-EMITIR') AS numero_factura,
    COALESCE(SUM(p.monto), 0.00) AS monto_abonado,
    r.costo_total - COALESCE(SUM(p.monto), 0.00) AS saldo_pendiente,
    fn_estado_pago(r.id_reserva) AS estado_pago
FROM clientes c
JOIN reservas r ON c.id_cliente = r.id_cliente
JOIN espacios e ON r.id_espacio = e.id_espacio
JOIN sucursales s ON e.id_sucursal = s.id_sucursal
LEFT JOIN facturas f ON r.id_reserva = f.id_reserva
LEFT JOIN pagos p ON f.id_factura = p.id_factura
GROUP BY c.id_cliente, c.tipo_cliente, c.identificacion, c.nombre_razon_social,
         r.id_reserva, e.nombre, s.nombre, r.fecha_inicio, r.fecha_fin,
         r.estado, r.costo_total, f.numero_factura
ORDER BY c.id_cliente, r.fecha_inicio DESC;

-- ==============================================================================
-- 4. PROCEDIMIENTOS ALMACENADOS (TRANSACCIONES DE NEGOCIO)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 4.1 SP_CREAR_RESERVA
-- Crea una reserva completa validando disponibilidad, estado del recinto y horario,
-- procesando un arreglo JSONB de servicios adicionales en forma atómica.
-- ------------------------------------------------------------------------------
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
    -- 1. Validar que el espacio exista y esté disponible
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

    -- 2. Validar disponibilidad de horario
    IF NOT fn_validar_disponibilidad(p_id_espacio, p_fecha_inicio, p_fecha_fin) THEN
        RAISE EXCEPTION 'e_espacio_ocupado: El espacio % no tiene disponibilidad en las fechas dadas', p_id_espacio;
    END IF;

    -- 3. Validar horario operativo (08:00 - 22:00)
    IF NOT fn_validar_horario(p_fecha_inicio, p_fecha_fin) THEN
        RAISE EXCEPTION 'e_horario_invalido: La reserva debe estar dentro del horario operativo (08:00 - 22:00)';
    END IF;

    -- 4. Calcular costo base por horas
    vd_horas := EXTRACT(EPOCH FROM (p_fecha_fin - p_fecha_inicio)) / 3600.0;
    IF vd_horas < 1 THEN
        vd_horas := 1;
    END IF;
    v_costo_inicial := ROUND((vd_horas * COALESCE(v_tarifa, 0))::numeric, 2);

    -- 5. Insertar el registro maestro de la reserva
    INSERT INTO reservas (id_cliente, id_espacio, id_empleado, fecha_reserva, fecha_inicio, fecha_fin, estado, costo_total)
    VALUES (p_id_cliente, p_id_espacio, p_id_empleado, CURRENT_TIMESTAMP, p_fecha_inicio, p_fecha_fin, 'pendiente', v_costo_inicial)
    RETURNING id_reserva INTO p_id_reserva;

    -- 6. Procesar arreglo JSONB de servicios adicionales si aplica
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
        RAISE NOTICE '[SP_CREAR_RESERVA] Rollback automático por error: % (SQLSTATE: %)', SQLERRM, SQLSTATE;
        RAISE;
END;
$$;

-- ------------------------------------------------------------------------------
-- 4.2 SP_REGISTRAR_PAGO
-- Registra pagos asociados a una reserva, genera la factura fiscal en relación 1:1,
-- controla sobrepagos y confirma automáticamente la reserva al cubrir el anticipo (>= 30%).
-- ------------------------------------------------------------------------------
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

    -- Obtener o generar la factura asociada (Relación 1:1)
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

    -- Confirmar reserva automáticamente si se cubre al menos el 30% de anticipo
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

-- ------------------------------------------------------------------------------
-- 4.3 SP_CANCELAR_RESERVA
-- Cancela una reserva validando existencia, vigencia temporal y estado previo.
-- ------------------------------------------------------------------------------
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

