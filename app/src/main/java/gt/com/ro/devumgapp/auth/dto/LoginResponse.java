package gt.com.ro.devumgapp.auth.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Successful response of POST api/auth/login. */
public class LoginResponse {

    @SerializedName("accessToken")
    public String accessToken;

    @SerializedName("tokenType")
    public String tokenType;

    @SerializedName("expiresIn")
    public long expiresIn;

    @SerializedName(value = "usuarioId", alternate = {"id"})
    public long usuarioId;

    @SerializedName("docenteId")
    public Long docenteId;

    @SerializedName("estudianteId")
    public Long estudianteId;

    @SerializedName("username")
    public String username;

    @SerializedName("email")
    public String email;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("apellido")
    public String apellido;

    @SerializedName("roles")
    public List<String> roles;

    @SerializedName("permisos")
    public List<String> permisos;
}
