package gt.com.ro.devumgapp.academico.ui;

import android.content.Context;

import com.google.gson.Gson;

import java.io.IOException;

import gt.com.ro.devumgapp.auth.dto.ApiError;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Response;

public final class AcademicErrors {
    private AcademicErrors() { }

    public static String from(Context context, Response<?> response) {
        if (response.code() == 401) {
            SessionManager.handleUnauthorized(context);
            return "La sesión ya no es válida.";
        }
        if (response.code() == 403) return "Acceso denegado.";
        ApiError error = null;
        try {
            if (response.errorBody() != null) error = new Gson().fromJson(response.errorBody().string(), ApiError.class);
        } catch (IOException ignored) { }
        if (response.code() == 404 && error != null
                && "VINCULACION_ACADEMICA_NO_ENCONTRADA".equals(error.code)) {
            return "Falta vincular esta cuenta con su registro académico. Solicita apoyo a administración.";
        }
        if (error != null && error.message != null && !error.message.trim().isEmpty()) return error.message;
        return "No fue posible completar la consulta (HTTP " + response.code() + ").";
    }
}
