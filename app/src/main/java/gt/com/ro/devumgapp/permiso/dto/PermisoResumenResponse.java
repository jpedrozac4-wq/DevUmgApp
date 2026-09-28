package gt.com.ro.devumgapp.permiso.dto;

import com.google.gson.annotations.SerializedName;

public class PermisoResumenResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    public PermisoResumenResponse() {
    }

    public PermisoResumenResponse(long id, String codigo, String nombre) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
    }
}
