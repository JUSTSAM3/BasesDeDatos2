package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.dto.PagoRequestDTO;
import co.edu.unbosque.reservasbackend.dto.PagoResponseDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PagoService {

    private final JdbcTemplate jdbcTemplate;

    public PagoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Registra un pago invocando el procedimiento almacenado SP_REGISTRAR_PAGO.
     * Si el acumulado cubre >= 30% del costo total, la reserva pasa a estado 'confirmada' automáticamente.
     */
    @Transactional
    public PagoResponseDTO registrarPago(PagoRequestDTO dto) {
        String callSql = "CALL SP_REGISTRAR_PAGO(?, ?, ?, ?, NULL)";

        Integer idPago = jdbcTemplate.queryForObject(
                callSql,
                Integer.class,
                dto.getIdReserva(),
                dto.getTipoPago(),
                dto.getMonto(),
                dto.getMetodoPago()
        );

        String infoSql = """
            SELECT p.id_pago, p.tipo_pago, p.monto, p.fecha_pago, p.metodo_pago,
                   f.id_factura, f.numero_factura,
                   r.id_reserva, r.estado AS estado_reserva,
                   fn_estado_pago(r.id_reserva) AS estado_pago
            FROM pagos p
            JOIN facturas f ON p.id_factura = f.id_factura
            JOIN reservas r ON f.id_reserva = r.id_reserva
            WHERE p.id_pago = ?
        """;

        Map<String, Object> row = jdbcTemplate.queryForMap(infoSql, idPago);

        PagoResponseDTO response = new PagoResponseDTO();
        response.setIdPago(idPago);
        response.setIdReserva((Integer) row.get("id_reserva"));
        response.setIdFactura((Integer) row.get("id_factura"));
        response.setNumeroFactura((String) row.get("numero_factura"));

        Object montoObj = row.get("monto");
        if (montoObj instanceof Number) {
            response.setMonto(new BigDecimal(montoObj.toString()));
        }

        response.setTipoPago((String) row.get("tipo_pago"));
        response.setMetodoPago((String) row.get("metodo_pago"));

        Object fechaObj = row.get("fecha_pago");
        if (fechaObj instanceof Timestamp ts) {
            response.setFechaPago(ts.toLocalDateTime());
        } else if (fechaObj instanceof LocalDateTime ldt) {
            response.setFechaPago(ldt);
        }

        response.setEstadoReserva((String) row.get("estado_reserva"));
        response.setEstadoPago((String) row.get("estado_pago"));

        return response;
    }

    /**
     * Consulta el estado de pago de una reserva ('sin_factura', 'pendiente', 'parcial', 'pagado')
     * mediante la función PL/pgSQL fn_estado_pago.
     */
    public String consultarEstadoPago(Integer idReserva) {
        String sql = "SELECT fn_estado_pago(?)";
        return jdbcTemplate.queryForObject(sql, String.class, idReserva);
    }
}
