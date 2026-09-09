package gt.com.ro.devumgapp.carrera.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.auth.dto.ApiError;
import retrofit2.Response;

final class CarreraErrorMapper {

    private CarreraErrorMapper() {
    }

    static String fromResponse(Context context, Response<?> response) {
        String backendMessage = parseBackendMessage(response);
        if (backendMessage != null && !backendMessage.trim().isEmpty()) {
            return backendMessage;
        }

        if (response.code() == 400) {
            return context.getString(R.string.carrera_error_bad_request);
        }
        if (response.code() == 404) {
            return context.getString(R.string.carrera_error_not_found);
        }
        if (response.code() == 409) {
            return context.getString(R.string.carrera_error_conflict);
        }
        return context.getString(R.string.carrera_error_server);
    }

    static String fromFailure(Context context, Throwable throwable) {
        if (!hasInternetConnection(context) || throwable instanceof UnknownHostException) {
            return context.getString(R.string.carrera_error_no_internet);
        }
        if (throwable instanceof SocketTimeoutException) {
            return context.getString(R.string.carrera_error_timeout);
        }
        return context.getString(R.string.carrera_error_network);
    }

    private static String parseBackendMessage(Response<?> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            ApiError apiError = new Gson().fromJson(response.errorBody().string(), ApiError.class);
            return apiError == null ? null : apiError.message;
        } catch (IOException | JsonSyntaxException exception) {
            return null;
        }
    }

    private static boolean hasInternetConnection(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || connectivityManager.getActiveNetwork() == null) {
            return false;
        }
        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
