package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.dto.EspacioRequestDTO;
import co.edu.unbosque.reservasbackend.sql.entity.Espacio;
import co.edu.unbosque.reservasbackend.sql.entity.Sucursal;
import co.edu.unbosque.reservasbackend.sql.repository.EspacioRepository;
import co.edu.unbosque.reservasbackend.sql.repository.SucursalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspacioService {

    @Autowired
    private EspacioRepository espacioRepository;
    
    @Autowired
    private SucursalRepository sucursalRepository;

    public Espacio crearEspacio(EspacioRequestDTO dto) {
        Sucursal sucursal = sucursalRepository.findById(dto.getIdSucursal())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada con ID: " + dto.getIdSucursal()));

        Espacio espacio = new Espacio();
        espacio.setSucursal(sucursal);
        espacio.setNombre(dto.getNombre());
        espacio.setTipo(dto.getTipo());
        espacio.setCapacidad(dto.getCapacidad());
        espacio.setTarifaPorHora(dto.getTarifaPorHora());
        espacio.setEstado(dto.getEstado() != null ? dto.getEstado() : "disponible");

        return espacioRepository.save(espacio);
    }

    public List<Espacio> obtenerTodos() {
        return espacioRepository.findAll();
    }
}
