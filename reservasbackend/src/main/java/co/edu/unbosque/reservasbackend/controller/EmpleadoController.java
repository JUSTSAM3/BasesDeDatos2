package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.sql.entity.Empleado;
import co.edu.unbosque.reservasbackend.sql.service.EmpleadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empleados")
@CrossOrigin(origins = "*")
public class EmpleadoController {

    @Autowired
    private EmpleadoService empleadoService;

    @PostMapping("/crear")
    public ResponseEntity<Empleado> crearEmpleado(@RequestBody Empleado empleado) {
        return ResponseEntity.ok(empleadoService.crearEmpleado(empleado));
    }

    @GetMapping("/listar")
    public ResponseEntity<List<Empleado>> listarEmpleados() {
        return ResponseEntity.ok(empleadoService.obtenerTodos());
    }
}
