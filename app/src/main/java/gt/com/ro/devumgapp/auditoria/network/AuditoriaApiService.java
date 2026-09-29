package gt.com.ro.devumgapp.auditoria.network;

import com.google.gson.JsonElement;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AuditoriaApiService {
    @GET("api/auditoria")
    Call<JsonElement> listar(
            @Query("fechaDesde") String fechaDesde,
            @Query("fechaHasta") String fechaHasta,
            @Query("usuario") String usuario,
            @Query("modulo") String modulo,
            @Query("accion") String accion,
            @Query("tipoEntidad") String tipoEntidad,
            @Query("entidadId") Long entidadId,
            @Query("page") int page,
            @Query("size") int size);
}
