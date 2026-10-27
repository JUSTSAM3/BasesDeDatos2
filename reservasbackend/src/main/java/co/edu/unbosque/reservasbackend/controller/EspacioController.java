package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.dto.EspacioRequestDTO;
import co.edu.unbosque.reservasbackend.dto.EspacioResponseDTO;
import co.edu.unbosque.reservasbackend.dto.ServicioAdicionalResponseDTO;
import co.edu.unbosque.reservasbackend.dto.TipoEspacioResponseDTO;
import co.edu.unbosque.reservasbackend.sql.service.EspacioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    @PostMapping("/crear")
    public ResponseEntity<EspacioResponseDTO> crearEspacio(@RequestBody EspacioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espacioService.crearEspacio(dto));
    }

    @GetMapping("/listar")
    public ResponseEntity<List<EspacioResponseDTO>> listarEspacios() {
        return ResponseEntity.ok(espacioService.obtenerTodos());
    }

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoEspacioResponseDTO>> listarTiposEspacio() {
        return ResponseEntity.ok(espacioService.listarTipos());
    }

    @GetMapping("/servicios")
    public ResponseEntity<List<ServicioAdicionalResponseDTO>> listarServiciosAdicionales() {
        return ResponseEntity.ok(espacioService.listarServicios());
    }
}
