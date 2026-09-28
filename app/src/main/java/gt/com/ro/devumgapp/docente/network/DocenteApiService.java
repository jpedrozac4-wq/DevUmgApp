package gt.com.ro.devumgapp.docente.network;

import gt.com.ro.devumgapp.carrera.dto.EstadoRequest;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.docente.dto.DocenteRequest;
import gt.com.ro.devumgapp.docente.dto.DocenteResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DocenteApiService {

    @POST("api/docentes")
    Call<DocenteResponse> crearDocente(@Body DocenteRequest request);

    @GET("api/docentes")
    Call<PageResponse<DocenteResponse>> listarDocentes(
            @Query("busqueda") String busqueda,
            @Query("activo") Boolean activo,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/docentes/{id}")
    Call<DocenteResponse> obtenerDocente(@Path("id") long id);

    @PUT("api/docentes/{id}")
    Call<DocenteResponse> actualizarDocente(@Path("id") long id, @Body DocenteRequest request);

    @PATCH("api/docentes/{id}/estado")
    Call<DocenteResponse> cambiarEstado(@Path("id") long id, @Body EstadoRequest request);

}
