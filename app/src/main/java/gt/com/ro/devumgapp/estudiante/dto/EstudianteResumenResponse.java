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

    public String getDisplayName() {
        String codigo = codigoEstudiantil == null ? "" : codigoEstudiantil;
        String nombre = ((nombres == null ? "" : nombres) + " " +
                (apellidos == null ? "" : apellidos)).trim();

        if (codigo.isEmpty()) return nombre;
        if (nombre.isEmpty()) return codigo;

        return codigo + " - " + nombre;
    }
}