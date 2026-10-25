package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.sql.entity.Espacio;
import co.edu.unbosque.reservasbackend.sql.entity.ServicioAdicional;
import co.edu.unbosque.reservasbackend.sql.entity.Sucursal;
import co.edu.unbosque.reservasbackend.sql.entity.TipoEspacio;
import co.edu.unbosque.reservasbackend.sql.repository.ServicioAdicionalRepository;
import co.edu.unbosque.reservasbackend.sql.repository.TipoEspacioRepository;
import co.edu.unbosque.reservasbackend.sql.service.EspacioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
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
    private TipoEspacioRepository tipoEspacioRepository;
    private ServicioAdicionalRepository servicioAdicionalRepository;

    @BeforeEach
    public void setUp() {
        espacioService = Mockito.mock(EspacioService.class);
        tipoEspacioRepository = Mockito.mock(TipoEspacioRepository.class);
        servicioAdicionalRepository = Mockito.mock(ServicioAdicionalRepository.class);

        EspacioController controller = new EspacioController();
        ReflectionTestUtils.setField(controller, "espacioService", espacioService);
        ReflectionTestUtils.setField(controller, "tipoEspacioRepository", tipoEspacioRepository);
        ReflectionTestUtils.setField(controller, "servicioAdicionalRepository", servicioAdicionalRepository);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testListarEspaciosSinRecursionJson_Returns200() throws Exception {
        Sucursal sucursal = new Sucursal("Sede Norte", "Calle 100", "Bogotá", "3001234567");
        sucursal.setIdSucursal(1);

        TipoEspacio tipo = new TipoEspacio();
        tipo.setIdTipoEspacio(1);
        tipo.setNombre("Eventos sociales");

        Espacio espacio = new Espacio();
        espacio.setIdEspacio(10);
        espacio.setNombre("Salón Aura");
        espacio.setCapacidad(120);
        espacio.setTarifaPorHora(new BigDecimal("250000.00"));
        espacio.setEstado("disponible");
        espacio.setSucursal(sucursal);
        espacio.setTipoEspacio(tipo);
        tipo.setEspacios(List.of(espacio)); // Referencia circular intencional para validar @JsonIgnore

        Mockito.when(espacioService.obtenerTodos()).thenReturn(List.of(espacio));

        mockMvc.perform(get("/api/espacios/listar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idEspacio").value(10))
                .andExpect(jsonPath("$[0].nombre").value("Salón Aura"))
                .andExpect(jsonPath("$[0].tipoEspacio.nombre").value("Eventos sociales"))
                .andExpect(jsonPath("$[0].sucursal.nombre").value("Sede Norte"));
    }

    @Test
    public void testCrearEspacio_Returns200() throws Exception {
        Espacio espacio = new Espacio();
        espacio.setIdEspacio(11);
        espacio.setNombre("Terraza Nómada");
        espacio.setCapacidad(80);
        espacio.setTarifaPorHora(new BigDecimal("190000.00"));

        Mockito.when(espacioService.crearEspacio(any())).thenReturn(espacio);

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEspacio").value(11))
                .andExpect(jsonPath("$.nombre").value("Terraza Nómada"));
    }

    @Test
    public void testListarServiciosAdicionales_Returns200() throws Exception {
        ServicioAdicional s = new ServicioAdicional();
        s.setIdServicio(1);
        s.setNombre("Catering");
        s.setPrecioUnitario(new BigDecimal("45000.00"));

        Mockito.when(servicioAdicionalRepository.findAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/espacios/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idServicio").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Catering"));
    }

    @Test
    public void testListarTiposEspacio_Returns200() throws Exception {
        TipoEspacio t = new TipoEspacio();
        t.setIdTipoEspacio(1);
        t.setNombre("Conferencia");

        Mockito.when(tipoEspacioRepository.findAll()).thenReturn(List.of(t));

        mockMvc.perform(get("/api/espacios/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTipoEspacio").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Conferencia"));
    }
}
