package co.edu.unbosque.reservasbackend.sql.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "reserva_servicios")
public class ReservaServicio implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva_servicio")
    private Integer idReservaServicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio", nullable = false)
    private ServicioAdicional servicioAdicional;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @PrePersist
    public void prePersist() {
        if (cantidad == null) {
            cantidad = 1;
        }
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }
    }

    public ReservaServicio() {
    }

    public Integer getIdReservaServicio() { return idReservaServicio; }
    public void setIdReservaServicio(Integer idReservaServicio) { this.idReservaServicio = idReservaServicio; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public ServicioAdicional getServicioAdicional() { return servicioAdicional; }
    public void setServicioAdicional(ServicioAdicional servicioAdicional) { this.servicioAdicional = servicioAdicional; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
