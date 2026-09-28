package gt.com.ro.devumgapp.rol.dto;

import com.google.gson.annotations.SerializedName;

public class RolResumenResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    public RolResumenResponse() {
    }

    public RolResumenResponse(long id, String codigo, String nombre) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
    }
}
