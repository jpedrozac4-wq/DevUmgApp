package gt.com.ro.devumgapp.nota.dto;

import com.google.gson.annotations.SerializedName;

public class NotaRequest {

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

    public NotaRequest() {
    }

    public NotaRequest(
            long estudianteId,
            long cursoId,
            int cicloAnio,
            String tipoEvaluacion,
            double calificacion,
            String observaciones) {
        this.estudianteId = estudianteId;
        this.cursoId = cursoId;
        this.cicloAnio = cicloAnio;
        this.tipoEvaluacion = tipoEvaluacion;
        this.calificacion = calificacion;
        this.observaciones = observaciones;
    }
}
