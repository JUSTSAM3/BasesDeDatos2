package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.dto.ReservaRequestDTO;
import co.edu.unbosque.reservasbackend.sql.entity.Reserva;
import co.edu.unbosque.reservasbackend.sql.service.ReservaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @PostMapping("/crear")
    public ResponseEntity<?> crearReserva(@RequestBody ReservaRequestDTO dto) {
        try {
            Reserva nuevaReserva = reservaService.crearReserva(dto);
            return ResponseEntity.ok(nuevaReserva);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear la reserva: " + e.getMessage());
        }
    }
}
