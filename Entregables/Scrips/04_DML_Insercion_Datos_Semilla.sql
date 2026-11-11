-- ==============================================================================
-- UNIVERSIDAD EL BOSQUE - BASES DE DATOS 2
-- SISTEMA DE GESTIÓN DE RESERVAS DE ESPACIOS PARA EVENTOS
-- SCRIPT 04: INSERCIÓN DE DATOS SEMILLA (DML IDEMPOTENTE)
-- ==============================================================================
-- Contenido:
--   1. Catálogo de tipos de espacio (3FN)
--   2. Sucursales de operación
--   3. Espacios reservables por sede
--   4. Catálogo de servicios adicionales
--   5. Empleados coordinadores
--   6. Clientes iniciales (personas naturales y jurídicas)
--   7. Reservas transaccionales de ejemplo con servicios, facturas y pagos
-- ==============================================================================

-- ==============================================================================
-- 1. TIPOS DE ESPACIO
-- ==============================================================================
INSERT INTO tipos_espacio (nombre, descripcion)
SELECT 'General', 'Tipo de espacio estándar para reuniones y actividades generales.'
WHERE NOT EXISTS (SELECT 1 FROM tipos_espacio WHERE nombre = 'General');

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

-- ==============================================================================
-- 2. SUCURSALES
-- ==============================================================================
INSERT INTO sucursales (nombre, direccion, ciudad, telefono)
SELECT 'Sede Usaquén Norte', 'Carrera 7 # 119-45', 'Bogotá', '6017428000'
WHERE NOT EXISTS (SELECT 1 FROM sucursales WHERE nombre = 'Sede Usaquén Norte');

INSERT INTO sucursales (nombre, direccion, ciudad, telefono)
SELECT 'Sede Parque de la 93', 'Calle 93A # 13-24', 'Bogotá', '6017428001'
WHERE NOT EXISTS (SELECT 1 FROM sucursales WHERE nombre = 'Sede Parque de la 93');

INSERT INTO sucursales (nombre, direccion, ciudad, telefono)
SELECT 'Sede Chapinero Cultural', 'Calle 67 # 8-32', 'Bogotá', '6017428002'
WHERE NOT EXISTS (SELECT 1 FROM sucursales WHERE nombre = 'Sede Chapinero Cultural');

-- ==============================================================================
-- 3. ESPACIOS
-- ==============================================================================
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

-- ==============================================================================
-- 4. SERVICIOS ADICIONALES
-- ==============================================================================
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

-- ==============================================================================
-- 5. EMPLEADOS COORDINADORES
-- ==============================================================================
INSERT INTO empleados (nombre, cargo, email)
SELECT 'Laura Valentina Gómez', 'Coordinadora Senior de Eventos', 'laura.gomez@reservasbosque.edu.co'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email = 'laura.gomez@reservasbosque.edu.co');

INSERT INTO empleados (nombre, cargo, email)
SELECT 'Carlos Andrés Mendoza', 'Coordinador Logístico y Corporativo', 'carlos.mendoza@reservasbosque.edu.co'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email = 'carlos.mendoza@reservasbosque.edu.co');

INSERT INTO empleados (nombre, cargo, email)
SELECT 'Diana Carolina Ríos', 'Especialista de Bodas y Protocolo', 'diana.rios@reservasbosque.edu.co'
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE email = 'diana.rios@reservasbosque.edu.co');

-- ==============================================================================
-- 6. CLIENTES
-- ==============================================================================
INSERT INTO clientes (tipo_cliente, identificacion, nombre_razon_social, email, telefono)
SELECT 'natural', '1020754321', 'Alejandro Rodríguez Pérez', 'alejandro.rodriguez@gmail.com', '3154892031'
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE identificacion = '1020754321');

INSERT INTO clientes (tipo_cliente, identificacion, nombre_razon_social, email, telefono)
SELECT 'juridico', '900456789-1', 'TechNova Soluciones S.A.S.', 'eventos@technova.co', '3108529630'
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE identificacion = '900456789-1');

INSERT INTO clientes (tipo_cliente, identificacion, nombre_razon_social, email, telefono)
SELECT 'natural', '52987654', 'María Fernanda Torres', 'mafe.torres@outlook.com', '3207418529'
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE identificacion = '52987654');

-- ==============================================================================
-- 7. TRANSACCIONES SEMILLA (RESERVAS, SERVICIOS, FACTURAS Y PAGOS)
-- ==============================================================================
DO $$
DECLARE
    v_cli1 INT; v_cli2 INT; v_cli3 INT;
    v_esp1 INT; v_esp2 INT; v_esp3 INT; v_esp4 INT;
    v_emp1 INT; v_emp2 INT; v_emp3 INT;
    v_serv_cat INT; v_serv_son INT; v_serv_dec INT; v_serv_str INT;
    v_res1 INT; v_res2 INT; v_res3 INT; v_res4 INT;
    v_pago INT;
BEGIN
    SELECT id_cliente INTO v_cli1 FROM clientes WHERE identificacion = '1020754321';
    SELECT id_cliente INTO v_cli2 FROM clientes WHERE identificacion = '900456789-1';
    SELECT id_cliente INTO v_cli3 FROM clientes WHERE identificacion = '52987654';

    SELECT id_espacio INTO v_esp1 FROM espacios WHERE nombre = 'Salón Imperial Aura';
    SELECT id_espacio INTO v_esp2 FROM espacios WHERE nombre = 'Terraza Nómada 93';
    SELECT id_espacio INTO v_esp3 FROM espacios WHERE nombre = 'Auditorio Magno El Bosque';
    SELECT id_espacio INTO v_esp4 FROM espacios WHERE nombre = 'Studio Ejecutivo Cumbre';

    SELECT id_empleado INTO v_emp1 FROM empleados WHERE email = 'laura.gomez@reservasbosque.edu.co';
    SELECT id_empleado INTO v_emp2 FROM empleados WHERE email = 'carlos.mendoza@reservasbosque.edu.co';
    SELECT id_empleado INTO v_emp3 FROM empleados WHERE email = 'diana.rios@reservasbosque.edu.co';

    SELECT id_servicio INTO v_serv_cat FROM servicios_adicionales WHERE nombre LIKE 'Catering%';
    SELECT id_servicio INTO v_serv_son FROM servicios_adicionales WHERE nombre LIKE 'Sonido%';
    SELECT id_servicio INTO v_serv_dec FROM servicios_adicionales WHERE nombre LIKE 'Decoración%';
    SELECT id_servicio INTO v_serv_str FROM servicios_adicionales WHERE nombre LIKE 'Streaming%';

    -- 7.1 Reserva 1: Evento Social Confirmado (con anticipo y saldo)
    IF NOT EXISTS (SELECT 1 FROM reservas WHERE id_cliente = v_cli1 AND id_espacio = v_esp1) THEN
        CALL SP_CREAR_RESERVA(
            v_cli1, v_esp1, v_emp1,
            (CURRENT_DATE + INTERVAL '10 days' + TIME '14:00')::TIMESTAMP,
            (CURRENT_DATE + INTERVAL '10 days' + TIME '20:00')::TIMESTAMP,
            jsonb_build_array(
                jsonb_build_object('id_servicio', v_serv_cat, 'cantidad', 50),
                jsonb_build_object('id_servicio', v_serv_son, 'cantidad', 1)
            ),
            v_res1
        );
        -- Pago anticipo (50%)
        CALL SP_REGISTRAR_PAGO(v_res1, 'anticipo', 2500000.00, 'transferencia', v_pago);
    END IF;

    -- 7.2 Reserva 2: Congreso Corporativo TechNova (Totalmente pagado)
    IF NOT EXISTS (SELECT 1 FROM reservas WHERE id_cliente = v_cli2 AND id_espacio = v_esp3) THEN
        CALL SP_CREAR_RESERVA(
            v_cli2, v_esp3, v_emp2,
            (CURRENT_DATE + INTERVAL '20 days' + TIME '08:00')::TIMESTAMP,
            (CURRENT_DATE + INTERVAL '20 days' + TIME '16:00')::TIMESTAMP,
            jsonb_build_array(
                jsonb_build_object('id_servicio', v_serv_son, 'cantidad', 1),
                jsonb_build_object('id_servicio', v_serv_str, 'cantidad', 1)
            ),
            v_res2
        );
        -- Pago total
        CALL SP_REGISTRAR_PAGO(v_res2, 'total', (SELECT costo_total FROM reservas WHERE id_reserva = v_res2), 'transferencia', v_pago);
    END IF;

    -- 7.3 Reserva 3: Evento en Terraza (Pendiente de pago)
    IF NOT EXISTS (SELECT 1 FROM reservas WHERE id_cliente = v_cli3 AND id_espacio = v_esp2) THEN
        CALL SP_CREAR_RESERVA(
            v_cli3, v_esp2, v_emp3,
            (CURRENT_DATE + INTERVAL '25 days' + TIME '17:00')::TIMESTAMP,
            (CURRENT_DATE + INTERVAL '25 days' + TIME '22:00')::TIMESTAMP,
            jsonb_build_array(
                jsonb_build_object('id_servicio', v_serv_dec, 'cantidad', 1)
            ),
            v_res3
        );
    END IF;

    -- 7.4 Reserva 4: Junta Directiva en Sala VIP
    IF NOT EXISTS (SELECT 1 FROM reservas WHERE id_cliente = v_cli2 AND id_espacio = v_esp4) THEN
        CALL SP_CREAR_RESERVA(
            v_cli2, v_esp4, v_emp2,
            (CURRENT_DATE + INTERVAL '5 days' + TIME '09:00')::TIMESTAMP,
            (CURRENT_DATE + INTERVAL '5 days' + TIME '13:00')::TIMESTAMP,
            NULL,
            v_res4
        );
        CALL SP_REGISTRAR_PAGO(v_res4, 'anticipo', 300000.00, 'tarjeta_credito', v_pago);
    END IF;
END $$;

