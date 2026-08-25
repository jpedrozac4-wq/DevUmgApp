package gt.com.ro.devumgapp.core.network;

import gt.com.ro.devumgapp.core.session.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit instance shared by the whole app.
 * Every request carries JSON headers and, when a session exists,
 * the Authorization Bearer token.
 */
public class RetrofitClient {

    private static Retrofit retrofit;

    private RetrofitClient() {
        // Singleton: no instances.
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(chain -> {
                        Request.Builder builder = chain.request().newBuilder()
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json");

                        String token = SessionManager.getInstance().getToken();
                        if (!token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                        return chain.proceed(builder.build());
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
