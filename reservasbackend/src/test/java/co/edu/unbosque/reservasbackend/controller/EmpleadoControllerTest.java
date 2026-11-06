package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.sql.entity.Empleado;
import co.edu.unbosque.reservasbackend.sql.service.EmpleadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmpleadoControllerTest {

    private MockMvc mockMvc;
    private EmpleadoService empleadoService;

    @BeforeEach
    public void setUp() {
        empleadoService = Mockito.mock(EmpleadoService.class);
        EmpleadoController controller = new EmpleadoController(empleadoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testAutenticarEmpleado_Exitoso_Returns200() throws Exception {
        Empleado emp = new Empleado();
        emp.setIdEmpleado(1);
        emp.setNombre("Laura Valentina Gómez");
        emp.setCargo("Coordinadora Senior de Eventos");
        emp.setEmail("laura.gomez@reservasbosque.edu.co");

        Mockito.when(empleadoService.autenticarEmpleado("laura.gomez@reservasbosque.edu.co", "claveSegura"))
                .thenReturn(emp);

        String json = """
            {
                "email": "laura.gomez@reservasbosque.edu.co",
                "clave": "claveSegura"
            }
        """;

        mockMvc.perform(post("/api/empleados/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEmpleado").value(1))
                .andExpect(jsonPath("$.email").value("laura.gomez@reservasbosque.edu.co"));
    }

    @Test
    public void testAutenticarEmpleado_CredencialesInvalidas_Returns401() throws Exception {
        Mockito.when(empleadoService.autenticarEmpleado("laura.gomez@reservasbosque.edu.co", "incorrecta"))
                .thenThrow(new IllegalArgumentException("Acceso denegado: correo corporativo o clave no válidos."));

        String json = """
            {
                "email": "laura.gomez@reservasbosque.edu.co",
                "clave": "incorrecta"
            }
        """;

        mockMvc.perform(post("/api/empleados/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado: correo corporativo o clave no válidos."));
    }
}

