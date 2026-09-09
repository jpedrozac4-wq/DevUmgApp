package gt.com.ro.devumgapp.carrera.network;

import java.util.List;

import gt.com.ro.devumgapp.carrera.dto.CarreraRequest;
import gt.com.ro.devumgapp.carrera.dto.CarreraResponse;
import gt.com.ro.devumgapp.carrera.dto.CarreraResumenResponse;
import gt.com.ro.devumgapp.carrera.dto.EstadoRequest;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CarreraApiService {

    @POST("api/carreras")
    Call<CarreraResponse> crearCarrera(@Body CarreraRequest request);

    @GET("api/carreras")
    Call<PageResponse<CarreraResponse>> listarCarreras(
            @Query("texto") String texto,
            @Query("activo") Boolean activo,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/carreras/{id}")
    Call<CarreraResponse> obtenerCarrera(@Path("id") long id);

    @PUT("api/carreras/{id}")
    Call<CarreraResponse> actualizarCarrera(
            @Path("id") long id,
            @Body CarreraRequest request);

    @PATCH("api/carreras/{id}/estado")
    Call<CarreraResponse> cambiarEstado(
            @Path("id") long id,
            @Body EstadoRequest request);

    @GET("api/carreras/activas")
    Call<List<CarreraResponse>> listarCarrerasActivas();

    @GET("api/carreras/nombres-activos")
    Call<List<CarreraResumenResponse>> listarNombresActivos();
}
