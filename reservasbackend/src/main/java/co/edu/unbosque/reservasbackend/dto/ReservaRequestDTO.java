package co.edu.unbosque.reservasbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class ReservaRequestDTO {

    private Integer idCliente;
    private Integer idEspacio;
    private Integer idEmpleado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
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

    public static class ServicioAdicionalDTO {
        @JsonProperty("id_servicio")
        private Integer idServicio;

        @JsonProperty("cantidad")
        private Integer cantidad;

        public Integer getIdServicio() { return idServicio; }
        public void setIdServicio(Integer idServicio) { this.idServicio = idServicio; }

        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }
}
