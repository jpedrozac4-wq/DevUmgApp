package gt.com.ro.devumgapp.academico.network;

import com.google.gson.JsonElement;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/** Consultas acotadas a la identidad académica contenida en el JWT. */
public interface AcademicoApiService {
    @GET("api/academico/docente/me") Call<JsonElement> docenteMe();
    @GET("api/academico/docente/me/cursos") Call<JsonElement> cursosDocente();
    @GET("api/academico/docente/me/cursos/{cursoId}/estudiantes")
    Call<JsonElement> estudiantesCurso(@Path("cursoId") long cursoId);
    @GET("api/academico/docente/me/cursos/{cursoId}/notas")
    Call<JsonElement> notasCurso(@Path("cursoId") long cursoId);

    @GET("api/academico/estudiante/me") Call<JsonElement> estudianteMe();
    @GET("api/academico/estudiante/me/inscripciones") Call<JsonElement> inscripcionesEstudiante();
    @GET("api/academico/estudiante/me/cursos") Call<JsonElement> cursosEstudiante();
    @GET("api/academico/estudiante/me/notas") Call<JsonElement> notasEstudiante();
    @GET("api/academico/estudiante/me/promedio") Call<JsonElement> promedioEstudiante();
    @GET("api/academico/estudiante/me/colegiaturas") Call<JsonElement> colegiaturasEstudiante();
    @GET("api/academico/estudiante/me/estado-cuenta") Call<JsonElement> estadoCuentaEstudiante();
}
