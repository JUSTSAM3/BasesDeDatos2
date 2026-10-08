package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.dto.ReservaRequestDTO;
import co.edu.unbosque.reservasbackend.sql.entity.Cliente;
import co.edu.unbosque.reservasbackend.sql.entity.Empleado;
import co.edu.unbosque.reservasbackend.sql.entity.Espacio;
import co.edu.unbosque.reservasbackend.sql.entity.Reserva;
import co.edu.unbosque.reservasbackend.sql.repository.ClienteRepository;
import co.edu.unbosque.reservasbackend.sql.repository.EmpleadoRepository;
import co.edu.unbosque.reservasbackend.sql.repository.EspacioRepository;
import co.edu.unbosque.reservasbackend.sql.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private EspacioRepository espacioRepository;
    @Autowired
    private EmpleadoRepository empleadoRepository;

    public Reserva crearReserva(ReservaRequestDTO dto) {

        // 1. Buscar el cliente
        Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + dto.getIdCliente()));

        // 2. Buscar el espacio
        Espacio espacio = espacioRepository.findById(dto.getIdEspacio())
                .orElseThrow(() -> new RuntimeException("Espacio no encontrado con ID: " + dto.getIdEspacio()));

        // 3. Buscar empleado (Opcional, puede ser null)
        Empleado empleado = null;
        if (dto.getIdEmpleado() != null) {
            empleado = empleadoRepository.findById(dto.getIdEmpleado())
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + dto.getIdEmpleado()));
        }

        // 4. Crear el objeto Reserva
        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setEspacio(espacio);
        reserva.setEmpleado(empleado);
        reserva.setFechaInicio(dto.getFechaInicio());
        reserva.setFechaFin(dto.getFechaFin());

        // 5. Guardar en Base de Datos.
        // - trg_validar_cruce_horario (Para ver si el espacio está disponible)
        // - tg_validar_fechas_coherentes (Para evitar fechas en el pasado)
        return reservaRepository.save(reserva);
    }
}
