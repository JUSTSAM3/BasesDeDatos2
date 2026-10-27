package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.dto.EspacioResponseDTO;
import co.edu.unbosque.reservasbackend.dto.ServicioAdicionalResponseDTO;
import co.edu.unbosque.reservasbackend.dto.TipoEspacioResponseDTO;
import co.edu.unbosque.reservasbackend.sql.service.EspacioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EspacioControllerTest {

    private MockMvc mockMvc;
    private EspacioService espacioService;

    @BeforeEach
    public void setUp() {
        espacioService = Mockito.mock(EspacioService.class);
        EspacioController controller = new EspacioController(espacioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testListarEspaciosDTO_Returns200() throws Exception {
        EspacioResponseDTO dto = new EspacioResponseDTO(
                10,
                "Salón Aura",
                1,
                "Sede Norte",
                1,
                "Eventos sociales",
                120,
                new BigDecimal("250000.00"),
                "disponible"
        );

        Mockito.when(espacioService.obtenerTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/espacios/listar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idEspacio").value(10))
                .andExpect(jsonPath("$[0].nombre").value("Salón Aura"))
                .andExpect(jsonPath("$[0].nombreTipoEspacio").value("Eventos sociales"))
                .andExpect(jsonPath("$[0].nombreSucursal").value("Sede Norte"));
    }

    @Test
    public void testCrearEspacio_Returns201() throws Exception {
        EspacioResponseDTO dto = new EspacioResponseDTO(
                11,
                "Terraza Nómada",
                1,
                "Sede Norte",
                1,
                "Eventos sociales",
                80,
                new BigDecimal("190000.00"),
                "disponible"
        );

        Mockito.when(espacioService.crearEspacio(any())).thenReturn(dto);

        String json = """
                {
                  "idSucursal": 1,
                  "nombre": "Terraza Nómada",
                  "idTipoEspacio": 1,
                  "capacidad": 80,
                  "tarifaPorHora": 190000.00,
                  "estado": "disponible"
                }
                """;

        mockMvc.perform(post("/api/espacios/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEspacio").value(11))
                .andExpect(jsonPath("$.nombre").value("Terraza Nómada"));
    }

    @Test
    public void testListarServiciosAdicionales_Returns200() throws Exception {
        ServicioAdicionalResponseDTO s = new ServicioAdicionalResponseDTO(
                1,
                "Catering",
                "Servicio de alimentos",
                new BigDecimal("45000.00")
        );

        Mockito.when(espacioService.listarServicios()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/espacios/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idServicio").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Catering"));
    }

    @Test
    public void testListarTiposEspacio_Returns200() throws Exception {
        TipoEspacioResponseDTO t = new TipoEspacioResponseDTO(
                1,
                "Conferencia",
                "Espacios para conferencias"
        );

        Mockito.when(espacioService.listarTipos()).thenReturn(List.of(t));

        mockMvc.perform(get("/api/espacios/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTipoEspacio").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Conferencia"));
    }
}
