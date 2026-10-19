package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.dto.ReservaResponseDTO;
import co.edu.unbosque.reservasbackend.exceptions.GlobalExceptionHandler;
import co.edu.unbosque.reservasbackend.sql.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReservaControllerTest {

    private MockMvc mockMvc;
    private ReservaService reservaService;

    @BeforeEach
    public void setUp() {
        reservaService = Mockito.mock(ReservaService.class);
        ReservaController controller = new ReservaController(reservaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String getValidJson() {
        return "{\n" +
                "  \"idCliente\": 1,\n" +
                "  \"idEspacio\": 1,\n" +
                "  \"fechaInicio\": \"2029-10-08T10:00:00\",\n" +
                "  \"fechaFin\": \"2029-10-08T12:00:00\"\n" +
                "}";
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
    public void testCrearReservaExitosa_Returns201() throws Exception {
        ReservaResponseDTO mockResponse = new ReservaResponseDTO();
        mockResponse.setIdReserva(99);
        mockResponse.setEstado("pendiente");
        mockResponse.setCostoTotal(new java.math.BigDecimal("150.00"));
        
        Mockito.when(reservaService.crearReserva(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/reservas/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getValidJson()))
                .andExpect(status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Location", org.hamcrest.Matchers.endsWith("/99")))
                .andExpect(jsonPath("$.idReserva").value(99))
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andExpect(jsonPath("$.costoTotal").value(150.00));
    }

    @Test
    public void testPayloadInvalido_Returns400() throws Exception {
        String invalidJson = "{}"; // Vacio
        mockMvc.perform(post("/api/reservas/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Campos inválidos")));
    }

    @Test
    public void testSolapeExclude_Returns409() throws Exception {
        DataAccessException ex = createDataAccessException("23P01", "Solape detectado");
        Mockito.when(reservaService.crearReserva(any())).thenThrow(ex);

        mockMvc.perform(post("/api/reservas/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getValidJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El espacio ya está reservado en ese horario."));
    }

    @Test
    public void testSolapeP0001_Returns409() throws Exception {
        DataAccessException ex = createDataAccessException("P0001", "e_espacio_ocupado: no tiene disponibilidad");
        Mockito.when(reservaService.crearReserva(any())).thenThrow(ex);

        mockMvc.perform(post("/api/reservas/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getValidJson()))
                .andExpect(status().isConflict());
    }

    @Test
    public void testNegocioP0001_Returns422() throws Exception {
        DataAccessException ex = createDataAccessException("P0001", "e_horario_invalido: fuera de horario");
        Mockito.when(reservaService.crearReserva(any())).thenThrow(ex);

        mockMvc.perform(post("/api/reservas/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getValidJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("e_horario_invalido: fuera de horario"));
    }

    @Test
    public void testLlaveForanea_Returns422() throws Exception {
        DataAccessException ex = createDataAccessException("23503", "fk_constraint violation");
        Mockito.when(reservaService.crearReserva(any())).thenThrow(ex);

        mockMvc.perform(post("/api/reservas/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getValidJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El recurso referenciado (cliente, espacio o empleado) no existe."));
    }

    @Test
    public void testConsultarDisponibilidad_Returns200() throws Exception {
        co.edu.unbosque.reservasbackend.dto.DisponibilidadResponseDTO dispResponse =
                new co.edu.unbosque.reservasbackend.dto.DisponibilidadResponseDTO(
                        1,
                        java.time.LocalDateTime.of(2029, 10, 8, 10, 0),
                        java.time.LocalDateTime.of(2029, 10, 8, 12, 0),
                        true,
                        "El espacio se encuentra disponible en el horario consultado."
                );

        Mockito.when(reservaService.validarDisponibilidad(any(), any(), any(), any()))
                .thenReturn(dispResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/reservas/disponibilidad")
                .param("idEspacio", "1")
                .param("fechaInicio", "2029-10-08T10:00:00")
                .param("fechaFin", "2029-10-08T12:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEspacio").value(1))
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    public void testCancelarReservaExitosa_Returns200() throws Exception {
        Mockito.when(reservaService.cancelarReserva(99))
                .thenReturn(java.util.Map.of(
                        "idReserva", 99,
                        "estado", "cancelada",
                        "mensaje", "Reserva cancelada exitosamente."
                ));

        mockMvc.perform(post("/api/reservas/99/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva").value(99))
                .andExpect(jsonPath("$.estado").value("cancelada"));
    }

    @Test
    public void testCancelarReservaYaIniciada_Returns422() throws Exception {
        DataAccessException ex = createDataAccessException("P0001", "No es posible cancelar una reserva cuyo evento ya ha iniciado o finalizado");
        Mockito.when(reservaService.cancelarReserva(99)).thenThrow(ex);

        mockMvc.perform(post("/api/reservas/99/cancelar"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("No es posible cancelar una reserva cuyo evento ya ha iniciado o finalizado"));
    }

    @Test
    public void testObtenerReservaPorId_Returns200() throws Exception {
        ReservaResponseDTO mockResponse = new ReservaResponseDTO();
        mockResponse.setIdReserva(99);
        mockResponse.setEstado("confirmada");
        mockResponse.setCostoTotal(new java.math.BigDecimal("250.00"));

        Mockito.when(reservaService.obtenerPorId(99)).thenReturn(mockResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/reservas/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva").value(99))
                .andExpect(jsonPath("$.estado").value("confirmada"))
                .andExpect(jsonPath("$.costoTotal").value(250.00));
    }
}

