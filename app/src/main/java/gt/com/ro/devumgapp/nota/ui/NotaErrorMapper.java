package gt.com.ro.devumgapp.nota.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.network.ApiCallLogger;
import gt.com.ro.devumgapp.core.network.ApiResponses;
import retrofit2.Response;

final class NotaErrorMapper {

    private static final String TAG = "NotaApi";

    private NotaErrorMapper() {
    }

    static String fromResponse(Context context, Response<?> response) {
        ApiCallLogger.logResponse(TAG, response);
        String authMessage = ApiResponses.authorizationMessage(context, response);
        if (authMessage != null) return authMessage;
        String errorBody = ApiCallLogger.readAndLogErrorBody(TAG, response);
        if (errorBody != null && !errorBody.trim().isEmpty()) {
            return errorBody.trim();
        }

        if (response.code() == 400) {
            return context.getString(R.string.nota_error_bad_request);
        }
        if (response.code() == 401) {
            return context.getString(R.string.nota_error_unauthorized);
        }
        if (response.code() == 403) {
            return context.getString(R.string.nota_error_forbidden);
        }
        if (response.code() == 404) {
            return context.getString(R.string.nota_error_not_found);
        }
        if (response.code() == 409) {
            return context.getString(R.string.nota_error_conflict);
        }
        if (response.code() == 500) {
            return context.getString(R.string.nota_error_internal_server);
        }
        return context.getString(R.string.nota_error_server);
    }

    static String fromFailure(Context context, Throwable throwable) {
        ApiCallLogger.logFailure(TAG, throwable);
        if (!hasInternetConnection(context) || throwable instanceof UnknownHostException) {
            return context.getString(R.string.nota_error_no_internet);
        }
        if (throwable instanceof SocketTimeoutException) {
            return context.getString(R.string.nota_error_timeout);
        }
        return context.getString(R.string.nota_error_network);
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
