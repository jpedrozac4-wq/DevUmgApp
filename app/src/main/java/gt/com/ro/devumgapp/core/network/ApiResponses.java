package gt.com.ro.devumgapp.core.network;

import android.content.Context;

import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Response;

public final class ApiResponses {
    private ApiResponses() { }
    public static String authorizationMessage(Context context, Response<?> response) {
        if (response.code() == 401) {
            SessionManager.handleUnauthorized(context);
            return "La sesión no es válida o venció. Inicia sesión de nuevo.";
        }
        return response.code() == 403 ? "No tienes permiso para realizar esta operación." : null;
    }
}
