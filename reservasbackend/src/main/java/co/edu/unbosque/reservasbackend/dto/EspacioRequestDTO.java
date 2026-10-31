package co.edu.unbosque.reservasbackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class EspacioRequestDTO {
    @NotNull(message = "El ID de la sucursal es obligatorio")
    private Integer idSucursal;

    @NotBlank(message = "El nombre del espacio es obligatorio")
    private String nombre;

    @NotNull(message = "El ID del tipo de espacio es obligatorio")
    private Integer idTipoEspacio;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser al menos de 1 persona")
    private Integer capacidad;

    @NotNull(message = "La tarifa por hora es obligatoria")
    @DecimalMin(value = "0.00", message = "La tarifa por hora no puede ser negativa")
    private BigDecimal tarifaPorHora;

    private String estado;

    public Integer getIdSucursal() { return idSucursal; }
    public void setIdSucursal(Integer idSucursal) { this.idSucursal = idSucursal; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getIdTipoEspacio() { return idTipoEspacio; }
    public void setIdTipoEspacio(Integer idTipoEspacio) { this.idTipoEspacio = idTipoEspacio; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public BigDecimal getTarifaPorHora() { return tarifaPorHora; }
    public void setTarifaPorHora(BigDecimal tarifaPorHora) { this.tarifaPorHora = tarifaPorHora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
