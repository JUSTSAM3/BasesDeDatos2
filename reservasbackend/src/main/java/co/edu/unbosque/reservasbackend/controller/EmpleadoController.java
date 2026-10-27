package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.sql.entity.Empleado;
import co.edu.unbosque.reservasbackend.sql.service.EmpleadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
