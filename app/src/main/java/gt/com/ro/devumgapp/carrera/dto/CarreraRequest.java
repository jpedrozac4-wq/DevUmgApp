package gt.com.ro.devumgapp.carrera.dto;

import com.google.gson.annotations.SerializedName;

public class CarreraRequest {

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("descripcion")
    public String descripcion;

    @SerializedName("duracionAnios")
    public int duracionAnios;

    public CarreraRequest(String codigo, String nombre, String descripcion, int duracionAnios) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionAnios = duracionAnios;
    }
}
