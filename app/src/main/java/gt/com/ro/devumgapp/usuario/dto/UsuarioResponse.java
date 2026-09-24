package gt.com.ro.devumgapp.usuario.dto;

import java.util.List;
import gt.com.ro.devumgapp.rol.dto.RolResumenResponse;

public class UsuarioResponse {
    public long id; // Solo lectura
    public String username;
    public String email;
    public String nombre;
    public String apellido;
    public List<RolResumenResponse> roles;
    public boolean activo;
    public String fechaCreacion; // Solo lectura
}