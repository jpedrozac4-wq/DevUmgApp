package gt.com.ro.devumgapp.curso.network;

import java.util.List;

import gt.com.ro.devumgapp.carrera.dto.EstadoRequest;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.curso.dto.CursoRequest;
import gt.com.ro.devumgapp.curso.dto.CursoResponse;
import gt.com.ro.devumgapp.curso.dto.CursoResumenResponse;
import gt.com.ro.devumgapp.curso.dto.DocenteRequest;
import gt.com.ro.devumgapp.curso.dto.DocenteResumenResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CursoApiService {

    @POST("api/cursos")
    Call<CursoResponse> crearCurso(@Body CursoRequest request);

    @GET("api/cursos")
    Call<PageResponse<CursoResponse>> listarCursos(
            @Query("texto") String texto,
            @Query("carreraId") Long carreraId,
            @Query("docenteId") Long docenteId,
            @Query("cicloAnio") Integer cicloAnio,
            @Query("activo") Boolean activo,
            @Query("page") int page,
            @Query("size") int size);

    @GET("api/cursos/{id}")
    Call<CursoResponse> obtenerCurso(@Path("id") long id);

    @PUT("api/cursos/{id}")
    Call<CursoResponse> actualizarCurso(@Path("id") long id, @Body CursoRequest request);

    @PATCH("api/cursos/{id}/estado")
    Call<CursoResponse> cambiarEstado(@Path("id") long id, @Body EstadoRequest request);

    @PATCH("api/cursos/{id}/docente")
    Call<CursoResponse> asignarDocente(@Path("id") long id, @Body DocenteRequest request);

    @DELETE("api/cursos/{id}/docente")
    Call<CursoResponse> quitarDocente(@Path("id") long id);

    @GET("api/cursos/{id}/docente")
    Call<DocenteResumenResponse> obtenerDocenteDelCurso(@Path("id") long id);

    @GET("api/cursos/docente/{docenteId}")
    Call<List<CursoResponse>> listarCursosPorDocente(@Path("docenteId") long docenteId);

    @GET("api/cursos/carrera/{carreraId}")
    Call<List<CursoResponse>> listarCursosPorCarrera(@Path("carreraId") long carreraId);

    @GET("api/cursos/activos")
    Call<List<CursoResponse>> listarCursosActivos();

    @GET("api/cursos/nombres-activos")
    Call<List<CursoResumenResponse>> listarNombresActivos();

    @GET("api/docentes/activos")
    Call<List<DocenteResumenResponse>> listarDocentesActivos();
}
