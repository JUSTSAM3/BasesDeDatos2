package co.edu.unbosque.reservasbackend.dto;

import java.math.BigDecimal;

public class ServicioAdicionalResponseDTO {
    private Integer idServicio;
    private String nombre;
    private String descripcion;
    private BigDecimal precioUnitario;

    public ServicioAdicionalResponseDTO() {
    }

    public ServicioAdicionalResponseDTO(Integer idServicio, String nombre, String descripcion, BigDecimal precioUnitario) {
        this.idServicio = idServicio;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioUnitario = precioUnitario;
    }

    public Integer getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(Integer idServicio) {
        this.idServicio = idServicio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}
