package gt.com.ro.devumgapp.estudiante.dto;

import com.google.gson.annotations.SerializedName;

public class NotaDetalleResponse {
    @SerializedName("notaId") public Long notaId;
    @SerializedName("tipoEvaluacion") public String tipoEvaluacion;
    @SerializedName("calificacion") public Double calificacion;
    @SerializedName("observaciones") public String observaciones;
    @SerializedName("activo") public Boolean activo;
}
