package gt.com.ro.devumgapp.curso.dto;

import com.google.gson.annotations.SerializedName;

public class CursoResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("descripcion")
    public String descripcion;

    @SerializedName("creditos")
    public int creditos;

    @SerializedName("horasSemanales")
    public int horasSemanales;

    @SerializedName("carreraId")
    public long carreraId;

    @SerializedName("docenteId")
    public Long docenteId;

    @SerializedName("cicloAnio")
    public int cicloAnio;

    @SerializedName("activo")
    public boolean activo;

    @SerializedName("fechaCreacion")
    public String fechaCreacion;

    @SerializedName("fechaActualizacion")
    public String fechaActualizacion;
}
