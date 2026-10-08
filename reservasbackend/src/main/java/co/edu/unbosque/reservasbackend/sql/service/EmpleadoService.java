package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.sql.entity.Empleado;
import co.edu.unbosque.reservasbackend.sql.repository.EmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    public Empleado crearEmpleado(Empleado empleado) {
        return empleadoRepository.save(empleado);
    }

    public List<Empleado> obtenerTodos() {
        return empleadoRepository.findAll();
    }
}
