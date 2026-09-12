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
}
