package gt.com.ro.devumgapp.curso.ui;

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

final class CursoErrorMapper {

    private CursoErrorMapper() {
    }

    static String fromResponse(Context context, Response<?> response) {
        String message = parseBackendMessage(response);
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        if (response.code() == 400) {
            return context.getString(R.string.curso_error_bad_request);
        }
        if (response.code() == 404) {
            return context.getString(R.string.curso_error_not_found);
        }
        if (response.code() == 409) {
            return context.getString(R.string.curso_error_conflict);
        }
        return context.getString(R.string.curso_error_server);
    }

    static String fromFailure(Context context, Throwable throwable) {
        if (!hasInternetConnection(context) || throwable instanceof UnknownHostException) {
            return context.getString(R.string.curso_error_no_internet);
        }
        if (throwable instanceof SocketTimeoutException) {
            return context.getString(R.string.curso_error_timeout);
        }
        return context.getString(R.string.curso_error_network);
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
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null || manager.getActiveNetwork() == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
