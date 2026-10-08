package co.edu.unbosque.reservasbackend.sql.repository;

import co.edu.unbosque.reservasbackend.sql.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {
    
    List<Pago> findByFacturaIdFactura(Integer idFactura);
}
