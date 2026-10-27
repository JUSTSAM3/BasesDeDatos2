package co.edu.unbosque.reservasbackend.sql.repository;

import co.edu.unbosque.reservasbackend.sql.entity.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspacioRepository extends JpaRepository<Espacio, Integer> {

    // SELECT * FROM espacios WHERE id_sucursal = ?
    List<Espacio> findBySucursalIdSucursal(Integer idSucursal);

    // SELECT * FROM espacios WHERE estado = ?
    List<Espacio> findByEstado(String estado);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Espacio e LEFT JOIN FETCH e.sucursal LEFT JOIN FETCH e.tipoEspacio")
    List<Espacio> findAllWithSucursalAndTipo();
}
