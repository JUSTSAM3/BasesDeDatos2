package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.dto.PagoRequestDTO;
import co.edu.unbosque.reservasbackend.dto.PagoResponseDTO;
import co.edu.unbosque.reservasbackend.sql.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "*")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    /**
     * RF-13: Registra un pago para una reserva. Invoca SP_REGISTRAR_PAGO.
     */
    @PostMapping("/registrar")
    public ResponseEntity<PagoResponseDTO> registrarPago(@Valid @RequestBody PagoRequestDTO dto) {
        PagoResponseDTO response = pagoService.registrarPago(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getIdPago())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * RF-15: Consulta el estado de liquidación/pago de una reserva.
     */
    @GetMapping("/estado/{idReserva}")
    public ResponseEntity<Map<String, Object>> consultarEstadoPago(@PathVariable Integer idReserva) {
        String estadoPago = pagoService.consultarEstadoPago(idReserva);
        return ResponseEntity.ok(Map.of(
                "idReserva", idReserva,
                "estadoPago", estadoPago
        ));
    }
}
