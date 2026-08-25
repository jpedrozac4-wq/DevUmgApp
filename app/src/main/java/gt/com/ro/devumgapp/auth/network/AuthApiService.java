package gt.com.ro.devumgapp.auth.network;

import gt.com.ro.devumgapp.auth.dto.LoginRequest;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/** REST endpoints of the auth module. */
public interface AuthApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
}
