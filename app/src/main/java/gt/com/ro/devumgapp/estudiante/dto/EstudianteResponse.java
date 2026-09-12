package gt.com.ro.devumgapp.estudiante.dto;

import com.google.gson.annotations.SerializedName;

public class EstudianteResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigoEstudiantil")
    public String codigoEstudiantil;

    @SerializedName("numeroIdentificacion")
    public String numeroIdentificacion;

    @SerializedName("nombres")
    public String nombres;

    @SerializedName("apellidos")
    public String apellidos;

    @SerializedName("fechaNacimiento")
    public String fechaNacimiento;

    @SerializedName("correo")
    public String correo;

    @SerializedName("telefono")
    public String telefono;

    @SerializedName("direccion")
    public String direccion;

    @SerializedName("activo")
    public boolean activo;

    @SerializedName("fechaCreacion")
    public String fechaCreacion;

    @SerializedName("fechaActualizacion")
    public String fechaActualizacion;
}
