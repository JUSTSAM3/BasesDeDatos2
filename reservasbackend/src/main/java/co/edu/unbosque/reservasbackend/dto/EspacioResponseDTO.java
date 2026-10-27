package co.edu.unbosque.reservasbackend.dto;

import java.math.BigDecimal;

public class EspacioResponseDTO {
    private Integer idEspacio;
    private String nombre;
    private Integer idSucursal;
    private String nombreSucursal;
    private Integer idTipoEspacio;
    private String nombreTipoEspacio;
    private Integer capacidad;
    private BigDecimal tarifaPorHora;
    private String estado;

    public EspacioResponseDTO() {
    }

    public EspacioResponseDTO(Integer idEspacio, String nombre, Integer idSucursal, String nombreSucursal,
                              Integer idTipoEspacio, String nombreTipoEspacio, Integer capacidad,
                              BigDecimal tarifaPorHora, String estado) {
        this.idEspacio = idEspacio;
        this.nombre = nombre;
        this.idSucursal = idSucursal;
        this.nombreSucursal = nombreSucursal;
        this.idTipoEspacio = idTipoEspacio;
        this.nombreTipoEspacio = nombreTipoEspacio;
        this.capacidad = capacidad;
        this.tarifaPorHora = tarifaPorHora;
        this.estado = estado;
    }

    public Integer getIdEspacio() {
        return idEspacio;
    }

    public void setIdEspacio(Integer idEspacio) {
        this.idEspacio = idEspacio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getIdSucursal() {
        return idSucursal;
    }

    public void setIdSucursal(Integer idSucursal) {
        this.idSucursal = idSucursal;
    }

    public String getNombreSucursal() {
        return nombreSucursal;
    }

    public void setNombreSucursal(String nombreSucursal) {
        this.nombreSucursal = nombreSucursal;
    }

    public Integer getIdTipoEspacio() {
        return idTipoEspacio;
    }

    public void setIdTipoEspacio(Integer idTipoEspacio) {
        this.idTipoEspacio = idTipoEspacio;
    }

    public String getNombreTipoEspacio() {
        return nombreTipoEspacio;
    }

    public void setNombreTipoEspacio(String nombreTipoEspacio) {
        this.nombreTipoEspacio = nombreTipoEspacio;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public BigDecimal getTarifaPorHora() {
        return tarifaPorHora;
    }

    public void setTarifaPorHora(BigDecimal tarifaPorHora) {
        this.tarifaPorHora = tarifaPorHora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
