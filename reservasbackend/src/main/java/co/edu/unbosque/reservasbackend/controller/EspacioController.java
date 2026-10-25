package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.dto.EspacioRequestDTO;
import co.edu.unbosque.reservasbackend.sql.entity.Espacio;
import co.edu.unbosque.reservasbackend.sql.entity.ServicioAdicional;
import co.edu.unbosque.reservasbackend.sql.entity.TipoEspacio;
import co.edu.unbosque.reservasbackend.sql.repository.ServicioAdicionalRepository;
import co.edu.unbosque.reservasbackend.sql.repository.TipoEspacioRepository;
import co.edu.unbosque.reservasbackend.sql.service.EspacioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
@CrossOrigin(origins = "*")
public class EspacioController {

    @Autowired
    private EspacioService espacioService;

    @Autowired
    private TipoEspacioRepository tipoEspacioRepository;

    @Autowired
    private ServicioAdicionalRepository servicioAdicionalRepository;

    @PostMapping("/crear")
    public ResponseEntity<Espacio> crearEspacio(@RequestBody EspacioRequestDTO dto) {
        return ResponseEntity.ok(espacioService.crearEspacio(dto));
    }

    @GetMapping("/listar")
    public ResponseEntity<List<Espacio>> listarEspacios() {
        return ResponseEntity.ok(espacioService.obtenerTodos());
    }

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoEspacio>> listarTiposEspacio() {
        return ResponseEntity.ok(tipoEspacioRepository.findAll());
    }

    @GetMapping("/servicios")
    public ResponseEntity<List<ServicioAdicional>> listarServiciosAdicionales() {
        return ResponseEntity.ok(servicioAdicionalRepository.findAll());
    }
}
