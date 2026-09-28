package gt.com.ro.devumgapp.core.session;

import android.app.Activity;
import android.widget.Toast;

import java.util.Set;

import gt.com.ro.devumgapp.R;

/** Central permission codes and screen/action checks. Permissions are exact codes from the API. */
public final class Permissions {
    public static final String CARRERAS_LEER = "CARRERAS_LEER";
    public static final String CURSOS_LEER = "CURSOS_LEER";
    public static final String ESTUDIANTES_LEER = "ESTUDIANTES_LEER";
    public static final String DOCENTES_LEER = "DOCENTES_LEER";
    public static final String INSCRIPCIONES_LEER = "INSCRIPCIONES_LEER";
    public static final String NOTAS_LEER = "NOTAS_LEER";
    public static final String COLEGIATURAS_LEER = "COLEGIATURAS_LEER";
    public static final String USUARIOS_LEER = "USUARIOS_LEER";
    public static final String ROLES_LEER = "ROLES_LEER";
    public static final String PERMISOS_LEER = "PERMISOS_LEER";

    private Permissions() { }

    public static boolean has(String permission) {
        Set<String> permissions = SessionManager.getInstance().getPermissions();
        return permissions.contains(permission);
    }

    /** One source of truth for every module destination exposed by the app navigation. */
    public static String readingPermissionFor(int destinationId) {
        if (destinationId == R.id.nav_carreras) return CARRERAS_LEER;
        if (destinationId == R.id.nav_cursos) return CURSOS_LEER;
        if (destinationId == R.id.nav_estudiantes) return ESTUDIANTES_LEER;
        if (destinationId == R.id.nav_docentes) return DOCENTES_LEER;
        if (destinationId == R.id.nav_inscripciones) return INSCRIPCIONES_LEER;
        if (destinationId == R.id.nav_notas) return NOTAS_LEER;
        if (destinationId == R.id.nav_colegiaturas) return COLEGIATURAS_LEER;
        if (destinationId == R.id.nav_usuarios) return USUARIOS_LEER;
        if (destinationId == R.id.nav_roles) return ROLES_LEER;
        if (destinationId == R.id.nav_permisos) return PERMISOS_LEER;
        return null; // Inicio and Perfil are authenticated destinations, not modules.
    }

    public static boolean canOpenDestination(int destinationId) {
        String permission = readingPermissionFor(destinationId);
        return permission == null || has(permission);
    }

    public static boolean require(Activity activity, String permission) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            activity.finish();
            return false;
        }
        if (has(permission)) return true;
        Toast.makeText(activity, "No tienes permiso para realizar esta operación.", Toast.LENGTH_LONG).show();
        activity.finish();
        return false;
    }

    /** Internal forms/details require module visibility as well as their specific action. */
    public static boolean requireAll(Activity activity, String... permissions) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            SessionManager.handleUnauthorized(activity);
            activity.finish();
            return false;
        }
        for (String permission : permissions) {
            if (permission != null && !has(permission)) {
                Toast.makeText(activity, "No tienes permiso para realizar esta operación.", Toast.LENGTH_LONG).show();
                activity.finish();
                return false;
            }
        }
        return true;
    }
}
