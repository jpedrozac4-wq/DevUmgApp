package gt.com.ro.devumgapp.estudiante.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HistorialAcademicoResponse {
    @SerializedName("estudianteId") public long estudianteId;
    @SerializedName("nombreCompleto") public String nombreCompleto;
    @SerializedName("estudianteActivo") public boolean estudianteActivo;
    @SerializedName("promedioGeneral") public Double promedioGeneral;
    @SerializedName("totalCursos") public Integer totalCursos;
    @SerializedName("cursosAprobados") public Integer cursosAprobados;
    @SerializedName("cursosReprobados") public Integer cursosReprobados;
    @SerializedName("cursosSinCalificacion") public Integer cursosSinCalificacion;
    @SerializedName("detalleCursos") public List<DetalleCursoResponse> detalleCursos;
}
