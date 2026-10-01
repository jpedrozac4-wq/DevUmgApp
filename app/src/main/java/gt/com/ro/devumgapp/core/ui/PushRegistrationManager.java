package gt.com.ro.devumgapp.notificacion;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;

import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Registra y desvincula el token FCM de este dispositivo contra el backend.
 * El token se registra al iniciar sesión y se desvincula al cerrarla.
 */
public final class PushRegistrationManager {

    private static final String TAG = "PushRegistration";

    private PushRegistrationManager() {
    }

    private static NotificationApi api() {
        return RetrofitClient.getClient().create(NotificationApi.class);
    }

    public static void register(Context context) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            Log.d(TAG, "register() omitido: no hay sesión activa.");
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "FCM token obtenido; registrando en el backend.");
                    NotificationModels.Device device = new NotificationModels.Device();
                    device.token = token;
                    api().register(device).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            Log.d(TAG, "Registro del token en el backend: HTTP " + response.code());
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable error) {
                            Log.w(TAG, "No se pudo registrar el token en el backend.", error);
                        }
                    });
                })
                .addOnFailureListener(error -> Log.w(TAG, "FCM no pudo entregar un token.", error));
    }

    public static void unregister(Context context, Runnable done) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> api().unregister(token).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        Log.d(TAG, "Token desvinculado: HTTP " + response.code());
                        done.run();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable error) {
                        Log.w(TAG, "No se pudo desvincular el token.", error);
                        done.run();
                    }
                }))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "FCM no pudo entregar un token para desvincular.", error);
                    done.run();
                });
    }
}
