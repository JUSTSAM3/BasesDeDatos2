package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.dto.PagoResponseDTO;
import co.edu.unbosque.reservasbackend.exceptions.GlobalExceptionHandler;
import co.edu.unbosque.reservasbackend.sql.service.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PagoControllerTest {

    private MockMvc mockMvc;
    private PagoService pagoService;

    @BeforeEach
    public void setUp() {
        pagoService = Mockito.mock(PagoService.class);
        PagoController controller = new PagoController(pagoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private DataAccessException createDataAccessException(String sqlState, String message) {
        PSQLException psqlEx = Mockito.mock(PSQLException.class);
        Mockito.when(psqlEx.getSQLState()).thenReturn(sqlState);

        ServerErrorMessage sem = Mockito.mock(ServerErrorMessage.class);
        Mockito.when(sem.getMessage()).thenReturn(message);

        Mockito.when(psqlEx.getServerErrorMessage()).thenReturn(sem);

        return new DataAccessException("Mock exception", psqlEx) {};
    }

    @Test
    public void testRegistrarPagoExitoso_Returns201() throws Exception {
        PagoResponseDTO response = new PagoResponseDTO();
        response.setIdPago(10);
        response.setIdReserva(1);
        response.setIdFactura(5);
        response.setNumeroFactura("FAC-2026-00001");
        response.setMonto(new BigDecimal("100.00"));
        response.setTipoPago("anticipo");
        response.setMetodoPago("transferencia");
        response.setFechaPago(LocalDateTime.now());
        response.setEstadoReserva("confirmada");
        response.setEstadoPago("parcial");

        Mockito.when(pagoService.registrarPago(any())).thenReturn(response);

        String json = """
            {
                "idReserva": 1,
                "tipoPago": "anticipo",
                "monto": 100.00,
                "metodoPago": "transferencia"
            }
        """;

        mockMvc.perform(post("/api/pagos/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPago").value(10))
                .andExpect(jsonPath("$.numeroFactura").value("FAC-2026-00001"))
                .andExpect(jsonPath("$.estadoReserva").value("confirmada"))
                .andExpect(jsonPath("$.estadoPago").value("parcial"));
    }

    @Test
    public void testRegistrarPagoMontoInvalido_Returns400() throws Exception {
        String json = """
            {
                "idReserva": 1,
                "tipoPago": "anticipo",
                "monto": -10.00,
                "metodoPago": "transferencia"
            }
        """;

        mockMvc.perform(post("/api/pagos/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("monto")));
    }

    @Test
    public void testConsultarEstadoPago_Returns200() throws Exception {
        Mockito.when(pagoService.consultarEstadoPago(1)).thenReturn("pagado");

        mockMvc.perform(get("/api/pagos/estado/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva").value(1))
                .andExpect(jsonPath("$.estadoPago").value("pagado"));
    }

    @Test
    public void testPagoReservaNoExiste_Returns422() throws Exception {
        DataAccessException ex = createDataAccessException("P0001", "La reserva 999 no existe");
        Mockito.when(pagoService.registrarPago(any())).thenThrow(ex);

        String json = """
            {
                "idReserva": 999,
                "tipoPago": "anticipo",
                "monto": 50.00,
                "metodoPago": "transferencia"
            }
        """;

        mockMvc.perform(post("/api/pagos/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("La reserva 999 no existe"));
    }
}
