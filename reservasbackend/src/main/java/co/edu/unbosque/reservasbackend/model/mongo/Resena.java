package co.edu.unbosque.reservasbackend.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * Documento MongoDB para la coleccion 'resenas'.
 * Corresponde a la especificacion EDT 4.1.1 (RF-17, RF-18).
 */
@Document(collection = "resenas")
public class Resena {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("id_reserva")
    private Integer idReserva;

    @Indexed
    @Field("id_cliente")
    private Integer idCliente;

    @Indexed
    @Field("id_espacio")
    private Integer idEspacio;

    @Field("calificacion_general")
    private Integer calificacionGeneral;

    @Field("calificaciones")
    private CalificacionesDetalle calificaciones;

    @Field("comentario")
    private String comentario;

    @Field("recomendaria")
    private Boolean recomendaria;

    @Field("etiquetas")
    private List<String> etiquetas;

    @Field("respuesta_admin")
    private RespuestaAdmin respuestaAdmin;

    @Field("fecha_resena")
    private Instant fechaResena;

    public Resena() {
    }

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

    public Integer getIdEspacio() {
        return idEspacio;
    }

    public void setIdEspacio(Integer idEspacio) {
        this.idEspacio = idEspacio;
    }

    public Integer getCalificacionGeneral() {
        return calificacionGeneral;
    }

    public void setCalificacionGeneral(Integer calificacionGeneral) {
        this.calificacionGeneral = calificacionGeneral;
    }

    public CalificacionesDetalle getCalificaciones() {
        return calificaciones;
    }

    public void setCalificaciones(CalificacionesDetalle calificaciones) {
        this.calificaciones = calificaciones;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Boolean getRecomendaria() {
        return recomendaria;
    }

    public void setRecomendaria(Boolean recomendaria) {
        this.recomendaria = recomendaria;
    }

    public List<String> getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(List<String> etiquetas) {
        this.etiquetas = etiquetas;
    }

    public RespuestaAdmin getRespuestaAdmin() {
        return respuestaAdmin;
    }

    public void setRespuestaAdmin(RespuestaAdmin respuestaAdmin) {
        this.respuestaAdmin = respuestaAdmin;
    }

    public Instant getFechaResena() {
        return fechaResena;
    }

    public void setFechaResena(Instant fechaResena) {
        this.fechaResena = fechaResena;
    }

    public static class CalificacionesDetalle {
        private Integer espacio;
        private Integer servicios;
        private Integer coordinacion;
        private Integer precio;

        public Integer getEspacio() { return espacio; }
        public void setEspacio(Integer espacio) { this.espacio = espacio; }
        public Integer getServicios() { return servicios; }
        public void setServicios(Integer servicios) { this.servicios = servicios; }
        public Integer getCoordinacion() { return coordinacion; }
        public void setCoordinacion(Integer coordinacion) { this.coordinacion = coordinacion; }
        public Integer getPrecio() { return precio; }
        public void setPrecio(Integer precio) { this.precio = precio; }
    }

    public static class RespuestaAdmin {
        private String texto;
        @Field("id_empleado")
        private Integer idEmpleado;
        private Instant fecha;

        public String getTexto() { return texto; }
        public void setTexto(String texto) { this.texto = texto; }
        public Integer getIdEmpleado() { return idEmpleado; }
        public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }
        public Instant getFecha() { return fecha; }
        public void setFecha(Instant fecha) { this.fecha = fecha; }
    }
}
