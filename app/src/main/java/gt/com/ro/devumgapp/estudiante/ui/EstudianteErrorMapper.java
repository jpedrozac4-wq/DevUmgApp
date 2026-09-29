package gt.com.ro.devumgapp.estudiante.ui;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.content.Context;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.network.ApiResponses;
import gt.com.ro.devumgapp.core.network.IdentityConflictMessage;
import retrofit2.Response;

final class EstudianteErrorMapper {
    private EstudianteErrorMapper() {}

    static String fromResponse(Context context, Response<?> response) {
        String authMessage = ApiResponses.authorizationMessage(context, response);
        if (authMessage != null) return authMessage;
        if (response.code() == 409) return IdentityConflictMessage.fromResponse(response);
        try {
            if (response.errorBody() != null) {
                String text = response.errorBody().string();
                if (text != null && !text.trim().isEmpty()) return text.trim();
            }
        } catch (IOException ignored) { }

        if (response.code() == 400) return context.getString(R.string.estudiante_error_bad_request);
        if (response.code() == 401) return context.getString(R.string.estudiante_error_unauthorized);
        if (response.code() == 403) return context.getString(R.string.estudiante_error_forbidden);
        if (response.code() == 404) return context.getString(R.string.estudiante_error_not_found);
        if (response.code() == 500) return context.getString(R.string.estudiante_error_internal_server);
        return context.getString(R.string.estudiante_error_server);
    }

    static String fromFailure(Context context, Throwable throwable) {
        if (throwable instanceof UnknownHostException) return context.getString(R.string.estudiante_error_no_internet);
        if (throwable instanceof SocketTimeoutException) return context.getString(R.string.estudiante_error_timeout);
        return context.getString(R.string.estudiante_error_network);
    }
}
