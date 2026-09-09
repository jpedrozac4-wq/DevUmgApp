package gt.com.ro.devumgapp.carrera.dto;

import com.google.gson.annotations.SerializedName;

public class CarreraResumenResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("activo")
    public boolean activo;
}
