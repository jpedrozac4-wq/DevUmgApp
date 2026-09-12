package gt.com.ro.devumgapp.colegiatura.dto;

import com.google.gson.annotations.SerializedName;

public class ColegiaturaResponse {
    @SerializedName("id") public long id;
    @SerializedName("estudianteId") public long estudianteId;
    @SerializedName("cicloAnio") public int cicloAnio;
    @SerializedName("concepto") public String concepto;
    @SerializedName("montoTotal") public double montoTotal;
    @SerializedName("montoPagado") public double montoPagado;
    @SerializedName("saldoPendiente") public double saldoPendiente;
    @SerializedName("fechaEmision") public String fechaEmision;
    @SerializedName("fechaVencimiento") public String fechaVencimiento;
    @SerializedName("estado") public String estado;
    @SerializedName("activo") public boolean activo;
    @SerializedName("fechaCreacion") public String fechaCreacion;
    @SerializedName("fechaActualizacion") public String fechaActualizacion;
}
