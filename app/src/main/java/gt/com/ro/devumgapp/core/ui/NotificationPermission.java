package gt.com.ro.devumgapp.notificacion;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class NotificationPermission {
    private static final int REQUEST_CODE = 881;
    private static final String PREFERENCES = "sgau_notification_permission";
    private static final String ASKED = "asked";

    private NotificationPermission() {}

    public static void requestIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;

        var preferences = activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (preferences.getBoolean(ASKED, false)) return;

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Recibir notificaciones de SGAU")
                .setMessage("Activa las notificaciones para recibir avisos de notas, cursos, colegiaturas y pagos aunque la aplicación esté cerrada.")
                .setNegativeButton("Ahora no", (dialog, which) ->
                        preferences.edit().putBoolean(ASKED, true).apply())
                .setPositiveButton("Permitir", (dialog, which) -> {
                    preferences.edit().putBoolean(ASKED, true).apply();
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE);
                })
                .show();
    }
}
