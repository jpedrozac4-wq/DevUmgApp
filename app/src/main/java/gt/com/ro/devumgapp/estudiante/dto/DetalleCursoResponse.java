package gt.com.ro.devumgapp.estudiante.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DetalleCursoResponse {
    @SerializedName("inscripcionId") public Long inscripcionId;
    @SerializedName("cursoId") public Long cursoId;
    @SerializedName("codigoCurso") public String codigoCurso;
    @SerializedName("nombreCurso") public String nombreCurso;
    @SerializedName("carreraId") public Long carreraId;
    @SerializedName("cicloAnio") public Integer cicloAnio;
    @SerializedName("grado") public String grado;
    @SerializedName("seccion") public String seccion;
    @SerializedName("estadoInscripcion") public String estadoInscripcion;
    @SerializedName("inscripcionActiva") public Boolean inscripcionActiva;
    @SerializedName("cursoActivo") public Boolean cursoActivo;
    @SerializedName("promedioCurso") public Double promedioCurso;
    @SerializedName("resultado") public String resultado;
    @SerializedName("notas") public List<NotaDetalleResponse> notas;
}
