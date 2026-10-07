-- =====================================================================
-- EDT 3.3 / 6.1 - Procedimientos, Funciones y Triggers en PostgreSQL 16
-- Script Flyway V2
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. FUNCION: fn_validar_disponibilidad (RF-05, RF-08)
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 2. TRIGGER: trg_validar_cruce_horario (RF-08, RNF-03)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_validar_cruce_horario()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.estado IN ('pendiente', 'confirmada') THEN
        IF NOT fn_validar_disponibilidad(NEW.id_espacio, NEW.fecha_inicio, NEW.fecha_fin, NEW.id_reserva) THEN
            RAISE EXCEPTION 'Cruce de horario: El espacio % no esta disponible en el rango solicitado (% a %)',
                NEW.id_espacio, NEW.fecha_inicio, NEW.fecha_fin
                USING ERRCODE = '23P01'; -- exclusion_violation
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

-- ---------------------------------------------------------------------
-- 3. FUNCION: fn_calcular_costo_total (RF-12)
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 4. TRIGGER: trg_recalcular_costo_servicios (RF-12)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_recalcular_costo_servicios()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_reserva_id INT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_reserva_id := OLD.id_reserva;
    ELSE
        -- Calcula automáticamente el subtotal si no viene establecido
        IF NEW.subtotal IS NULL OR NEW.subtotal = 0 THEN
            SELECT NEW.cantidad * COALESCE(precio_unitario, 0)
            INTO NEW.subtotal
            FROM servicios_adicionales
            WHERE id_servicio = NEW.id_servicio;
        END IF;
        v_reserva_id := NEW.id_reserva;
    END IF;

    UPDATE reservas
    SET costo_total = fn_calcular_costo_total(v_reserva_id)
    WHERE id_reserva = v_reserva_id;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_recalcular_costo_servicios ON reserva_servicios;
CREATE TRIGGER trg_recalcular_costo_servicios
BEFORE INSERT OR UPDATE OR DELETE ON reserva_servicios
FOR EACH ROW
EXECUTE FUNCTION fn_trg_recalcular_costo_servicios();

-- ---------------------------------------------------------------------
-- 5. TRIGGER: trg_generar_factura_al_confirmar (RF-14)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_generar_factura_al_confirmar()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    vv_num_factura VARCHAR(50);
BEGIN
    IF NEW.estado = 'confirmada' AND (OLD.estado IS NULL OR OLD.estado <> 'confirmada') THEN
        IF NOT EXISTS (SELECT 1 FROM facturas WHERE id_reserva = NEW.id_reserva) THEN
            vv_num_factura := 'FAC-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || LPAD(NEW.id_reserva::TEXT, 5, '0');
            INSERT INTO facturas (id_reserva, numero_factura, fecha_emision, total)
            VALUES (NEW.id_reserva, vv_num_factura, CURRENT_TIMESTAMP, NEW.costo_total);
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_generar_factura_al_confirmar ON reservas;
CREATE TRIGGER trg_generar_factura_al_confirmar
AFTER UPDATE OF estado ON reservas
FOR EACH ROW
EXECUTE FUNCTION fn_trg_generar_factura_al_confirmar();

-- ---------------------------------------------------------------------
-- 6. PROCEDIMIENTO: SP_CREAR_RESERVA (RF-07)
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SP_CREAR_RESERVA(
    IN p_id_cliente INT,
    IN p_id_espacio INT,
    IN p_id_empleado INT,
    IN p_fecha_inicio TIMESTAMP,
    IN p_fecha_fin TIMESTAMP,
    INOUT p_id_reserva INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_horas NUMERIC;
    v_tarifa DECIMAL(10,2);
    v_costo_inicial DECIMAL(12,2);
BEGIN
    IF NOT fn_validar_disponibilidad(p_id_espacio, p_fecha_inicio, p_fecha_fin) THEN
        RAISE EXCEPTION 'El espacio % no tiene disponibilidad en las fechas dadas', p_id_espacio;
    END IF;

    SELECT tarifa_por_hora INTO v_tarifa FROM espacios WHERE id_espacio = p_id_espacio;
    v_horas := EXTRACT(EPOCH FROM (p_fecha_fin - p_fecha_inicio)) / 3600.0;
    IF v_horas < 1 THEN v_horas := 1; END IF;
    v_costo_inicial := ROUND((v_horas * COALESCE(v_tarifa, 0))::numeric, 2);

    INSERT INTO reservas (id_cliente, id_espacio, id_empleado, fecha_reserva, fecha_inicio, fecha_fin, estado, costo_total)
    VALUES (p_id_cliente, p_id_espacio, p_id_empleado, CURRENT_TIMESTAMP, p_fecha_inicio, p_fecha_fin, 'pendiente', v_costo_inicial)
    RETURNING id_reserva INTO p_id_reserva;
END;
$$;

-- ---------------------------------------------------------------------
-- 7. PROCEDIMIENTO: SP_REGISTRAR_PAGO (RF-13)
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SP_REGISTRAR_PAGO(
    IN p_id_factura INT,
    IN p_tipo_pago VARCHAR(20),
    IN p_monto DECIMAL(12,2),
    IN p_metodo_pago VARCHAR(50),
    INOUT p_id_pago INT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_reserva INT;
    v_total_factura DECIMAL(12,2);
    v_pagado_acumulado DECIMAL(12,2);
BEGIN
    IF p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto del pago debe ser mayor a 0';
    END IF;

    SELECT id_reserva, total INTO v_id_reserva, v_total_factura FROM facturas WHERE id_factura = p_id_factura;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La factura % no existe', p_id_factura;
    END IF;

    INSERT INTO pagos (id_factura, tipo_pago, monto, fecha_pago, metodo_pago)
    VALUES (p_id_factura, p_tipo_pago, p_monto, CURRENT_TIMESTAMP, p_metodo_pago)
    RETURNING id_pago INTO p_id_pago;

    SELECT COALESCE(SUM(monto), 0) INTO v_pagado_acumulado FROM pagos WHERE id_factura = p_id_factura;

    -- Si se ha cubierto al menos el 30% (anticipo minimo reglamentario), la reserva se confirma
    IF v_pagado_acumulado >= (v_total_factura * 0.30) THEN
        UPDATE reservas SET estado = 'confirmada' WHERE id_reserva = v_id_reserva AND estado = 'pendiente';
    END IF;
END;
$$;

-- ---------------------------------------------------------------------
-- 8. PROCEDIMIENTO: SP_CANCELAR_RESERVA (RF-09, RNF-03)
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
    SELECT fecha_inicio, estado INTO vd_inicio, vv_estado FROM reservas WHERE id_reserva = p_id_reserva;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Reserva % no encontrada', p_id_reserva;
    END IF;

    IF vd_inicio <= CURRENT_TIMESTAMP THEN
        RAISE EXCEPTION 'No es posible cancelar una reserva cuyo evento ya ha iniciado o finalizado';
    END IF;

    IF vv_estado = 'cancelada' THEN
        RAISE EXCEPTION 'La reserva % ya se encuentra cancelada', p_id_reserva;
    END IF;

    UPDATE reservas SET estado = 'cancelada' WHERE id_reserva = p_id_reserva;
END;
$$;

-- ---------------------------------------------------------------------
-- 9. FUNCION: fn_estado_pago (RF-15)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_estado_pago(p_id_reserva INT)
RETURNS VARCHAR(20)
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_factura INT;
    v_total DECIMAL(12,2);
    v_pagado DECIMAL(12,2);
BEGIN
    SELECT id_factura, total INTO v_id_factura, v_total FROM facturas WHERE id_reserva = p_id_reserva;
    IF NOT FOUND THEN
        RETURN 'sin_factura';
    END IF;

    SELECT COALESCE(SUM(monto), 0) INTO v_pagado FROM pagos WHERE id_factura = v_id_factura;

    IF v_pagado = 0 THEN
        RETURN 'pendiente';
    ELSIF v_pagado < v_total THEN
        RETURN 'parcial';
    ELSE
        RETURN 'pagado';
    END IF;
END;
$$;
