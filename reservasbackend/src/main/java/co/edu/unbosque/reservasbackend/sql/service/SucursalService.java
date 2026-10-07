package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.sql.entity.Sucursal;
import co.edu.unbosque.reservasbackend.sql.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SucursalService {

    @Autowired
    private SucursalRepository sucursalRepository;

    public Sucursal crearSucursal(Sucursal sucursal) {
        return sucursalRepository.save(sucursal);
    }

    public List<Sucursal> obtenerTodas() {
        return sucursalRepository.findAll();
    }
}
