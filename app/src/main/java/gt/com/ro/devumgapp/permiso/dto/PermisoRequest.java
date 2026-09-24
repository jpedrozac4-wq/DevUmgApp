package gt.com.ro.devumgapp.permiso.dto;

public class PermisoRequest {
    public String codigo;      // 3 a 80, patron A-Za-z0-9_
    public String nombre;      // 3 a 120
    public String descripcion; // Máximo 300
}