package co.edu.unbosque.reservasbackend.sql.repository;

import co.edu.unbosque.reservasbackend.sql.entity.ServicioAdicional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicioAdicionalRepository extends JpaRepository<ServicioAdicional, Integer> {
}
