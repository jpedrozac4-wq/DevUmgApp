package gt.com.ro.devumgapp.curso.dto;

import com.google.gson.annotations.SerializedName;

public class CursoRequest {

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

    @SerializedName("cicloAnio")
    public int cicloAnio;

    public CursoRequest(
            String codigo,
            String nombre,
            String descripcion,
            int creditos,
            int horasSemanales,
            long carreraId,
            int cicloAnio) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creditos = creditos;
        this.horasSemanales = horasSemanales;
        this.carreraId = carreraId;
        this.cicloAnio = cicloAnio;
    }
}
