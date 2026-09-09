package gt.com.ro.devumgapp.core.network;

import android.util.Log;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class ApiCallLogger {

    private static final long MAX_ERROR_BODY_BYTES = 8_192L;

    private ApiCallLogger() {
    }

    public static void logResponse(String tag, Response<?> response) {
        Log.d(tag, "HTTP " + response.code() + " " + response.raw().request().method()
                + " " + response.raw().request().url());
    }

    public static String readAndLogErrorBody(String tag, Response<?> response) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            Log.d(tag, "Error body: <empty>");
            return null;
        }
        try {
            String body = errorBody.string();
            Log.d(tag, "Error body: " + trimBody(body));
            return body;
        } catch (IOException exception) {
            Log.e(tag, "Could not read error body", exception);
            return null;
        }
    }

    public static void logFailure(String tag, Throwable throwable) {
        Log.e(tag, "Network failure", throwable);
    }

    private static String trimBody(String body) {
        if (body == null) {
            return null;
        }
        if (body.length() <= MAX_ERROR_BODY_BYTES) {
            return body;
        }
        return body.substring(0, (int) MAX_ERROR_BODY_BYTES) + "...";
    }
}
