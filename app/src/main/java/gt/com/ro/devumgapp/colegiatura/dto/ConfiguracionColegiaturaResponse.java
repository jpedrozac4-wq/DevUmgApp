package gt.com.ro.devumgapp.colegiatura.dto;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class ConfiguracionColegiaturaResponse {
    @SerializedName("carreraId") public long carreraId;
    @SerializedName("carreraNombre") public String carreraNombre;
    @SerializedName("mensualidad") public BigDecimal mensualidad;
    @SerializedName("cicloAnio") public int cicloAnio;
    @SerializedName("cicloId") public long cicloId;
    @SerializedName("cicloNombre") public String cicloNombre;
    @SerializedName("diaVencimiento") public Integer diaVencimiento;
    @SerializedName("fechaEmision") public String fechaEmision;
}
