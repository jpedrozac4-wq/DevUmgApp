package gt.com.ro.devumgapp.estudiante.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EstadoGeneralResponse {
    @SerializedName("estudianteId") public long estudianteId;
    @SerializedName("nombreCompleto") public String nombreCompleto;
    @SerializedName("correo") public String correo;
    @SerializedName("codigoEstudiantil") public String codigoEstudiantil;
    @SerializedName("activo") public boolean activo;
    @SerializedName("fechaRegistro") public String fechaRegistro;
    @SerializedName("estadoGeneral") public String estadoGeneral;
    @SerializedName("resumenAcademico") public List<Object> resumenAcademico;
    @SerializedName("resumenFinanciero") public List<Object> resumenFinanciero;
    @SerializedName("detalleAcademico") public List<Object> detalleAcademico;
    @SerializedName("detalleFinanciero") public List<Object> detalleFinanciero;
}
