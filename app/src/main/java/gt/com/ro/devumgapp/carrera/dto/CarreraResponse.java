package gt.com.ro.devumgapp.carrera.dto;

import com.google.gson.annotations.SerializedName;

public class CarreraResponse {

    @SerializedName("id")
    public long id;

    @SerializedName("codigo")
    public String codigo;

    @SerializedName("nombre")
    public String nombre;

    @SerializedName("descripcion")
    public String descripcion;

    @SerializedName("duracionAnios")
    public int duracionAnios;
    @SerializedName("mensualidad") public java.math.BigDecimal mensualidad;
    @SerializedName("cantidadCuotas") public Integer cantidadCuotas;
    @SerializedName("diaVencimiento") public Integer diaVencimiento;

    @SerializedName("activo")
    public boolean activo;

    @SerializedName("fechaCreacion")
    public String fechaCreacion;

    @SerializedName("fechaActualizacion")
    public String fechaActualizacion;
}
