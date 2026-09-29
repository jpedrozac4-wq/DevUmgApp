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

    @SerializedName("identidadFuente") public String identidadFuente;
    @SerializedName("usuarioId") public Long usuarioId;
    @SerializedName("accesoApp") public boolean accesoApp;

    public String getDisplayName() {
        String codigo = codigoEstudiantil == null ? "" : codigoEstudiantil;
        String nombre = ((nombres == null ? "" : nombres) + " " +
                (apellidos == null ? "" : apellidos)).trim();

        String display = codigo.isEmpty() ? nombre : nombre.isEmpty() ? codigo : codigo + " - " + nombre;
        String source = getIdentitySourceLabel();
        return source.isEmpty() ? display : display + " · " + source;
    }

    public String getIdentitySourceLabel() {
        if ("USUARIO".equalsIgnoreCase(identidadFuente)) return "Usuario";
        if ("PERFIL_HISTORICO".equalsIgnoreCase(identidadFuente)) return "Perfil histórico";
        return "";
    }
}
