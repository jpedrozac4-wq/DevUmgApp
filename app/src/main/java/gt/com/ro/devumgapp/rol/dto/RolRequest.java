package gt.com.ro.devumgapp.rol.dto;

import com.google.gson.annotations.SerializedName;

public class RolRequest {

    @SerializedName("codigo")
    public String codigo;      // 3 a 50, patron A-Za-z0-9_

    @SerializedName("nombre")
    public String nombre;      // 3 a 100

    @SerializedName("descripcion")
    public String descripcion; // Máximo 300

    public RolRequest(String codigo, String nombre, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}
