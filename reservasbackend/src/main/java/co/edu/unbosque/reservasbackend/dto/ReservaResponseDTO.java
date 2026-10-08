package co.edu.unbosque.reservasbackend.dto;

import java.math.BigDecimal;

public class ReservaResponseDTO {
    private Integer idReserva;
    private String estado;
    private BigDecimal costoTotal;

    public Integer getIdReserva() { return idReserva; }
    public void setIdReserva(Integer idReserva) { this.idReserva = idReserva; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public BigDecimal getCostoTotal() { return costoTotal; }
    public void setCostoTotal(BigDecimal costoTotal) { this.costoTotal = costoTotal; }
}
