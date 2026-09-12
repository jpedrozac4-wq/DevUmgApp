package gt.com.ro.devumgapp.estudiante.network;

import java.util.List;

import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.estudiante.dto.EstadoRequest;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteRequest;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResponse;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.estudiante.dto.HistorialAcademicoResponse;
import gt.com.ro.devumgapp.estudiante.dto.EstadoGeneralResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EstudianteApiService {

    // =========================
    // Compatibilidad con Inscripciones
    // =========================

    @GET("api/estudiantes/activos")
    Call<List<EstudianteResumenResponse>> listarEstudiantesActivos();

    @GET("api/estudiantes/{id}")
    Call<EstudianteResumenResponse> obtenerEstudiante(@Path("id") long id);


    // =========================
    // CRUD de Estudiantes
    // =========================

    @POST("api/estudiantes")
    Call<EstudianteResponse> crearEstudiante(
            @Body EstudianteRequest request
    );

    @GET("api/estudiantes")
    Call<PageResponse<EstudianteResponse>> listarEstudiantes(
            @Query("texto") String texto,
            @Query("activo") Boolean activo,
            @Query("pagina") int pagina,
            @Query("tamanio") int tamanio
    );

    /*
     * Mismo endpoint /{id}, pero para las pantallas de Estudiante
     * necesitamos la respuesta completa.
     */
    @GET("api/estudiantes/{id}")
    Call<EstudianteResponse> obtenerEstudianteDetalle(
            @Path("id") long id
    );

    @PUT("api/estudiantes/{id}")
    Call<EstudianteResponse> actualizarEstudiante(
            @Path("id") long id,
            @Body EstudianteRequest request
    );

    @PATCH("api/estudiantes/{id}/estado")
    Call<EstudianteResponse> cambiarEstadoEstudiante(
            @Path("id") long id,
            @Body EstadoRequest request
    );

    @GET("api/estudiantes/{id}/resumen")
    Call<EstudianteResumenResponse> obtenerResumen(
            @Path("id") long id
    );

    @GET("api/estudiantes/{id}/historial")
    Call<HistorialAcademicoResponse> obtenerHistorialAcademico(
            @Path("id") long id
    );

    @GET("api/estudiantes/{id}/estado-general")
    Call<EstadoGeneralResponse> obtenerEstadoGeneral(
            @Path("id") long id
    );
}