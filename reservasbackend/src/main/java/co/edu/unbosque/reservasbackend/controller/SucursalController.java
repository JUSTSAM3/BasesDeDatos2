package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.sql.entity.Sucursal;
import co.edu.unbosque.reservasbackend.sql.service.SucursalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
@CrossOrigin(origins = "*")
public class SucursalController {

    @Autowired
    private SucursalService sucursalService;

    @PostMapping("/crear")
    public ResponseEntity<Sucursal> crearSucursal(@RequestBody Sucursal sucursal) {
        return ResponseEntity.ok(sucursalService.crearSucursal(sucursal));
    }

    @GetMapping("/listar")
    public ResponseEntity<List<Sucursal>> listarSucursales() {
        return ResponseEntity.ok(sucursalService.obtenerTodas());
    }
}
