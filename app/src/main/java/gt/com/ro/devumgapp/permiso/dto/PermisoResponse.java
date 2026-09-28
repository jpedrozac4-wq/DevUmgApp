package gt.com.ro.devumgapp.permiso.dto;

import com.google.gson.annotations.SerializedName;

public class PermisoResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("descripcion")
    public String descripcion;

    @SerializedName("activo")
    public boolean activo;

    @SerializedName("fechaCreacion")
    public String fechaCreacion;

    @SerializedName("fechaActualizacion")
    public String fechaActualizacion;

    public PermisoResponse() {
    }

    public PermisoResponse(
            long id,
            String codigo,
            String nombre,
            String descripcion,
            boolean activo,
            String fechaCreacion,
            String fechaActualizacion) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }
}
