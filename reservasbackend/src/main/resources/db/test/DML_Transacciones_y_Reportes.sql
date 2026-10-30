-- ==============================================================================
-- UNIVERSIDAD EL BOSQUE - BASES DE DATOS 2
-- SISTEMA DE GESTIÓN DE RESERVAS DE ESPACIOS PARA EVENTOS
-- SCRIPT DE INSERCIONES (DML), TRANSACCIONES (COMMIT / ROLLBACK) Y REPORTES SQL
-- ==============================================================================
-- Cumple con los entregables de Fase 1 (Documento SQL de reportes) y Fase 2
-- (Scripts de inserción, manejo de transacciones COMMIT/ROLLBACK y excepciones).
-- ==============================================================================

-- ==============================================================================
-- 1. DEMOSTRACIÓN DE TRANSACCIÓN EXITOSA CON COMMIT (ACID)
-- ==============================================================================
-- Crea una reserva completa con servicios adicionales y registra su anticipo
-- dentro de una transacción explícita confirmada con COMMIT.
-- ==============================================================================

BEGIN;

DO $$
DECLARE
    v_id_cliente INT;
    v_id_espacio INT;
    v_id_empleado INT;
    v_id_servicio_sonido INT;
    v_id_reserva INT;
    v_id_pago INT;
    vd_inicio TIMESTAMP := (CURRENT_DATE + INTERVAL '15 days' + INTERVAL '14 hours')::TIMESTAMP;
    vd_fin TIMESTAMP    := (CURRENT_DATE + INTERVAL '15 days' + INTERVAL '18 hours')::TIMESTAMP;
BEGIN
    SELECT id_cliente INTO v_id_cliente FROM clientes WHERE identificacion = '900456789-1' LIMIT 1;
    SELECT id_espacio INTO v_id_espacio FROM espacios WHERE nombre = 'Auditorio Magno El Bosque' LIMIT 1;
    SELECT id_empleado INTO v_id_empleado FROM empleados WHERE email = 'carlos.mendoza@reservasbosque.edu.co' LIMIT 1;
    SELECT id_servicio INTO v_id_servicio_sonido FROM servicios_adicionales WHERE nombre = 'Sonido e Iluminación Profesional' LIMIT 1;

    -- Invocación del Procedimiento Almacenado SP_CREAR_RESERVA con JSONB
    IF fn_validar_disponibilidad(v_id_espacio, vd_inicio, vd_fin) THEN
        CALL SP_CREAR_RESERVA(
            v_id_cliente,
            v_id_espacio,
            v_id_empleado,
            vd_inicio,
            vd_fin,
            jsonb_build_array(
                jsonb_build_object('id_servicio', v_id_servicio_sonido, 'cantidad', 1)
            ),
            v_id_reserva
        );

        -- Registro del pago de anticipo (>= 30%), generando factura 1:1 y confirmando la reserva
        CALL SP_REGISTRAR_PAGO(
            v_id_reserva,
            'anticipo',
            700000.00,
            'transferencia',
            v_id_pago
        );

        RAISE NOTICE 'Transacción lista para COMMIT -> Reserva ID: %, Pago ID: %, Estado Pago: %',
            v_id_reserva, v_id_pago, fn_estado_pago(v_id_reserva);
    END IF;
END;
$$;

COMMIT;

-- ==============================================================================
-- 2. DEMOSTRACIÓN DE ATOMICIDAD Y REVERSIÓN CON ROLLBACK
-- ==============================================================================
-- Escenario: Se inserta un cliente temporal y se intenta forzar una operación
-- inválida; al ejecutar ROLLBACK, ningún cambio persiste en la base de datos.
-- ==============================================================================

BEGIN;

INSERT INTO clientes (tipo_cliente, identificacion, nombre_razon_social, email, telefono)
VALUES ('natural', 'CC-TEMP-ROLLBACK', 'Usuario Temporal Rollback', 'rollback@test.com', '3000000000');

-- Verificación dentro de la transacción antes de revertir
SELECT id_cliente, identificacion, nombre_razon_social
FROM clientes
WHERE identificacion = 'CC-TEMP-ROLLBACK';

-- Se aborta la transacción para demostrar que el registro se revierte íntegramente
ROLLBACK;

-- Comprobación post-ROLLBACK (debe retornar 0 filas)
SELECT COUNT(*) AS registros_post_rollback
FROM clientes
WHERE identificacion = 'CC-TEMP-ROLLBACK';

-- ==============================================================================
-- 3. CONSULTAS Y REPORTES GERENCIALES (ENTREGABLE REPORTES SQL)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- REPORTE 1: Consolidado de Reservas, Clientes, Espacios, Sucursales y Estado de Pago
-- ------------------------------------------------------------------------------
SELECT
    r.id_reserva,
    c.identificacion AS doc_cliente,
    c.nombre_razon_social AS cliente,
    s.nombre AS sucursal,
    e.nombre AS espacio,
    te.nombre AS tipo_espacio,
    COALESCE(emp.nombre, 'Sin asignar') AS coordinador,
    r.fecha_inicio,
    r.fecha_fin,
    r.estado AS estado_reserva,
    r.costo_total,
    fn_estado_pago(r.id_reserva) AS estado_pago
FROM reservas r
JOIN clientes c ON r.id_cliente = c.id_cliente
JOIN espacios e ON r.id_espacio = e.id_espacio
JOIN sucursales s ON e.id_sucursal = s.id_sucursal
JOIN tipos_espacio te ON e.id_tipo_espacio = te.id_tipo_espacio
LEFT JOIN empleados emp ON r.id_empleado = emp.id_empleado
ORDER BY r.fecha_inicio DESC;

-- ------------------------------------------------------------------------------
-- REPORTE 2: Ingresos Facturados vs. Recaudados por Sucursal y Espacio
-- ------------------------------------------------------------------------------
SELECT
    s.nombre AS sucursal,
    e.nombre AS espacio,
    COUNT(DISTINCT r.id_reserva) AS total_reservas,
    COALESCE(SUM(DISTINCT f.total), 0.00) AS total_facturado,
    COALESCE(SUM(p.monto), 0.00) AS total_recaudado,
    COALESCE(SUM(DISTINCT f.total), 0.00) - COALESCE(SUM(p.monto), 0.00) AS saldo_por_cobrar
FROM sucursales s
JOIN espacios e ON e.id_sucursal = s.id_sucursal
LEFT JOIN reservas r ON r.id_espacio = e.id_espacio AND r.estado <> 'cancelada'
LEFT JOIN facturas f ON f.id_reserva = r.id_reserva
LEFT JOIN pagos p ON p.id_factura = f.id_factura
GROUP BY s.id_sucursal, s.nombre, e.id_espacio, e.nombre
ORDER BY total_recaudado DESC;

-- ------------------------------------------------------------------------------
-- REPORTE 3: Ranking de Servicios Adicionales Más Contratados (Relación N:M)
-- ------------------------------------------------------------------------------
SELECT
    sa.id_servicio,
    sa.nombre AS servicio_adicional,
    sa.precio_unitario AS precio_catalogo,
    COUNT(rs.id_reserva_servicio) AS veces_contratado,
    COALESCE(SUM(rs.cantidad), 0) AS unidades_totales,
    COALESCE(SUM(rs.subtotal), 0.00) AS ingresos_generados
FROM servicios_adicionales sa
LEFT JOIN reserva_servicios rs ON sa.id_servicio = rs.id_servicio
LEFT JOIN reservas r ON rs.id_reserva = r.id_reserva AND r.estado <> 'cancelada'
GROUP BY sa.id_servicio, sa.nombre, sa.precio_unitario
ORDER BY ingresos_generados DESC, unidades_totales DESC;

-- ------------------------------------------------------------------------------
-- REPORTE 4: Productividad de Empleados Coordinadores
-- ------------------------------------------------------------------------------
SELECT
    emp.id_empleado,
    emp.nombre AS coordinador,
    emp.cargo,
    COUNT(r.id_reserva) AS reservas_asignadas,
    COUNT(r.id_reserva) FILTER (WHERE r.estado = 'confirmada') AS reservas_confirmadas,
    COALESCE(SUM(r.costo_total) FILTER (WHERE r.estado <> 'cancelada'), 0.00) AS valor_gestionado
FROM empleados emp
LEFT JOIN reservas r ON emp.id_empleado = r.id_empleado
GROUP BY emp.id_empleado, emp.nombre, emp.cargo
ORDER BY valor_gestionado DESC;

