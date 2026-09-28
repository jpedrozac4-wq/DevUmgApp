package gt.com.ro.devumgapp.auth.network;

import gt.com.ro.devumgapp.auth.dto.LoginRequest;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;

/** REST endpoints of the auth module. */
public interface AuthApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("api/auth/me")
    Call<LoginResponse> me();
}
