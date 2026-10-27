package co.edu.unbosque.reservasbackend.dto;

public class TipoEspacioResponseDTO {
    private Integer idTipoEspacio;
    private String nombre;
    private String descripcion;

    public TipoEspacioResponseDTO() {
    }

    public TipoEspacioResponseDTO(Integer idTipoEspacio, String nombre, String descripcion) {
        this.idTipoEspacio = idTipoEspacio;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Integer getIdTipoEspacio() {
        return idTipoEspacio;
    }

    public void setIdTipoEspacio(Integer idTipoEspacio) {
        this.idTipoEspacio = idTipoEspacio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
