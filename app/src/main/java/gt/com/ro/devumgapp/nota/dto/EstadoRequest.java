package gt.com.ro.devumgapp.nota.dto;

import com.google.gson.annotations.SerializedName;

public class EstadoRequest {

    @SerializedName("activo")
    public boolean activo;

    public EstadoRequest() {
    }

    public EstadoRequest(boolean activo) {
        this.activo = activo;
    }
}
