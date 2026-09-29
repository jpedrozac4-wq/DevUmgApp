package gt.com.ro.devumgapp.usuario.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UsuarioAltaConjuntaRequest {
    @SerializedName("username") public String username;
    @SerializedName("password") public String password;
    @SerializedName("nombre") public String nombre;
    @SerializedName("apellido") public String apellido;
    @SerializedName("correo") public String correo;
    @SerializedName("rolIds") public List<Long> rolIds;
    @SerializedName("docente") public Docente docente;
    @SerializedName("estudiante") public Estudiante estudiante;

    public static class Docente {
        @SerializedName("codigoDocente") public String codigoDocente;
        @SerializedName("telefono") public String telefono;
        @SerializedName("especialidad") public String especialidad;
    }

    public static class Estudiante {
        @SerializedName("codigoEstudiantil") public String codigoEstudiantil;
        @SerializedName("numeroIdentificacion") public String numeroIdentificacion;
        @SerializedName("fechaNacimiento") public String fechaNacimiento;
        @SerializedName("telefono") public String telefono;
        @SerializedName("direccion") public String direccion;
    }
}
