package gt.com.ro.devumgapp.inscripcion.dto;

import com.google.gson.annotations.SerializedName;

public class AnulacionRequest {

    @SerializedName("motivo")
    public String motivo;

    public AnulacionRequest() {
    }

    public AnulacionRequest(String motivo) {
        this.motivo = motivo;
    }
}
