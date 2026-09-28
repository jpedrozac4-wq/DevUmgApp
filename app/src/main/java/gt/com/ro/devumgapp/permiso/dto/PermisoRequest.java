package gt.com.ro.devumgapp.permiso.dto;

import com.google.gson.annotations.SerializedName;

public class PermisoRequest {

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("descripcion")
    public String descripcion;

    public PermisoRequest(String codigo, String nombre, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}
