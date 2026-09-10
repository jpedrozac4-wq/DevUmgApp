package gt.com.ro.devumgapp.estudiante.dto;

import com.google.gson.annotations.SerializedName;

public class EstudianteResumenResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigoEstudiantil")
    public String codigoEstudiantil;

    @SerializedName("nombres")
    public String nombres;

    @SerializedName("apellidos")
    public String apellidos;

    @SerializedName("activo")
    public boolean activo;
}
