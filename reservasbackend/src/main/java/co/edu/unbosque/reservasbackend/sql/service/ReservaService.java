package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.dto.ReservaRequestDTO;
import co.edu.unbosque.reservasbackend.dto.ReservaResponseDTO;


import tools.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        // porque JdbcTemplate mapea automáticamente el retorno del SP (INOUT) en PostgreSQL
        // a un ResultSet de una fila y una columna, permitiendo extraer el Integer directamente.
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

        // Consultar el estado final y el costo total despues del trigger
        String selectSql = "SELECT estado, costo_total FROM reservas WHERE id_reserva = ?";
        Map<String, Object> result = jdbcTemplate.queryForMap(selectSql, idReserva);

        ReservaResponseDTO response = new ReservaResponseDTO();
        response.setIdReserva(idReserva);
        response.setEstado((String) result.get("estado"));
        
        Object costoObj = result.get("costo_total");
        if (costoObj instanceof Number) {
            response.setCostoTotal(new BigDecimal(costoObj.toString()));
        } else if (costoObj != null) {
            response.setCostoTotal(new BigDecimal(costoObj.toString()));
        }

        return response;
    }
}
