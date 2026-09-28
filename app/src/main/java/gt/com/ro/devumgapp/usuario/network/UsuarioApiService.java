package gt.com.ro.devumgapp.usuario.network;

import java.util.List;
import gt.com.ro.devumgapp.usuario.dto.*;
import gt.com.ro.devumgapp.rol.dto.RolResumenResponse;
import retrofit2.Call;
import retrofit2.http.*;

public interface UsuarioApiService {
    @POST("/api/usuarios")
    Call<UsuarioResponse> crear(@Body UsuarioRequest request);

    @GET("/api/usuarios")
    Call<List<UsuarioResponse>> listar();

    @GET("/api/usuarios/{id}")
    Call<UsuarioResponse> obtener(@Path("id") long id);

    @PUT("/api/usuarios/{id}")
    Call<UsuarioResponse> actualizar(@Path("id") long id, @Body UsuarioRequest request);

    @DELETE("/api/usuarios/{id}")
    Call<Void> eliminar(@Path("id") long id);

    @PUT("/api/usuarios/{id}/roles")
    Call<UsuarioResponse> asignarRoles(@Path("id") long id, @Body RolesRequest request);

    @GET("/api/usuarios/{id}/roles")
    Call<List<RolResumenResponse>> listarRoles(@Path("id") long id);
}