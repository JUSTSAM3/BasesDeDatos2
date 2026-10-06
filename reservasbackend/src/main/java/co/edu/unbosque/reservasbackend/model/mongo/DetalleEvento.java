package co.edu.unbosque.reservasbackend.model.mongo;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Documento MongoDB para la coleccion 'detalles_evento'.
 * Corresponde a la especificacion EDT 4.1.1 (RF-16, RF-18).
 */
@Document(collection = "detalles_evento")
public class DetalleEvento {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("id_reserva")
    private Integer idReserva;

    @Indexed
    @Field("id_cliente")
    private Integer idCliente;

    @Indexed
    @Field("tipo_evento")
    private String tipoEvento;

    @Field("nombre_evento")
    private String nombreEvento;

    @Field("fecha_inicio")
    private Instant fechaInicio;

    @Field("fecha_fin")
    private Instant fechaFin;

    @Field("num_invitados_estimado")
    private Integer numInvitadosEstimado;

    @Field("espacio")
    private EspacioEmbed espacio;

    @Field("servicios")
    private List<ServicioEmbed> servicios;

    @Field("invitados")
    private List<InvitadoEmbed> invitados;

    @Field("requerimientos_especiales")
    private List<RequerimientoEspecialEmbed> requerimientosEspeciales;

    @Field("agenda")
    private List<ActividadAgendaEmbed> agenda;

    @Field("datos_extra")
    private Map<String, Object> datosExtra;

    @Field("estado_reserva")
    private String estadoReserva;

    @CreatedDate
    @Field("creado_en")
    private Instant creadoEn;

    @LastModifiedDate
    @Field("actualizado_en")
    private Instant actualizadoEn;

    public DetalleEvento() {
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getNombreEvento() {
        return nombreEvento;
    }

    public void setNombreEvento(String nombreEvento) {
        this.nombreEvento = nombreEvento;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Instant fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Instant getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Instant fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getNumInvitadosEstimado() {
        return numInvitadosEstimado;
    }

    public void setNumInvitadosEstimado(Integer numInvitadosEstimado) {
        this.numInvitadosEstimado = numInvitadosEstimado;
    }

    public EspacioEmbed getEspacio() {
        return espacio;
    }

    public void setEspacio(EspacioEmbed espacio) {
        this.espacio = espacio;
    }

    public List<ServicioEmbed> getServicios() {
        return servicios;
    }

    public void setServicios(List<ServicioEmbed> servicios) {
        this.servicios = servicios;
    }

    public List<InvitadoEmbed> getInvitados() {
        return invitados;
    }

    public void setInvitados(List<InvitadoEmbed> invitados) {
        this.invitados = invitados;
    }

    public List<RequerimientoEspecialEmbed> getRequerimientosEspeciales() {
        return requerimientosEspeciales;
    }

    public void setRequerimientosEspeciales(List<RequerimientoEspecialEmbed> requerimientosEspeciales) {
        this.requerimientosEspeciales = requerimientosEspeciales;
    }

    public List<ActividadAgendaEmbed> getAgenda() {
        return agenda;
    }

    public void setAgenda(List<ActividadAgendaEmbed> agenda) {
        this.agenda = agenda;
    }

    public Map<String, Object> getDatosExtra() {
        return datosExtra;
    }

    public void setDatosExtra(Map<String, Object> datosExtra) {
        this.datosExtra = datosExtra;
    }

    public String getEstadoReserva() {
        return estadoReserva;
    }

    public void setEstadoReserva(String estadoReserva) {
        this.estadoReserva = estadoReserva;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }

    // Clases embebidas
    public static class EspacioEmbed {
        @Field("id_espacio")
        private Integer idEspacio;
        private String nombre;
        private String tipo;
        @Field("id_sucursal")
        private Integer idSucursal;
        private String ciudad;

        public Integer getIdEspacio() { return idEspacio; }
        public void setIdEspacio(Integer idEspacio) { this.idEspacio = idEspacio; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public Integer getIdSucursal() { return idSucursal; }
        public void setIdSucursal(Integer idSucursal) { this.idSucursal = idSucursal; }
        public String getCiudad() { return ciudad; }
        public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    }

    public static class ServicioEmbed {
        @Field("id_servicio")
        private Integer idServicio;
        private String nombre;
        private Integer cantidad;

        public Integer getIdServicio() { return idServicio; }
        public void setIdServicio(Integer idServicio) { this.idServicio = idServicio; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }

    public static class InvitadoEmbed {
        private String nombre;
        private String email;
        private String telefono;
        private Boolean confirmado;
        private Integer mesa;
        @Field("restricciones_alimentarias")
        private List<String> restriccionesAlimentarias;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }
        public Boolean getConfirmado() { return confirmado; }
        public void setConfirmado(Boolean confirmado) { this.confirmado = confirmado; }
        public Integer getMesa() { return mesa; }
        public void setMesa(Integer mesa) { this.mesa = mesa; }
        public List<String> getRestriccionesAlimentarias() { return restriccionesAlimentarias; }
        public void setRestriccionesAlimentarias(List<String> restriccionesAlimentarias) { this.restriccionesAlimentarias = restriccionesAlimentarias; }
    }

    public static class RequerimientoEspecialEmbed {
        private String tipo;
        private String descripcion;
        private String estado;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
    }

    public static class ActividadAgendaEmbed {
        @Field("hora_inicio")
        private Instant horaInicio;
        @Field("hora_fin")
        private Instant horaFin;
        private String actividad;
        private String responsable;

        public Instant getHoraInicio() { return horaInicio; }
        public void setHoraInicio(Instant horaInicio) { this.horaInicio = horaInicio; }
        public Instant getHoraFin() { return horaFin; }
        public void setHoraFin(Instant horaFin) { this.horaFin = horaFin; }
        public String getActividad() { return actividad; }
        public void setActividad(String actividad) { this.actividad = actividad; }
        public String getResponsable() { return responsable; }
        public void setResponsable(String responsable) { this.responsable = responsable; }
    }
}
