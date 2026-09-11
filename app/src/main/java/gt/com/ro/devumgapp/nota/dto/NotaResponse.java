package gt.com.ro.devumgapp.nota.dto;

import com.google.gson.annotations.SerializedName;

public class NotaResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("estudianteId")
    public long estudianteId;

    @SerializedName("cursoId")
    public long cursoId;

    @SerializedName("cicloAnio")
    public int cicloAnio;

    @SerializedName("tipoEvaluacion")
    public String tipoEvaluacion;

    @SerializedName("calificacion")
    public double calificacion;

    @SerializedName("observaciones")
    public String observaciones;

    @SerializedName("activo")
    public boolean activo;

    @SerializedName("fechaCreacion")
    public String fechaCreacion;

    @SerializedName("fechaActualizacion")
    public String fechaActualizacion;
}
