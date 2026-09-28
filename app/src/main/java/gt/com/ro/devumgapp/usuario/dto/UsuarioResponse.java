package gt.com.ro.devumgapp.usuario.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import gt.com.ro.devumgapp.rol.dto.RolResumenResponse;

public class UsuarioResponse {

    @SerializedName("id")
    public long id; // Solo lectura

    @SerializedName("username")
    public String username;

    @SerializedName("email")
    public String email;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("apellido")
    public String apellido;

    @SerializedName("roles")
    public List<RolResumenResponse> roles;

    @SerializedName("activo")
    public boolean activo;

    @SerializedName("fechaCreacion")
    public String fechaCreacion; // Solo lectura

    public UsuarioResponse() {
    }

    public UsuarioResponse(
            long id,
            String username,
            String email,
            String nombre,
            String apellido,
            List<RolResumenResponse> roles,
            boolean activo,
            String fechaCreacion) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
        this.roles = roles;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
    }
}
