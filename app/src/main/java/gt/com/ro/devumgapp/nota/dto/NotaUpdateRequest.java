package gt.com.ro.devumgapp.nota.dto;

import com.google.gson.annotations.SerializedName;

public class NotaUpdateRequest {

    @SerializedName("tipoEvaluacion")
    public String tipoEvaluacion;

    @SerializedName("calificacion")
    public double calificacion;

    @SerializedName("observaciones")
    public String observaciones;

    public NotaUpdateRequest() {
    }

    public NotaUpdateRequest(
            String tipoEvaluacion,
            double calificacion,
            String observaciones) {
        this.tipoEvaluacion = tipoEvaluacion;
        this.calificacion = calificacion;
        this.observaciones = observaciones;
    }
}
