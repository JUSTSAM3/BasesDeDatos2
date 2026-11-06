package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.sql.entity.Empleado;
import co.edu.unbosque.reservasbackend.sql.service.EmpleadoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @PostMapping("/crear")
    public ResponseEntity<Empleado> crearEmpleado(@RequestBody Empleado empleado) {
        return ResponseEntity.ok(empleadoService.crearEmpleado(empleado));
    }

    @GetMapping("/listar")
    public ResponseEntity<List<Empleado>> listarEmpleados() {
        return ResponseEntity.ok(empleadoService.obtenerTodos());
    }

    @PostMapping("/login")
    public ResponseEntity<?> autenticarEmpleado(@RequestBody Map<String, String> credenciales) {
        String email = credenciales != null ? credenciales.get("email") : null;
        String clave = credenciales != null ? credenciales.get("clave") : null;
        try {
            Empleado empleado = empleadoService.autenticarEmpleado(email, clave);
            return ResponseEntity.ok(empleado);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", ex.getMessage()));
        }
    }
}
