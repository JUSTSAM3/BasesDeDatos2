package co.edu.unbosque.reservasbackend.service;

import co.edu.unbosque.reservasbackend.nosql.service.EventoNoSqlService;
import co.edu.unbosque.reservasbackend.sql.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReservaServiceTest {

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;
    private EventoNoSqlService eventoNoSqlService;
    private ReservaService reservaService;

    @BeforeEach
    public void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        eventoNoSqlService = mock(EventoNoSqlService.class);
        reservaService = new ReservaService(jdbcTemplate, objectMapper, eventoNoSqlService);
    }

    @Test
    public void testCancelarReserva_SincronizaConMongoExitosamente() {
        when(jdbcTemplate.update(anyString(), eq(10))).thenReturn(1);
        when(eventoNoSqlService.sincronizarCancelacionReserva(10)).thenReturn(true);

        Map<String, Object> result = reservaService.cancelarReserva(10);

        verify(jdbcTemplate, times(1)).update("CALL SP_CANCELAR_RESERVA(?)", 10);
        verify(eventoNoSqlService, times(1)).sincronizarCancelacionReserva(10);

        assertEquals(10, result.get("idReserva"));
        assertEquals("cancelada", result.get("estado"));
        assertEquals(true, result.get("sincronizadoMongo"));
    }

    @Test
    public void testCancelarReserva_SinDocumentoEnMongo() {
        when(jdbcTemplate.update(anyString(), eq(20))).thenReturn(1);
        when(eventoNoSqlService.sincronizarCancelacionReserva(20)).thenReturn(false);

        Map<String, Object> result = reservaService.cancelarReserva(20);

        verify(jdbcTemplate, times(1)).update("CALL SP_CANCELAR_RESERVA(?)", 20);
        verify(eventoNoSqlService, times(1)).sincronizarCancelacionReserva(20);

        assertEquals(20, result.get("idReserva"));
        assertEquals("cancelada", result.get("estado"));
        assertEquals(false, result.get("sincronizadoMongo"));
    }

    @Test
    public void testCancelarReserva_ErrorMongoNoRompeTransaccionPostgres() {
        when(jdbcTemplate.update(anyString(), eq(30))).thenReturn(1);
        when(eventoNoSqlService.sincronizarCancelacionReserva(30))
                .thenThrow(new RuntimeException("MongoDB connection timeout"));

        Map<String, Object> result = reservaService.cancelarReserva(30);

        verify(jdbcTemplate, times(1)).update("CALL SP_CANCELAR_RESERVA(?)", 30);
        assertEquals(30, result.get("idReserva"));
        assertEquals("cancelada", result.get("estado"));
        assertEquals(false, result.get("sincronizadoMongo"));
    }
}

