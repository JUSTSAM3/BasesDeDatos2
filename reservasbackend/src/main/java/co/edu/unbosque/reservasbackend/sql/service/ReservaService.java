package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.dto.DisponibilidadResponseDTO;
import co.edu.unbosque.reservasbackend.dto.ReservaRequestDTO;
import co.edu.unbosque.reservasbackend.dto.ReservaResponseDTO;
import tools.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReservaService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ReservaService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * RF-07 / RF-11 / RF-12: Crea una reserva invocando el procedimiento almacenado SP_CREAR_RESERVA.
     * Pasa los servicios adicionales como JSONB para inserción y cálculo atómico dentro del motor.
     */
    @Transactional
    public ReservaResponseDTO crearReserva(ReservaRequestDTO dto) {
        String jsonServicios = "[]";
        if (dto.getServicios() != null && !dto.getServicios().isEmpty()) {
            try {
                List<Map<String, Object>> mappedServicios = new ArrayList<>();
                for (ReservaRequestDTO.ServicioAdicionalDTO s : dto.getServicios()) {
                    mappedServicios.add(Map.of(
                            "id_servicio", s.getIdServicio(),
                            "cantidad", s.getCantidad()
                    ));
                }
                jsonServicios = objectMapper.writeValueAsString(mappedServicios);
            } catch (Exception e) {
                throw new RuntimeException("Error al serializar los servicios", e);
            }
        }

        // Llamar al SP usando queryForObject. Esto es preferible a CallableStatement
        // porque JdbcTemplate mapea automáticamente el retorno del SP (INOUT) en PostgreSQL.
        String sql = "CALL SP_CREAR_RESERVA(?, ?, ?, ?, ?, ?::jsonb, NULL)";

        Integer idReserva = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                dto.getIdCliente(),
                dto.getIdEspacio(),
                dto.getIdEmpleado(), // Puede ser null
                dto.getFechaInicio(),
                dto.getFechaFin(),
                jsonServicios
        );

        // Consultar el estado final, costo total y estado de pago después de los triggers
        String selectSql = "SELECT estado, costo_total, fn_estado_pago(id_reserva) AS estado_pago FROM reservas WHERE id_reserva = ?";
        Map<String, Object> result = jdbcTemplate.queryForMap(selectSql, idReserva);

        ReservaResponseDTO response = new ReservaResponseDTO();
        response.setIdReserva(idReserva);
        response.setIdCliente(dto.getIdCliente());
        response.setIdEspacio(dto.getIdEspacio());
        response.setIdEmpleado(dto.getIdEmpleado());
        response.setFechaInicio(dto.getFechaInicio());
        response.setFechaFin(dto.getFechaFin());
        response.setEstado((String) result.get("estado"));
        response.setEstadoPago((String) result.get("estado_pago"));

        Object costoObj = result.get("costo_total");
        if (costoObj instanceof Number) {
            response.setCostoTotal(new BigDecimal(costoObj.toString()));
        }

        return response;
    }

    /**
     * RF-05: Consulta disponibilidad horaria de un espacio llamando a la función fn_validar_disponibilidad.
     */
    public DisponibilidadResponseDTO validarDisponibilidad(Integer idEspacio, LocalDateTime fechaInicio, LocalDateTime fechaFin, Integer idReservaExcluir) {
        String sql = "SELECT fn_validar_disponibilidad(?, ?, ?, ?)";
        Boolean disponible = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                idEspacio,
                fechaInicio,
                fechaFin,
                idReservaExcluir
        );

        boolean isDisp = Boolean.TRUE.equals(disponible);
        String mensaje = isDisp ? "El espacio se encuentra disponible en el horario consultado."
                                : "El espacio ya tiene reservas activas en el horario solicitado.";

        return new DisponibilidadResponseDTO(idEspacio, fechaInicio, fechaFin, isDisp, mensaje);
    }

    /**
     * RF-09: Cancela una reserva invocando el procedimiento almacenado SP_CANCELAR_RESERVA.
     * Valida que no haya iniciado el evento y que no esté ya cancelada.
     */
    @Transactional
    public Map<String, Object> cancelarReserva(Integer idReserva) {
        String sql = "CALL SP_CANCELAR_RESERVA(?)";
        jdbcTemplate.update(sql, idReserva);

        return Map.of(
                "idReserva", idReserva,
                "estado", "cancelada",
                "mensaje", "Reserva cancelada exitosamente."
        );
    }

    /**
     * Consulta una reserva específica por su identificador único.
     */
    public ReservaResponseDTO obtenerPorId(Integer idReserva) {
        String sql = "SELECT id_reserva, id_cliente, id_espacio, id_empleado, fecha_reserva, fecha_inicio, fecha_fin, estado, costo_total, fn_estado_pago(id_reserva) AS estado_pago FROM reservas WHERE id_reserva = ?";
        Map<String, Object> row = jdbcTemplate.queryForMap(sql, idReserva);
        return mapRowToReservaDTO(row);
    }

    /**
     * Lista todas las reservas registradas en el sistema.
     */
    public List<ReservaResponseDTO> obtenerTodas() {
        String sql = "SELECT id_reserva, id_cliente, id_espacio, id_empleado, fecha_reserva, fecha_inicio, fecha_fin, estado, costo_total, fn_estado_pago(id_reserva) AS estado_pago FROM reservas ORDER BY id_reserva DESC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<ReservaResponseDTO> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            list.add(mapRowToReservaDTO(row));
        }
        return list;
    }

    private ReservaResponseDTO mapRowToReservaDTO(Map<String, Object> row) {
        ReservaResponseDTO dto = new ReservaResponseDTO();
        dto.setIdReserva((Integer) row.get("id_reserva"));
        dto.setIdCliente((Integer) row.get("id_cliente"));
        dto.setIdEspacio((Integer) row.get("id_espacio"));
        dto.setIdEmpleado((Integer) row.get("id_empleado"));

        Object fr = row.get("fecha_reserva");
        if (fr instanceof Timestamp ts) dto.setFechaReserva(ts.toLocalDateTime());
        else if (fr instanceof LocalDateTime ldt) dto.setFechaReserva(ldt);

        Object fi = row.get("fecha_inicio");
        if (fi instanceof Timestamp ts) dto.setFechaInicio(ts.toLocalDateTime());
        else if (fi instanceof LocalDateTime ldt) dto.setFechaInicio(ldt);

        Object ff = row.get("fecha_fin");
        if (ff instanceof Timestamp ts) dto.setFechaFin(ts.toLocalDateTime());
        else if (ff instanceof LocalDateTime ldt) dto.setFechaFin(ldt);

        dto.setEstado((String) row.get("estado"));
        dto.setEstadoPago((String) row.get("estado_pago"));

        Object c = row.get("costo_total");
        if (c instanceof Number) {
            dto.setCostoTotal(new BigDecimal(c.toString()));
        }
        return dto;
    }
}
