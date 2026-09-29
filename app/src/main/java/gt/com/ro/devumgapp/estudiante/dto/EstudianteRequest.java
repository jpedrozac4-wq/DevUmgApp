package gt.com.ro.devumgapp.estudiante.dto;

import com.google.gson.annotations.SerializedName;

public class EstudianteRequest {

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
    @SerializedName("accesoApp") public Boolean accesoApp;
    @SerializedName("usuarioId") public Long usuarioId;
    @SerializedName("username") public String username;
    @SerializedName("password") public String password;
}
