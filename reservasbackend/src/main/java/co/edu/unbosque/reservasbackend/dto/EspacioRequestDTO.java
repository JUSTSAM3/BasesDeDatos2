package co.edu.unbosque.reservasbackend.dto;

import java.math.BigDecimal;

public class EspacioRequestDTO {
    private Integer idSucursal;
    private String nombre;
    private String tipo;
    private Integer capacidad;
    private BigDecimal tarifaPorHora;
    private String estado;

    public Integer getIdSucursal() { return idSucursal; }
    public void setIdSucursal(Integer idSucursal) { this.idSucursal = idSucursal; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public BigDecimal getTarifaPorHora() { return tarifaPorHora; }
    public void setTarifaPorHora(BigDecimal tarifaPorHora) { this.tarifaPorHora = tarifaPorHora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
