package gt.com.ro.devumgapp.colegiatura.network;

import java.util.List;

import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaRequest;
import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaResponse;
import gt.com.ro.devumgapp.colegiatura.dto.EstadoRequest;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.colegiatura.dto.PagoRequest;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ColegiaturaApiService {
    @POST("api/colegiaturas")
    Call<ColegiaturaResponse> crear(@Body ColegiaturaRequest request);

    @GET("api/colegiaturas")
    Call<PageResponse<ColegiaturaResponse>> listar(
            @Query("estudianteId") Long estudianteId,
            @Query("cicloAnio") Integer cicloAnio,
            @Query("estado") String estado,
            @Query("activo") Boolean activo,
            @Query("concepto") String concepto,
            @Query("pagina") int pagina,
            @Query("tamanio") int tamanio
    );

    @GET("api/colegiaturas/{id}")
    Call<ColegiaturaResponse> obtener(@Path("id") long id);

    @PUT("api/colegiaturas/{id}")
    Call<ColegiaturaResponse> actualizar(
            @Path("id") long id,
            @Body ColegiaturaRequest request
    );

    @PATCH("api/colegiaturas/{id}/pago")
    Call<ColegiaturaResponse> registrarPago(
            @Path("id") long id,
            @Body PagoRequest request
    );

    @PATCH("api/colegiaturas/{id}/estado")
    Call<ColegiaturaResponse> cambiarEstado(
            @Path("id") long id,
            @Body EstadoRequest request
    );

    @GET("api/estudiantes/activos")
    Call<List<EstudianteResumenResponse>> listarEstudiantesActivos();
}
