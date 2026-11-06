package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.exceptions.GlobalExceptionHandler;
import co.edu.unbosque.reservasbackend.nosql.document.DetalleEvento;
import co.edu.unbosque.reservasbackend.nosql.document.Resena;
import co.edu.unbosque.reservasbackend.nosql.service.EventoNoSqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EventoNoSqlControllerTest {

    private MockMvc mockMvc;
    private EventoNoSqlService eventoNoSqlService;

    @BeforeEach
    public void setUp() {
        eventoNoSqlService = Mockito.mock(EventoNoSqlService.class);
        EventoNoSqlController controller = new EventoNoSqlController(eventoNoSqlService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testGuardarDetalleEvento_Returns201() throws Exception {
        DetalleEvento doc = new DetalleEvento();
        doc.setId("mongo-id-1");
        doc.setIdReserva(101);
        doc.setTipoEvento("boda");
        doc.setNombreEvento("Boda Rodríguez - Torres");

        Mockito.when(eventoNoSqlService.guardarOActualizarDetalleEvento(any())).thenReturn(doc);

        String json = """
            {
                "idReserva": 101,
                "idCliente": 1,
                "tipoEvento": "boda",
                "nombreEvento": "Boda Rodríguez - Torres",
                "numInvitadosEstimado": 150
            }
        """;

        mockMvc.perform(post("/api/nosql/detalles-evento")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idReserva").value(101))
                .andExpect(jsonPath("$.tipoEvento").value("boda"));
    }

    @Test
    public void testObtenerDetallePorReserva_Returns200() throws Exception {
        DetalleEvento doc = new DetalleEvento();
        doc.setIdReserva(101);
        doc.setNombreEvento("Boda Rodríguez - Torres");

        Mockito.when(eventoNoSqlService.obtenerDetallePorReserva(101)).thenReturn(Optional.of(doc));

        mockMvc.perform(get("/api/nosql/detalles-evento/reserva/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva").value(101))
                .andExpect(jsonPath("$.nombreEvento").value("Boda Rodríguez - Torres"));
    }

    @Test
    public void testGuardarResena_Returns201() throws Exception {
        Resena resena = new Resena();
        resena.setIdReserva(101);
        resena.setCalificacionGeneral(5);
        resena.setComentario("Excelente atención");

        Mockito.when(eventoNoSqlService.guardarOActualizarResena(any())).thenReturn(resena);

        String json = """
            {
                "idReserva": 101,
                "idCliente": 1,
                "idEspacio": 1,
                "calificacionGeneral": 5,
                "comentario": "Excelente atención"
            }
        """;

        mockMvc.perform(post("/api/nosql/resenas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idReserva").value(101))
                .andExpect(jsonPath("$.calificacionGeneral").value(5));
    }

    @Test
    public void testReporteServiciosMasSolicitados_Returns200() throws Exception {
        Mockito.when(eventoNoSqlService.reporteServiciosMasSolicitados())
                .thenReturn(List.of(Map.of(
                        "idServicio", 1,
                        "nombreServicio", "Catering Gourmet",
                        "eventosQueLoSolicitaron", 2,
                        "unidadesTotalesSolicitadas", 370
                )));

        mockMvc.perform(get("/api/nosql/reportes/servicios-mas-solicitados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreServicio").value("Catering Gourmet"))
                .andExpect(jsonPath("$[0].unidadesTotalesSolicitadas").value(370));
    }

    @Test
    public void testEliminarResenaPorReserva_Exitoso() throws Exception {
        Mockito.when(eventoNoSqlService.eliminarResenaPorReserva(101)).thenReturn(true);

        mockMvc.perform(delete("/api/nosql/resenas/reserva/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva").value(101))
                .andExpect(jsonPath("$.eliminado").value(true));
    }

    @Test
    public void testEliminarResenaPorReserva_NoEncontrada() throws Exception {
        Mockito.when(eventoNoSqlService.eliminarResenaPorReserva(999)).thenReturn(false);

        mockMvc.perform(delete("/api/nosql/resenas/reserva/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBusquedaAvanzadaEventos_Returns200() throws Exception {
        DetalleEvento ev = new DetalleEvento();
        ev.setIdReserva(102);
        ev.setTipoEvento("conferencia");
        ev.setNombreEvento("Summit IA");

        Mockito.when(eventoNoSqlService.consultarEventosAvanzado(any(), any(), any(), any(), any()))
                .thenReturn(List.of(ev));

        mockMvc.perform(get("/api/nosql/detalles-evento/busqueda-avanzada")
                .param("tiposEvento", "conferencia", "corporativo")
                .param("minInvitados", "100")
                .param("estadoReserva", "confirmada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idReserva").value(102))
                .andExpect(jsonPath("$[0].tipoEvento").value("conferencia"));
    }
}

