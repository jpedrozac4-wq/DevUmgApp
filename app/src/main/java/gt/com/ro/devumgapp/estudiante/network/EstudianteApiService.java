package gt.com.ro.devumgapp.estudiante.network;

import java.util.List;

import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface EstudianteApiService {

    @GET("api/estudiantes/activos")
    Call<List<EstudianteResumenResponse>> listarEstudiantesActivos();

    @GET("api/estudiantes/{id}")
    Call<EstudianteResumenResponse> obtenerEstudiante(@Path("id") long id);
}
