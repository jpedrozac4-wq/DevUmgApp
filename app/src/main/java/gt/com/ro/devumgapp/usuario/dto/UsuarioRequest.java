package gt.com.ro.devumgapp.usuario.dto;

import com.google.gson.annotations.SerializedName;

public class UsuarioRequest {

    @SerializedName("username")
    public String username;

    @SerializedName("password")
    public String password; // Solo se envía, nunca se guarda ni se muestra

    @SerializedName("email")
    public String email;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("apellido")
    public String apellido;

    public UsuarioRequest(
            String username,
            String password,
            String email,
            String nombre,
            String apellido) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
    }
}
