package gt.com.ro.devumgapp.usuario.dto;

public class UsuarioRequest {
    public String username;
    public String password; // Solo se envía, nunca se guarda ni se muestra
    public String email;
    public String nombre;
    public String apellido;
}