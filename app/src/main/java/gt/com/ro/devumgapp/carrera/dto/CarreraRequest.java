package gt.com.ro.devumgapp.carrera.dto;

import com.google.gson.annotations.SerializedName;

public class CarreraRequest {

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

    public CarreraRequest(String codigo, String nombre, String descripcion, int duracionAnios, java.math.BigDecimal mensualidad, Integer cantidadCuotas, Integer diaVencimiento) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionAnios = duracionAnios;
        this.mensualidad = mensualidad; this.cantidadCuotas = cantidadCuotas; this.diaVencimiento = diaVencimiento;
    }
}
