package co.edu.unbosque.reservasbackend.controller;

import co.edu.unbosque.reservasbackend.sql.entity.Cliente;
import co.edu.unbosque.reservasbackend.sql.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class TestController {

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping("/api/test")
    public String test() {
        return "Conexion Angular Spring correcta";
    }

    @GetMapping("/api/test/crear-cliente")
    public String crearClientePrueba() {
        try {
            String identificacionPrueba = "1234567890";

            Optional<Cliente> existente = clienteRepository.findByIdentificacion(identificacionPrueba);
            if (existente.isPresent()) {
                return "El cliente de prueba ya estaba guardado en PostgreSQL (ID: "
                        + existente.get().getIdCliente() + ").";
            }

            Cliente nuevoCliente = new Cliente(
                    "Regular",
                    identificacionPrueba,
                    "Juan Pérez Test",
                    "juan.test@correo.com",
                    "555-1234");

            clienteRepository.save(nuevoCliente);

            return "El cliente '" + nuevoCliente.getNombreRazonSocial()
                    + " se ha guardado correctamente.";

        } catch (Exception e) {
            return " hubo un error al comunicarse con la base de datos: " + e.getMessage();
        }
    }
}