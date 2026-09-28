package gt.com.ro.devumgapp.rol.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import gt.com.ro.devumgapp.permiso.dto.PermisoResumenResponse;

public class RolResponse {

    @SerializedName("id")
    public long id; // Solo lectura

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("descripcion")
    public String descripcion;

    @SerializedName("activo")
    public boolean activo;

    @SerializedName("permisos")
    public List<PermisoResumenResponse> permisos;

    @SerializedName("fechaCreacion")
    public String fechaCreacion;     // Solo lectura

    @SerializedName("fechaActualizacion")
    public String fechaActualizacion; // Solo lectura

    public RolResponse() {
    }

    public RolResponse(
            long id,
            String codigo,
            String nombre,
            String descripcion,
            boolean activo,
            List<PermisoResumenResponse> permisos,
            String fechaCreacion,
            String fechaActualizacion) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
        this.permisos = permisos;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }
}
