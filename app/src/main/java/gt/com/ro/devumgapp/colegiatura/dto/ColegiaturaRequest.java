package gt.com.ro.devumgapp.colegiatura.dto;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class ColegiaturaRequest {
    @SerializedName("estudianteId") public long estudianteId;
    @SerializedName("cicloAnio") public int cicloAnio;
    @SerializedName("concepto") public String concepto;
    @SerializedName("montoTotal") public BigDecimal montoTotal;
    @SerializedName("fechaEmision") public String fechaEmision;
    @SerializedName("fechaVencimiento") public String fechaVencimiento;

    public ColegiaturaRequest(long estudianteId, int cicloAnio, String concepto,
                              BigDecimal montoTotal, String fechaEmision, String fechaVencimiento) {
        this.estudianteId = estudianteId;
        this.cicloAnio = cicloAnio;
        this.concepto = concepto;
        this.montoTotal = montoTotal;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
    }
}
