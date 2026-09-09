package gt.com.ro.devumgapp.carrera.network;

import java.util.List;

import gt.com.ro.devumgapp.carrera.dto.CarreraResumenResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface CarreraApiService {

    @GET("api/carreras/activas")
    Call<List<CarreraResumenResponse>> listarCarrerasActivas();
}
