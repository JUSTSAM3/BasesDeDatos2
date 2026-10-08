package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.dto.ReservaRequestDTO;
import co.edu.unbosque.reservasbackend.dto.ReservaResponseDTO;
import co.edu.unbosque.reservasbackend.sql.service.ReservaService;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping("/crear")
    public ResponseEntity<ReservaResponseDTO> crearReserva(@Valid @RequestBody ReservaRequestDTO dto) {
        ReservaResponseDTO response = reservaService.crearReserva(dto);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getIdReserva())
                .toUri();
                
        return ResponseEntity.created(location).body(response);
    }
}
