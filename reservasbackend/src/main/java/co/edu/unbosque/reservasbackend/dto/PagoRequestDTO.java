package co.edu.unbosque.reservasbackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PagoRequestDTO {

    @NotNull(message = "El ID de la reserva es obligatorio")
    private Integer idReserva;

    @NotBlank(message = "El tipo de pago es obligatorio (anticipo, saldo, total, reembolso)")
    private String tipoPago;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotBlank(message = "El método de pago es obligatorio (transferencia, tarjeta, efectivo, PSE)")
    private String metodoPago;

    public PagoRequestDTO() {
    }

    public PagoRequestDTO(Integer idReserva, String tipoPago, BigDecimal monto, String metodoPago) {
        this.idReserva = idReserva;
        this.tipoPago = tipoPago;
        this.monto = monto;
        this.metodoPago = metodoPago;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public String getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(String tipoPago) {
        this.tipoPago = tipoPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }
}
