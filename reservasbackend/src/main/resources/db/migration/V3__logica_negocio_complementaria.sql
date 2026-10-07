-- =====================================================================
-- Complemento de Lógica de Negocio (Basado en LOGICA_NEGOCIO_BD.md)
-- Script Flyway V3
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. FUNCION: fn_validar_horario
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_horario(vd_fecha_inicio TIMESTAMP, vd_fecha_fin TIMESTAMP)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
    v_hora_inicio := EXTRACT(HOUR FROM vd_fecha_inicio);
    v_hora_fin := EXTRACT(HOUR FROM vd_fecha_fin);
    
    -- Ventana operativa: 08:00 a 22:00
    IF v_hora_inicio < 8 OR v_hora_fin >= 22 THEN
        RETURN FALSE;
    END IF;
    
    RETURN TRUE;
END;
$$;

-- ---------------------------------------------------------------------
-- 2. TRIGGER: tg_validar_fechas_coherentes
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_validar_fechas_coherentes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.fecha_fin <= NEW.fecha_inicio THEN
        RAISE EXCEPTION 'e_fechas_incoherentes: La fecha de fin debe ser posterior a la fecha de inicio';
    END IF;
    
    IF NEW.fecha_inicio < CURRENT_TIMESTAMP THEN
        RAISE EXCEPTION 'e_fechas_incoherentes: No se pueden realizar reservas en el pasado';
    END IF;
    
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tg_validar_fechas_coherentes ON reservas;
CREATE TRIGGER tg_validar_fechas_coherentes
BEFORE INSERT OR UPDATE ON reservas
FOR EACH ROW
EXECUTE FUNCTION fn_trg_validar_fechas_coherentes();

-- ---------------------------------------------------------------------
-- 3. TRIGGER: tg_evitar_borrado_facturas
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_trg_evitar_borrado_facturas()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'e_borrado_facturas: Las facturas no pueden ser eliminadas del sistema. Utilice anulación lógica.';
    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS tg_evitar_borrado_facturas ON facturas;
CREATE TRIGGER tg_evitar_borrado_facturas
BEFORE DELETE ON facturas
FOR EACH ROW
EXECUTE FUNCTION fn_trg_evitar_borrado_facturas();

-- ---------------------------------------------------------------------
-- 4. ÍNDICES DE OPTIMIZACIÓN (Performance)
-- ---------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS ix_reservas_rango_fechas ON reservas (id_espacio, fecha_inicio, fecha_fin);
CREATE INDEX IF NOT EXISTS ix_facturas_id_reserva ON facturas (id_reserva);
CREATE INDEX IF NOT EXISTS ix_clientes_identificacion ON clientes (identificacion);

-- ---------------------------------------------------------------------
-- 5. REESCRITURA DE SP_CREAR_RESERVA (Para soportar JSON de servicios)
-- ---------------------------------------------------------------------
-- Nota: En V2 ya existía, pero la LOGICA NEGOCIO exige que procese
-- un arreglo JSON para insertar los servicios adicionales en la misma transacción.
DROP PROCEDURE IF EXISTS SP_CREAR_RESERVA(INT, INT, INT, TIMESTAMP, TIMESTAMP, INT);

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
    v_costo_inicial DECIMAL(12,2);
    r_servicio RECORD;
    v_precio_unitario DECIMAL(12,2);
    v_subtotal DECIMAL(12,2);
BEGIN
    -- 1. Validar Disponibilidad
    IF NOT fn_validar_disponibilidad(p_id_espacio, p_fecha_inicio, p_fecha_fin) THEN
        RAISE EXCEPTION 'e_espacio_ocupado: El espacio % no tiene disponibilidad en las fechas dadas', p_id_espacio;
    END IF;

    -- 2. Validar Horario Operativo
    IF NOT fn_validar_horario(p_fecha_inicio, p_fecha_fin) THEN
        RAISE EXCEPTION 'e_horario_invalido: La reserva debe estar dentro del horario operativo (08:00 - 22:00)';
    END IF;

    -- Calcular tarifa base
    SELECT tarifa_por_hora INTO v_tarifa FROM espacios WHERE id_espacio = p_id_espacio;
    vd_horas := EXTRACT(EPOCH FROM (p_fecha_fin - p_fecha_inicio)) / 3600.0;
    IF vd_horas < 1 THEN vd_horas := 1; END IF;
    v_costo_inicial := ROUND((vd_horas * COALESCE(v_tarifa, 0))::numeric, 2);

    -- 3. Crear el registro maestro de la reserva
    INSERT INTO reservas (id_cliente, id_espacio, id_empleado, fecha_reserva, fecha_inicio, fecha_fin, estado, costo_total)
    VALUES (p_id_cliente, p_id_espacio, p_id_empleado, CURRENT_TIMESTAMP, p_fecha_inicio, p_fecha_fin, 'pendiente', v_costo_inicial)
    RETURNING id_reserva INTO p_id_reserva;

    -- 4. Procesar JSON de servicios (Arreglo: [{"id_servicio": 1, "cantidad": 2}, ...])
    IF p_servicios_json IS NOT NULL THEN
        FOR r_servicio IN SELECT * FROM jsonb_to_recordset(p_servicios_json) AS x(id_servicio INT, cantidad INT)
        LOOP
            SELECT precio_unitario INTO v_precio_unitario 
            FROM servicios_adicionales 
            WHERE id_servicio = r_servicio.id_servicio;
            
            v_subtotal := v_precio_unitario * r_servicio.cantidad;
            
            INSERT INTO reserva_servicios (id_reserva, id_servicio, cantidad, precio_unitario, subtotal)
            VALUES (p_id_reserva, r_servicio.id_servicio, r_servicio.cantidad, v_precio_unitario, v_subtotal);
        END LOOP;
        
        -- 5. Recalcular costo total con los servicios añadidos
        UPDATE reservas 
        SET costo_total = fn_calcular_costo_total(p_id_reserva) 
        WHERE id_reserva = p_id_reserva;
    END IF;
END;
$$;
