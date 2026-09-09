package gt.com.ro.devumgapp.curso.dto;

import com.google.gson.annotations.SerializedName;

public class DocenteResumenResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigoDocente")
    public String codigoDocente;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("apellido")
    public String apellido;

    @SerializedName("email")
    public String email;

    @SerializedName("telefono")
    public String telefono;

    @SerializedName("especialidad")
    public String especialidad;

    @SerializedName("activo")
    public boolean activo;
}
