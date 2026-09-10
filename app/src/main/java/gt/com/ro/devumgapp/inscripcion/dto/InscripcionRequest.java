package gt.com.ro.devumgapp.inscripcion.dto;

import com.google.gson.annotations.SerializedName;

public class InscripcionRequest {

    @SerializedName("estudianteId")
    public long estudianteId;

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

    @SerializedName("fechaInscripcion")
    public String fechaInscripcion;

    @SerializedName("observaciones")
    public String observaciones;

    public InscripcionRequest() {
    }

    public InscripcionRequest(
            long estudianteId,
            long carreraId,
            Long cursoId,
            String grado,
            String seccion,
            int cicloAnio,
            String fechaInscripcion,
            String observaciones) {
        this.estudianteId = estudianteId;
        this.carreraId = carreraId;
        this.cursoId = cursoId;
        this.grado = grado;
        this.seccion = seccion;
        this.cicloAnio = cicloAnio;
        this.fechaInscripcion = fechaInscripcion;
        this.observaciones = observaciones;
    }
}
