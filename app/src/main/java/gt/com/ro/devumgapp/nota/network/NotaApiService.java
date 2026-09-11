package gt.com.ro.devumgapp.nota.network;

import java.util.List;

import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.nota.dto.EstadoRequest;
import gt.com.ro.devumgapp.nota.dto.NotaRequest;
import gt.com.ro.devumgapp.nota.dto.NotaResponse;
import gt.com.ro.devumgapp.nota.dto.NotaUpdateRequest;
import gt.com.ro.devumgapp.nota.dto.PromedioResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotaApiService {

    @POST("api/notas")
    Call<NotaResponse> crearNota(@Body NotaRequest request);

    @GET("api/notas")
    Call<PageResponse<NotaResponse>> listarNotas(
            @Query("estudianteId") Long estudianteId,
            @Query("cursoId") Long cursoId,
            @Query("cicloAnio") Integer cicloAnio,
            @Query("tipoEvaluacion") String tipoEvaluacion,
            @Query("activo") Boolean activo,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/notas/estudiante/{id}")
    Call<PageResponse<NotaResponse>> listarNotasPorEstudiante(
            @Path("id") long id,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/notas/estudiante/{id}/activas")
    Call<PageResponse<NotaResponse>> listarNotasActivasPorEstudiante(
            @Path("id") long id,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/notas/estudiante/{id}/curso/{cursoId}")
    Call<PageResponse<NotaResponse>> listarNotasPorEstudianteYCurso(
            @Path("id") long id,
            @Path("cursoId") long cursoId,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/notas/estudiante/{id}/promedio")
    Call<PromedioResponse> obtenerPromedio(@Path("id") long id);

    @GET("api/notas/estudiante/{id}/calificaciones")
    Call<List<Double>> listarCalificaciones(@Path("id") long id);

    @GET("api/notas/curso/{cursoId}")
    Call<PageResponse<NotaResponse>> listarNotasPorCurso(
            @Path("cursoId") long cursoId,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/notas/{id}")
    Call<NotaResponse> obtenerNota(@Path("id") long id);

    @PUT("api/notas/{id}")
    Call<NotaResponse> actualizarNota(@Path("id") long id, @Body NotaUpdateRequest request);

    @PATCH("api/notas/{id}/estado")
    Call<NotaResponse> cambiarEstado(@Path("id") long id, @Body EstadoRequest request);
}
