package gt.com.ro.devumgapp.inscripcion.dto;

import com.google.gson.annotations.SerializedName;

public class InscripcionResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("estudianteId")
    public long estudianteId;

    @SerializedName("carreraId")
    public long carreraId;

    @SerializedName("cursoId")
    public Long cursoId;

    @SerializedName("grado")
    public String grado;

    @SerializedName("seccion")
    public String seccion;

    @SerializedName("cicloAnio")
    public int cicloAnio;

    @SerializedName("fechaInscripcion")
    public String fechaInscripcion;

    @SerializedName("estado")
    public String estado;

    @SerializedName("observaciones")
    public String observaciones;

    @SerializedName("activo")
    public boolean activo;

    @SerializedName("fechaCreacion")
    public String fechaCreacion;

    @SerializedName("fechaActualizacion")
    public String fechaActualizacion;
}
