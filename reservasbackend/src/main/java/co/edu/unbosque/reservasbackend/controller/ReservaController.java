package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.dto.DisponibilidadResponseDTO;
import co.edu.unbosque.reservasbackend.dto.ReservaRequestDTO;
import co.edu.unbosque.reservasbackend.dto.ReservaResponseDTO;
import co.edu.unbosque.reservasbackend.sql.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * RF-07 / RF-11 / RF-12: Crea una nueva reserva delegando al SP_CREAR_RESERVA.
     */
    @PostMapping("/crear")
    public ResponseEntity<ReservaResponseDTO> crearReserva(@Valid @RequestBody ReservaRequestDTO dto) {
        ReservaResponseDTO response = reservaService.crearReserva(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getIdReserva())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Consulta el detalle de una reserva por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponseDTO> obtenerReserva(@PathVariable Integer id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    /**
     * Lista todas las reservas registradas.
     */
    @GetMapping("/listar")
    public ResponseEntity<List<ReservaResponseDTO>> listarReservas() {
        return ResponseEntity.ok(reservaService.obtenerTodas());
    }

    /**
     * RF-05: Valida si un espacio tiene disponibilidad horaria (invoca fn_validar_disponibilidad).
     */
    @GetMapping("/disponibilidad")
    public ResponseEntity<DisponibilidadResponseDTO> consultarDisponibilidad(
            @RequestParam Integer idEspacio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) Integer idReservaExcluir
    ) {
        DisponibilidadResponseDTO response = reservaService.validarDisponibilidad(
                idEspacio, fechaInicio, fechaFin, idReservaExcluir
        );
        return ResponseEntity.ok(response);
    }

    /**
     * RF-09: Cancela una reserva invocando el procedimiento almacenado SP_CANCELAR_RESERVA.
     */
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Map<String, Object>> cancelarReserva(@PathVariable Integer id) {
        Map<String, Object> resultado = reservaService.cancelarReserva(id);
        return ResponseEntity.ok(resultado);
    }
}
