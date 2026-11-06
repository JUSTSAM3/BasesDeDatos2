package co.edu.unbosque.reservasbackend.service;

import co.edu.unbosque.reservasbackend.nosql.document.DetalleEvento;
import co.edu.unbosque.reservasbackend.nosql.document.Resena;
import co.edu.unbosque.reservasbackend.nosql.repository.DetalleEventoRepository;
import co.edu.unbosque.reservasbackend.nosql.repository.ResenaRepository;
import co.edu.unbosque.reservasbackend.nosql.service.EventoNoSqlService;
import co.edu.unbosque.reservasbackend.sql.entity.Cliente;
import co.edu.unbosque.reservasbackend.sql.entity.Espacio;
import co.edu.unbosque.reservasbackend.sql.entity.Reserva;
import co.edu.unbosque.reservasbackend.sql.entity.Sucursal;
import co.edu.unbosque.reservasbackend.sql.entity.TipoEspacio;
import co.edu.unbosque.reservasbackend.sql.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EventoNoSqlServiceTest {

    private DetalleEventoRepository detalleEventoRepository;
    private ResenaRepository resenaRepository;
    private MongoTemplate mongoTemplate;
    private ReservaRepository reservaRepository;
    private EventoNoSqlService service;

    @BeforeEach
    public void setUp() {
        detalleEventoRepository = mock(DetalleEventoRepository.class);
        resenaRepository = mock(ResenaRepository.class);
        mongoTemplate = mock(MongoTemplate.class);
        reservaRepository = mock(ReservaRepository.class);

        service = new EventoNoSqlService(
                detalleEventoRepository,
                resenaRepository,
                mongoTemplate,
                reservaRepository
        );
    }

    @Test
    public void testGuardarDetalleEvento_ReservaInexistenteEnPostgres_ThrowsException() {
        when(reservaRepository.findById(999)).thenReturn(Optional.empty());

        DetalleEvento detalle = new DetalleEvento();
        detalle.setIdReserva(999);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.guardarOActualizarDetalleEvento(detalle)
        );
        assertTrue(ex.getMessage().contains("No existe una reserva registrada con ID #999"));
        verify(detalleEventoRepository, never()).save(any());
    }

    @Test
    public void testGuardarDetalleEvento_SincronizaDatosDesdePostgres() {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(5);

        Sucursal sucursal = new Sucursal();
        sucursal.setIdSucursal(2);
        sucursal.setCiudad("Bogotá");

        TipoEspacio tipoEspacio = new TipoEspacio();
        tipoEspacio.setNombre("Salón de Eventos");

        Espacio espacio = new Espacio();
        espacio.setIdEspacio(3);
        espacio.setNombre("Salón Galaxia");
        espacio.setSucursal(sucursal);
        espacio.setTipoEspacio(tipoEspacio);

        Reserva reserva = new Reserva();
        reserva.setIdReserva(10);
        reserva.setCliente(cliente);
        reserva.setEspacio(espacio);
        reserva.setEstado("confirmada");
        reserva.setFechaInicio(LocalDateTime.now().plusDays(2));
        reserva.setFechaFin(LocalDateTime.now().plusDays(2).plusHours(5));

        when(reservaRepository.findById(10)).thenReturn(Optional.of(reserva));
        when(detalleEventoRepository.findByIdReserva(10)).thenReturn(Optional.empty());
        when(detalleEventoRepository.save(any(DetalleEvento.class))).thenAnswer(i -> i.getArgument(0));

        DetalleEvento detalle = new DetalleEvento();
        detalle.setIdReserva(10);
        detalle.setNombreEvento("Fiesta de Aniversario");

        DetalleEvento guardado = service.guardarOActualizarDetalleEvento(detalle);

        assertEquals(5, guardado.getIdCliente());
        assertNotNull(guardado.getEspacio());
        assertEquals(3, guardado.getEspacio().getIdEspacio());
        assertEquals("Salón Galaxia", guardado.getEspacio().getNombre());
        assertEquals("confirmada", guardado.getEstadoReserva());
        verify(detalleEventoRepository, times(1)).save(detalle);
    }

    @Test
    public void testGuardarResena_ReservaInexistenteEnPostgres_ThrowsException() {
        when(reservaRepository.findById(999)).thenReturn(Optional.empty());

        Resena resena = new Resena();
        resena.setIdReserva(999);
        resena.setCalificacionGeneral(5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.guardarOActualizarResena(resena)
        );
        assertTrue(ex.getMessage().contains("No existe una reserva registrada con ID #999"));
        verify(resenaRepository, never()).save(any());
    }

    @Test
    public void testGuardarResena_ReservaNoFinalizada_ThrowsIllegalStateException() {
        Reserva reserva = new Reserva();
        reserva.setIdReserva(15);
        reserva.setEstado("confirmada");

        when(reservaRepository.findById(15)).thenReturn(Optional.of(reserva));

        Resena resena = new Resena();
        resena.setIdReserva(15);
        resena.setCalificacionGeneral(4);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.guardarOActualizarResena(resena)
        );
        assertTrue(ex.getMessage().contains("Solo se pueden registrar reseñas para reservas con estado 'finalizada'"));
        verify(resenaRepository, never()).save(any());
    }

    @Test
    public void testGuardarResena_ReservaFinalizada_GuardaExitosamente() {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(7);

        Espacio espacio = new Espacio();
        espacio.setIdEspacio(4);

        Reserva reserva = new Reserva();
        reserva.setIdReserva(25);
        reserva.setEstado("finalizada");
        reserva.setCliente(cliente);
        reserva.setEspacio(espacio);

        when(reservaRepository.findById(25)).thenReturn(Optional.of(reserva));
        when(resenaRepository.findByIdReserva(25)).thenReturn(Optional.empty());
        when(resenaRepository.save(any(Resena.class))).thenAnswer(i -> i.getArgument(0));

        Resena resena = new Resena();
        resena.setIdReserva(25);
        resena.setCalificacionGeneral(5);
        resena.setComentario("Servicio impecable");

        Resena guardada = service.guardarOActualizarResena(resena);

        assertNotNull(guardada);
        assertEquals(7, guardada.getIdCliente());
        assertEquals(4, guardada.getIdEspacio());
        verify(resenaRepository, times(1)).save(resena);
    }

    @Test
    public void testEliminarResenaPorReserva_Exitoso() {
        Resena resena = new Resena();
        resena.setIdReserva(101);

        when(resenaRepository.findByIdReserva(101)).thenReturn(Optional.of(resena));

        boolean resultado = service.eliminarResenaPorReserva(101);

        assertTrue(resultado);
        verify(resenaRepository, times(1)).delete(resena);
    }

    @Test
    public void testEliminarResenaPorReserva_NoEncontrada() {
        when(resenaRepository.findByIdReserva(999)).thenReturn(Optional.empty());

        boolean resultado = service.eliminarResenaPorReserva(999);

        assertFalse(resultado);
        verify(resenaRepository, never()).delete(any());
    }

    @Test
    public void testConsultarEventosAvanzado_EjecutaQueryConProyeccion() {
        DetalleEvento e1 = new DetalleEvento();
        e1.setIdReserva(101);

        when(mongoTemplate.find(any(Query.class), eq(DetalleEvento.class))).thenReturn(List.of(e1));

        List<DetalleEvento> resultado = service.consultarEventosAvanzado(
                List.of("boda", "social"),
                50,
                "confirmada",
                null,
                null
        );

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(DetalleEvento.class));
    }
}

