package co.edu.unbosque.reservasbackend.sql.repository;

import co.edu.unbosque.reservasbackend.sql.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {
    
    Optional<Empleado> findByEmail(String email);
}
