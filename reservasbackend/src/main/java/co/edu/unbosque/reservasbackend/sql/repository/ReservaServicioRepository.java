package co.edu.unbosque.reservasbackend.sql.repository;

import co.edu.unbosque.reservasbackend.sql.entity.ReservaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReservaServicioRepository extends JpaRepository<ReservaServicio, Integer> {
    
    List<ReservaServicio> findByReservaIdReserva(Integer idReserva);
}
