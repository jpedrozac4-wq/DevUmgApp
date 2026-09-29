package gt.com.ro.devumgapp.auth.dto;

/** Editable fields accepted by PUT api/auth/me. */
public class ProfileUpdateRequest {
    public final String username;
    public final String email;
    public final String nombre;
    public final String apellido;

    public ProfileUpdateRequest(String username, String email, String nombre, String apellido) {
        this.username = username;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
    }
}
