package gt.com.ro.devumgapp.rol.network;

import java.util.List;
import gt.com.ro.devumgapp.rol.dto.RolRequest;
import gt.com.ro.devumgapp.rol.dto.RolResponse;
import gt.com.ro.devumgapp.rol.dto.PermisosRequest;
import gt.com.ro.devumgapp.permiso.dto.PermisoResumenResponse;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.dto.EstadoRequest;
import retrofit2.Call;
import retrofit2.http.*;

public interface RolApiService {

    @POST("/api/roles")
    Call<RolResponse> crear(@Body RolRequest request);

    @GET("/api/roles")
    Call<PageResponse<RolResponse>> listar(
            @Query("texto") String texto,
            @Query("activo") Boolean activo,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("/api/roles/{id}")
    Call<RolResponse> obtener(@Path("id") long id);

    @PUT("/api/roles/{id}")
    Call<RolResponse> actualizar(@Path("id") long id, @Body RolRequest request);

    @PATCH("/api/roles/{id}/estado")
    Call<RolResponse> cambiarEstado(@Path("id") long id, @Body EstadoRequest estado);

    @PUT("/api/roles/{id}/permisos")
    Call<RolResponse> asignarPermisos(@Path("id") long id, @Body PermisosRequest request);

    @GET("/api/roles/{id}/permisos")
    Call<List<PermisoResumenResponse>> listarPermisos(@Path("id") long id);
}
