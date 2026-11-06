package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.sql.entity.Empleado;
import co.edu.unbosque.reservasbackend.sql.repository.EmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Value("${app.security.staff-access-key:}")
    private String staffAccessKey;

    public Empleado crearEmpleado(Empleado empleado) {
        return empleadoRepository.save(empleado);
    }

    public List<Empleado> obtenerTodos() {
        return empleadoRepository.findAll();
    }

    /**
     * Valida las credenciales corporativas de un empleado contra la clave de acceso
     * parametrizada por entorno (STAFF_ACCESS_KEY) y la existencia del correo en PostgreSQL.
     */
    public Empleado autenticarEmpleado(String email, String clave) {
        if (email == null || email.isBlank() || clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Ingresa tu correo corporativo y clave de acceso.");
        }
        if (staffAccessKey == null || staffAccessKey.isBlank()) {
            throw new IllegalArgumentException("La clave de acceso de personal (STAFF_ACCESS_KEY) no está configurada en el servidor.");
        }

        boolean claveValida = MessageDigest.isEqual(
                clave.trim().getBytes(StandardCharsets.UTF_8),
                staffAccessKey.trim().getBytes(StandardCharsets.UTF_8)
        );
        if (!claveValida) {
            throw new IllegalArgumentException("Acceso denegado: correo corporativo o clave no válidos.");
        }

        String correoNormalizado = email.trim().toLowerCase();
        return empleadoRepository.findByEmail(correoNormalizado)
                .or(() -> empleadoRepository.findAll().stream()
                        .filter(e -> e.getEmail() != null && e.getEmail().trim().equalsIgnoreCase(correoNormalizado))
                        .findFirst())
                .orElseThrow(() -> new IllegalArgumentException("No encontramos ningún empleado activo con ese correo corporativo."));
    }
}
