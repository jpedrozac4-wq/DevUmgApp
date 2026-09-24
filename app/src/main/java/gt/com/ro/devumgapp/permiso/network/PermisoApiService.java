package gt.com.ro.devumgapp.permiso.network;

import java.util.List;
import gt.com.ro.devumgapp.permiso.dto.PermisoRequest;
import gt.com.ro.devumgapp.permiso.dto.PermisoResponse;
import gt.com.ro.devumgapp.permiso.dto.PermisoResumenResponse;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.dto.EstadoRequest;
import retrofit2.Call;
import retrofit2.http.*;

public interface PermisoApiService {

    @POST("/api/permisos")
    Call<PermisoResponse> crear(@Body PermisoRequest request);

    @GET("/api/permisos")
    Call<PageResponse<PermisoResponse>> listar(
            @Query("texto") String texto,
            @Query("activo") Boolean activo,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("/api/permisos/{id}")
    Call<PermisoResponse> obtener(@Path("id") long id);

    @PUT("/api/permisos/{id}")
    Call<PermisoResponse> actualizar(@Path("id") long id, @Body PermisoRequest request);

    @PATCH("/api/permisos/{id}/estado")
    Call<PermisoResponse> cambiarEstado(@Path("id") long id, @Body EstadoRequest estado);

    @GET("/api/permisos/activos")
    Call<List<PermisoResumenResponse>> listarActivos();
}
