package gt.com.ro.devumgapp.nota.dto;

import com.google.gson.annotations.SerializedName;

public class PromedioResponse {

    @SerializedName("estudianteId")
    public long estudianteId;

    @SerializedName("promedioGeneral")
    public double promedioGeneral;

    @SerializedName("cantidadNotas")
    public int cantidadNotas;
}
