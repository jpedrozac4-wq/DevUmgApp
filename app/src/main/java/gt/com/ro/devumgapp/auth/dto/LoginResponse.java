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

    @SerializedName("usuarioId")
    public long usuarioId;

    @SerializedName("username")
    public String username;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("apellido")
    public String apellido;

    @SerializedName("roles")
    public List<String> roles;
}
