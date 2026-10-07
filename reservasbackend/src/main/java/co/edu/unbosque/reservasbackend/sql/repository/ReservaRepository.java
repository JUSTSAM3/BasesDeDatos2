package co.edu.unbosque.reservasbackend.sql.repository;

import co.edu.unbosque.reservasbackend.sql.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
    
    // Buscar reservas de un cliente específico
    List<Reserva> findByClienteIdCliente(Integer idCliente);
    
    // Buscar reservas por estado (ej. "pendiente", "completada")
    List<Reserva> findByEstado(String estado);
}
