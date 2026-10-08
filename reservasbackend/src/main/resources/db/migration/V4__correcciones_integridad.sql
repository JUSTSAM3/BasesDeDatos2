-- =====================================================================
-- Correcciones de integridad, concurrencia y dependencias circulares
-- Script Flyway V4
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. CORRECCION D1: Agregar columna precio_unitario a reserva_servicios
-- ---------------------------------------------------------------------
ALTER TABLE reserva_servicios
ADD COLUMN IF NOT EXISTS precio_unitario DECIMAL(10,2) NOT NULL DEFAULT 0.00;

-- ---------------------------------------------------------------------
-- 2. CORRECCION D4: Trigger recálculo costo 
-- El cálculo del subtotal antes del INSERT/UPDATE, y el update a reservas después.
-- ---------------------------------------------------------------------
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

CREATE OR REPLACE FUNCTION fn_trg_actualizar_total_reserva()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_reserva_id INT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_reserva_id := OLD.id_reserva;
    ELSE
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
AFTER INSERT OR UPDATE OR DELETE ON reserva_servicios
FOR EACH ROW
EXECUTE FUNCTION fn_trg_actualizar_total_reserva();

-- ---------------------------------------------------------------------
-- 3. CORRECCION D5: Fechas coherentes solo en INSERT o si cambian
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 4. CORRECCION D2: Resolver ciclo factura <-> pago
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_REGISTRAR_PAGO(INT, VARCHAR, DECIMAL, VARCHAR, INT);

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
    v_pagado_acumulado DECIMAL(12,2);
BEGIN
    IF p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto del pago debe ser mayor a 0';
    END IF;

    SELECT costo_total INTO v_costo_total FROM reservas WHERE id_reserva = p_id_reserva;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La reserva % no existe', p_id_reserva;
    END IF;

    SELECT id_factura INTO v_id_factura FROM facturas WHERE id_reserva = p_id_reserva;
    IF NOT FOUND THEN
        vv_num_factura := 'FAC-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || LPAD(p_id_reserva::TEXT, 5, '0');
        INSERT INTO facturas (id_reserva, numero_factura, fecha_emision, total)
        VALUES (p_id_reserva, vv_num_factura, CURRENT_TIMESTAMP, v_costo_total)
        RETURNING id_factura INTO v_id_factura;
    END IF;

    INSERT INTO pagos (id_factura, tipo_pago, monto, fecha_pago, metodo_pago)
    VALUES (v_id_factura, p_tipo_pago, p_monto, CURRENT_TIMESTAMP, p_metodo_pago)
    RETURNING id_pago INTO p_id_pago;

    SELECT COALESCE(SUM(monto), 0) INTO v_pagado_acumulado FROM pagos WHERE id_factura = v_id_factura;

    IF v_pagado_acumulado >= (v_costo_total * 0.30) THEN
        UPDATE reservas SET estado = 'confirmada' WHERE id_reserva = p_id_reserva AND estado = 'pendiente';
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_generar_factura_al_confirmar ON reservas;

-- ---------------------------------------------------------------------
-- 5. CORRECCION D6: Constraints 
-- ---------------------------------------------------------------------
ALTER TABLE reservas
ADD CONSTRAINT chk_fechas CHECK (fecha_fin > fecha_inicio);

-- En desarrollo de pruebas a veces los seeders no tienen empleado_id
ALTER TABLE reservas ALTER COLUMN id_empleado DROP NOT NULL;

ALTER TABLE reserva_servicios
ADD CONSTRAINT chk_cantidad CHECK (cantidad > 0);

ALTER TABLE pagos
ADD CONSTRAINT chk_monto CHECK (monto > 0);

-- Si no hay registros duplicados, podemos aplicarlo
ALTER TABLE facturas
ADD CONSTRAINT unq_facturas_id_reserva UNIQUE (id_reserva);

-- ---------------------------------------------------------------------
-- 6. CORRECCION D3: Concurrencia con btree_gist
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservas
DROP CONSTRAINT IF EXISTS no_solapamiento,
ADD CONSTRAINT no_solapamiento 
EXCLUDE USING gist (
    id_espacio WITH =, 
    tsrange(fecha_inicio, fecha_fin) WITH &&
) 
WHERE (estado IN ('pendiente','confirmada'));
