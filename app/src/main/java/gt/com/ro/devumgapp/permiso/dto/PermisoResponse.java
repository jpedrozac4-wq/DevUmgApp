package gt.com.ro.devumgapp.permiso.dto;

public class PermisoResponse {
    public long id; // Solo lectura
    public String codigo;
    public String nombre;
    public String descripcion;
    public boolean activo;
    public String fechaCreacion;     // Solo lectura
    public String fechaActualizacion; // Solo lectura
}