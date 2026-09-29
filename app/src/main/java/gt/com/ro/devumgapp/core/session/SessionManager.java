package gt.com.ro.devumgapp.core.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;

import java.util.HashSet;
import java.util.Set;

import gt.com.ro.devumgapp.App;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import gt.com.ro.devumgapp.auth.ui.LoginActivity;

/**
 * Singleton wrapper over SharedPreferences that holds the current
 * authenticated session. The password is NEVER stored.
 */
public class SessionManager {

    private static final String PREF_NAME = "user_session";

    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USUARIO_ID = "usuario_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NOMBRE = "nombre";
    private static final String KEY_APELLIDO = "apellido";
    private static final String KEY_ROLES = "roles";
    private static final String KEY_PERMISSIONS = "permissions";
    private static final String KEY_REMEMBERED_USERNAME = "remembered_username";

    private static SessionManager instance;

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager(App.getContext());
        }
        return instance;
    }

    /** Persists every session field returned by the backend. Password is not part of the response and is never stored. */
    public void saveSession(LoginResponse response) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_AUTH_TOKEN, response.accessToken);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USUARIO_ID, response.usuarioId);
        editor.putString(KEY_USERNAME, response.username);
        if (response.email != null) editor.putString(KEY_EMAIL, response.email);
        editor.putString(KEY_NOMBRE, response.nombre);
        editor.putString(KEY_APELLIDO, response.apellido);
        editor.putStringSet(KEY_ROLES, new HashSet<>(
                response.roles != null ? response.roles : new HashSet<>()));
        editor.putStringSet(KEY_PERMISSIONS, new HashSet<>(
                response.permisos != null ? response.permisos : new HashSet<>()));
        editor.apply();
    }

    /** Refreshes the server-owned profile without replacing the already persisted JWT. */
    public void refreshProfile(LoginResponse response) {
        if (response == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        if (response.usuarioId > 0) editor.putLong(KEY_USUARIO_ID, response.usuarioId);
        if (response.username != null) editor.putString(KEY_USERNAME, response.username);
        if (response.email != null) editor.putString(KEY_EMAIL, response.email);
        if (response.nombre != null) editor.putString(KEY_NOMBRE, response.nombre);
        if (response.apellido != null) editor.putString(KEY_APELLIDO, response.apellido);
        if (response.roles != null) editor.putStringSet(KEY_ROLES, new HashSet<>(response.roles));
        if (response.permisos != null) editor.putStringSet(KEY_PERMISSIONS, new HashSet<>(response.permisos));
        editor.putBoolean(KEY_IS_LOGGED_IN, true).apply();
    }

    /** Closes the session. The remembered username (if any) survives for the next login. */
    public void logout() {
        prefs.edit()
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_IS_LOGGED_IN)
                .remove(KEY_USUARIO_ID)
                .remove(KEY_USERNAME)
                .remove(KEY_EMAIL)
                .remove(KEY_NOMBRE)
                .remove(KEY_APELLIDO)
                .remove(KEY_ROLES)
                .remove(KEY_PERMISSIONS)
                .apply();
    }

    /** Stores the username for pre-fill when the user chose "remember me" on login. Never the password. */
    public void rememberUsername(String username) {
        prefs.edit().putString(KEY_REMEMBERED_USERNAME, username).apply();
    }

    public String getRememberedUsername() {
        return prefs.getString(KEY_REMEMBERED_USERNAME, "");
    }

    public void clearRememberedUsername() {
        prefs.edit().remove(KEY_REMEMBERED_USERNAME).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getToken() {
        return prefs.getString(KEY_AUTH_TOKEN, "");
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public long getUsuarioId() { return prefs.getLong(KEY_USUARIO_ID, 0); }

    public String getNombre() { return prefs.getString(KEY_NOMBRE, ""); }

    public String getApellido() { return prefs.getString(KEY_APELLIDO, ""); }

    /** Returns "nombre apellido" of the logged user. */
    public String getNombreCompleto() {
        String nombre = prefs.getString(KEY_NOMBRE, "");
        String apellido = prefs.getString(KEY_APELLIDO, "");
        return nombre.trim() + " " + apellido.trim();
    }

    public Set<String> getRoles() {
        return new HashSet<>(prefs.getStringSet(KEY_ROLES, new HashSet<>()));
    }

    public static void handleUnauthorized(Context context) {
        getInstance().logout();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public Set<String> getPermissions() {
        return new HashSet<>(prefs.getStringSet(KEY_PERMISSIONS, new HashSet<>()));
    }
}
