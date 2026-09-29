package gt.com.ro.devumgapp.docente.dto;

import com.google.gson.annotations.SerializedName;

public class DocenteRequest {

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

    @SerializedName("accesoApp") public Boolean accesoApp;
    @SerializedName("usuarioId") public Long usuarioId;
    @SerializedName("username") public String username;
    @SerializedName("password") public String password;

    public DocenteRequest() { }

    public DocenteRequest(
            String codigoDocente,
            String nombre,
            String apellido,
            String email,
            String telefono,
            String especialidad) {
        this.codigoDocente = codigoDocente;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.especialidad = especialidad;
    }

    public DocenteRequest withAppAccess(String username, String password) {
        this.accesoApp = true;
        this.username = username;
        this.password = password;
        return this;
    }
}
