package gt.com.ro.devumgapp.academico.network;

import java.util.List;

import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Carrera;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Curso;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.DocenteCurso;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.DocentePerfil;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.EstudiantePerfil;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Inscripcion;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Nota;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.PlanCarrera;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Promedio;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.POST;
import retrofit2.http.Body;

/** Consultas acotadas a la identidad académica contenida en el JWT. */
public interface AcademicoApiService {
    @GET("api/academico/docente/me") Call<DocentePerfil> docenteMe();
    @GET("api/academico/docente/me/cursos") Call<List<Curso>> cursosDocente(@Query("cicloAnio") Integer cicloAnio);
    @GET("api/academico/docente/me/cursos/{cursoId}/estudiantes")
    Call<PageResponse<Inscripcion>> estudiantesCurso(@Path("cursoId") long cursoId, @Query("cicloAnio") Integer cicloAnio,
                                                      @Query("page") int page, @Query("size") int size);
    @GET("api/academico/docente/me/cursos/{cursoId}/notas")
    Call<PageResponse<Nota>> notasCurso(@Path("cursoId") long cursoId, @Query("cicloAnio") Integer cicloAnio,
                                        @Query("page") int page, @Query("size") int size);

    @GET("api/academico/estudiante/me") Call<EstudiantePerfil> estudianteMe();
    @GET("api/academico/estudiante/me/carrera") Call<Carrera> carreraEstudiante();
    @GET("api/academico/estudiante/me/plan-carrera") Call<PlanCarrera> planCarreraEstudiante();
    @GET("api/academico/estudiante/me/cursos-inscritos") Call<List<Curso>> cursosInscritosEstudiante();
    @GET("api/academico/estudiante/me/docentes") Call<List<DocenteCurso>> docentesEstudiante();
    @GET("api/academico/estudiante/me/inscripciones")
    Call<PageResponse<Inscripcion>> inscripcionesEstudiante(@Query("page") int page, @Query("size") int size);
    @GET("api/academico/estudiante/me/notas")
    Call<PageResponse<Nota>> notasEstudiante(@Query("page") int page, @Query("size") int size);
    @GET("api/academico/estudiante/me/notas")
    Call<PageResponse<Nota>> notasEstudianteFiltradas(@Query("cicloAnio") Integer cicloAnio, @Query("page") int page, @Query("size") int size);
    @GET("api/academico/estudiante/me/promedio") Call<Promedio> promedioEstudiante();
    @GET("api/academico/estudiante/me/colegiaturas")
    Call<PageResponse<gt.com.ro.devumgapp.academico.dto.AcademicDtos.Colegiatura>> colegiaturas(@Query("page") int page, @Query("size") int size);
    @GET("api/academico/estudiante/me/pagos")
    Call<List<gt.com.ro.devumgapp.academico.dto.AcademicDtos.Pago>> pagos();
    @POST("api/academico/estudiante/me/colegiaturas/{id}/pagos")
    Call<gt.com.ro.devumgapp.academico.dto.AcademicDtos.Pago> registrarPago(@Path("id") long id,
        @Body gt.com.ro.devumgapp.academico.dto.AcademicDtos.PagoRegistro request);
}
