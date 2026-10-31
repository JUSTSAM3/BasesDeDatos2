package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.nosql.document.DetalleEvento;
import co.edu.unbosque.reservasbackend.nosql.document.Resena;
import co.edu.unbosque.reservasbackend.nosql.service.EventoNoSqlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la base de datos documental MongoDB (RF-16, RF-17, RF-18).
 * Expone operaciones CRUD sobre 'detalles_evento' y 'resenas', inicializacion
 * de semilla y reportes agregados con pipelines de MongoDB.
 */
@RestController
@RequestMapping("/api/nosql")
public class EventoNoSqlController {

    private final EventoNoSqlService eventoNoSqlService;

    public EventoNoSqlController(EventoNoSqlService eventoNoSqlService) {
        this.eventoNoSqlService = eventoNoSqlService;
    }

    // ==================== INICIALIZACION / SEED ====================

    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> inicializarDatosMongo() {
        return ResponseEntity.ok(eventoNoSqlService.inicializarEsquemaYDatosSemilla());
    }

    // ==================== DETALLES DE EVENTO (RF-16) ====================

    @PostMapping("/detalles-evento")
    public ResponseEntity<DetalleEvento> guardarDetalleEvento(@RequestBody DetalleEvento detalle) {
        DetalleEvento guardado = eventoNoSqlService.guardarOActualizarDetalleEvento(detalle);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @GetMapping("/detalles-evento")
    public ResponseEntity<List<DetalleEvento>> listarDetallesEvento(
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(required = false) Integer idCliente
    ) {
        return ResponseEntity.ok(eventoNoSqlService.listarDetallesEvento(tipoEvento, idCliente));
    }

    @GetMapping("/detalles-evento/reserva/{idReserva}")
    public ResponseEntity<DetalleEvento> obtenerDetallePorReserva(@PathVariable Integer idReserva) {
        return eventoNoSqlService.obtenerDetallePorReserva(idReserva)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/detalles-evento/reserva/{idReserva}")
    public ResponseEntity<Map<String, Object>> eliminarDetallePorReserva(@PathVariable Integer idReserva) {
        boolean eliminado = eventoNoSqlService.eliminarDetallePorReserva(idReserva);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "idReserva", idReserva,
                "eliminado", true,
                "mensaje", "Detalle de evento eliminado de MongoDB exitosamente."
        ));
    }

    // ==================== RESEÑAS POST-EVENTO (RF-17) ====================

    @PostMapping("/resenas")
    public ResponseEntity<Resena> guardarResena(@RequestBody Resena resena) {
        Resena guardada = eventoNoSqlService.guardarOActualizarResena(resena);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @GetMapping("/resenas")
    public ResponseEntity<List<Resena>> listarResenas(
            @RequestParam(required = false) Integer idEspacio,
            @RequestParam(required = false) Integer idCliente
    ) {
        return ResponseEntity.ok(eventoNoSqlService.listarResenas(idEspacio, idCliente));
    }

    @PatchMapping("/resenas/reserva/{idReserva}/respuesta")
    public ResponseEntity<Resena> responderResena(
            @PathVariable Integer idReserva,
            @RequestBody Resena.RespuestaAdmin respuesta
    ) {
        return ResponseEntity.ok(eventoNoSqlService.responderResenaAdmin(idReserva, respuesta));
    }

    // ==================== REPORTES Y AGREGACIONES (RF-18) ====================

    @GetMapping("/reportes/espacios-mas-reservados")
    public ResponseEntity<List<Map<String, Object>>> reporteEspaciosMasReservados() {
        return ResponseEntity.ok(eventoNoSqlService.reporteEspaciosMasReservados());
    }

    @GetMapping("/reportes/servicios-mas-solicitados")
    public ResponseEntity<List<Map<String, Object>>> reporteServiciosMasSolicitados() {
        return ResponseEntity.ok(eventoNoSqlService.reporteServiciosMasSolicitados());
    }

    @GetMapping("/reportes/calificaciones-espacios")
    public ResponseEntity<List<Map<String, Object>>> reporteCalificacionesPorEspacio() {
        return ResponseEntity.ok(eventoNoSqlService.reporteCalificacionesPorEspacio());
    }
}

