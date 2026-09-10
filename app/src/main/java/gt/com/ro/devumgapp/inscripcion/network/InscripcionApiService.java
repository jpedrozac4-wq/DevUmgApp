package gt.com.ro.devumgapp.inscripcion.network;

import java.util.List;

import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.inscripcion.dto.AnulacionRequest;
import gt.com.ro.devumgapp.inscripcion.dto.InscripcionRequest;
import gt.com.ro.devumgapp.inscripcion.dto.InscripcionResponse;
import gt.com.ro.devumgapp.inscripcion.dto.InscripcionUpdateRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface InscripcionApiService {

    @POST("api/inscripciones")
    Call<InscripcionResponse> crearInscripcion(@Body InscripcionRequest request);

    @GET("api/inscripciones")
    Call<PageResponse<InscripcionResponse>> listarInscripciones(
            @Query("estudianteId") Long estudianteId,
            @Query("carreraId") Long carreraId,
            @Query("cursoId") Long cursoId,
            @Query("cicloAnio") Integer cicloAnio,
            @Query("grado") String grado,
            @Query("seccion") String seccion,
            @Query("estado") String estado,
            @Query("activo") Boolean activo,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/inscripciones/{id}")
    Call<InscripcionResponse> obtenerInscripcion(@Path("id") long id);

    @PUT("api/inscripciones/{id}")
    Call<InscripcionResponse> actualizarInscripcion(
            @Path("id") long id,
            @Body InscripcionUpdateRequest request);

    @PATCH("api/inscripciones/{id}/estado")
    Call<InscripcionResponse> anularInscripcion(
            @Path("id") long id,
            @Body AnulacionRequest request);

    @PATCH("api/inscripciones/{id}/reactivar")
    Call<InscripcionResponse> reactivarInscripcion(@Path("id") long id);

    @GET("api/inscripciones/activas")
    Call<List<InscripcionResponse>> listarInscripcionesActivas();
}
