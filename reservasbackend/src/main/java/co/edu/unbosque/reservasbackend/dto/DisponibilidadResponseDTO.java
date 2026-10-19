package co.edu.unbosque.reservasbackend.dto;

import java.time.LocalDateTime;

public class DisponibilidadResponseDTO {

    private Integer idEspacio;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean disponible;
    private String mensaje;

    public DisponibilidadResponseDTO() {
    }

    public DisponibilidadResponseDTO(Integer idEspacio, LocalDateTime fechaInicio, LocalDateTime fechaFin, boolean disponible, String mensaje) {
        this.idEspacio = idEspacio;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.disponible = disponible;
        this.mensaje = mensaje;
    }

    public Integer getIdEspacio() {
        return idEspacio;
    }

    public void setIdEspacio(Integer idEspacio) {
        this.idEspacio = idEspacio;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
