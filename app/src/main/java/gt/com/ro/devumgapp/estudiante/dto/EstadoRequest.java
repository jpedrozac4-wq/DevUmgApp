package gt.com.ro.devumgapp.estudiante.dto;

import com.google.gson.annotations.SerializedName;

public class EstadoRequest {
    @SerializedName("activo")
    public boolean activo;

    public EstadoRequest(boolean activo) {
        this.activo = activo;
    }
}
