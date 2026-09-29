package gt.com.ro.devumgapp.auth.network;

import gt.com.ro.devumgapp.auth.dto.LoginRequest;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import gt.com.ro.devumgapp.auth.dto.PasswordChangeRequest;
import gt.com.ro.devumgapp.auth.dto.ProfileUpdateRequest;
import gt.com.ro.devumgapp.auth.dto.ProfileUpdateResponse;

/** REST endpoints of the auth module. */
public interface AuthApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("api/auth/me")
    Call<LoginResponse> me();

    @PUT("api/auth/me")
    Call<ProfileUpdateResponse> updateProfile(@Body ProfileUpdateRequest request);

    @PUT("api/auth/me/password")
    Call<Void> changePassword(@Body PasswordChangeRequest request);
}
