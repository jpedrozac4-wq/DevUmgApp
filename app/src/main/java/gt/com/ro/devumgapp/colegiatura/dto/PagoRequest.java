package gt.com.ro.devumgapp.colegiatura.dto;

import com.google.gson.annotations.SerializedName;

public class PagoRequest {
    @SerializedName("montoPago") public double montoPago;
    @SerializedName("fechaPago") public String fechaPago;
    @SerializedName("observaciones") public String observaciones;

    public PagoRequest(double montoPago, String fechaPago, String observaciones) {
        this.montoPago = montoPago;
        this.fechaPago = fechaPago;
        this.observaciones = observaciones;
    }
}
