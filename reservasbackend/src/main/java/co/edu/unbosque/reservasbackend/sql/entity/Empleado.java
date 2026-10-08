package co.edu.unbosque.reservasbackend.sql.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "empleados")
public class Empleado implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "cargo", length = 50)
    private String cargo;

    @Column(name = "email", length = 100)
    private String email;

    public Empleado() {
    }

    public Empleado(String nombre, String cargo, String email) {
        this.nombre = nombre;
        this.cargo = cargo;
        this.email = email;
    }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
