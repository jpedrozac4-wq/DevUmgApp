package gt.com.ro.devumgapp.inscripcion.dto;

import com.google.gson.annotations.SerializedName;

public class InscripcionUpdateRequest {

    @SerializedName("carreraId")
    public long carreraId;

    @SerializedName("cursoId")
    public Long cursoId;

    @SerializedName("grado")
    public String grado;

    @SerializedName("seccion")
    public String seccion;

    @SerializedName("cicloAnio")
    public int cicloAnio;

    @SerializedName("observaciones")
    public String observaciones;

    public InscripcionUpdateRequest() {
    }

    public InscripcionUpdateRequest(
            long carreraId,
            Long cursoId,
            String grado,
            String seccion,
            int cicloAnio,
            String observaciones) {
        this.carreraId = carreraId;
        this.cursoId = cursoId;
        this.grado = grado;
        this.seccion = seccion;
        this.cicloAnio = cicloAnio;
        this.observaciones = observaciones;
    }
}
