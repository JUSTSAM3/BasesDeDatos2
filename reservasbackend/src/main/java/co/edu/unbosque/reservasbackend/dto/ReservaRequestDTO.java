package co.edu.unbosque.reservasbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public class ReservaRequestDTO {

    @NotNull(message = "El cliente es obligatorio")
    private Integer idCliente;

    @NotNull(message = "El espacio es obligatorio")
    private Integer idEspacio;

    private Integer idEmpleado;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio no puede ser en el pasado")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @FutureOrPresent(message = "La fecha de fin no puede ser en el pasado")
    private LocalDateTime fechaFin;

    @Valid
    private List<ServicioAdicionalDTO> servicios;

    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }

    public Integer getIdEspacio() { return idEspacio; }
    public void setIdEspacio(Integer idEspacio) { this.idEspacio = idEspacio; }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public List<ServicioAdicionalDTO> getServicios() { return servicios; }
    public void setServicios(List<ServicioAdicionalDTO> servicios) { this.servicios = servicios; }

    @AssertTrue(message = "La fecha de fin debe ser posterior a la fecha de inicio")
    public boolean isFechasValidas() {
        if (fechaInicio == null || fechaFin == null) {
            return true; 
        }
        return fechaFin.isAfter(fechaInicio);
    }

    public static class ServicioAdicionalDTO {
        @NotNull(message = "El id_servicio es obligatorio")
        @JsonProperty("id_servicio")
        private Integer idServicio;

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0")
        @JsonProperty("cantidad")
        private Integer cantidad;

        public Integer getIdServicio() { return idServicio; }
        public void setIdServicio(Integer idServicio) { this.idServicio = idServicio; }

        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }
}
