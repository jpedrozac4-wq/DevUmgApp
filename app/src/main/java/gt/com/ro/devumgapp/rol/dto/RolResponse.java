package gt.com.ro.devumgapp.rol.dto;

import java.util.List;
import gt.com.ro.devumgapp.permiso.dto.PermisoResumenResponse;

public class RolResponse {
    public long id; // Solo lectura
    public String codigo;
    public String nombre;
    public String descripcion;
    public boolean activo;
    public List<PermisoResumenResponse> permisos;
    public String fechaCreacion;     // Solo lectura
    public String fechaActualizacion; // Solo lectura
}