package gt.com.ro.devumgapp.curso.dto;

import com.google.gson.annotations.SerializedName;

public class CursoResumenResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("carreraId")
    public long carreraId;

    @SerializedName("activo")
    public boolean activo;
}
