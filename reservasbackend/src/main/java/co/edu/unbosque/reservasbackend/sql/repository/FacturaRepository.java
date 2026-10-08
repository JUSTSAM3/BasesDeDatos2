package co.edu.unbosque.reservasbackend.sql.repository;

import co.edu.unbosque.reservasbackend.sql.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    
    Optional<Factura> findByNumeroFactura(String numeroFactura);
}
