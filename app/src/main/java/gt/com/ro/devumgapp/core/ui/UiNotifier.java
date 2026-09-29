package gt.com.ro.devumgapp.core.ui;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import gt.com.ro.devumgapp.R;

public final class UiNotifier {

    private UiNotifier() {
    }

    public static void success(Activity activity, int messageRes) {
        success(activity, activity.getString(messageRes));
    }

    public static void success(Activity activity, String message) {
        // Use the application context so the confirmation remains visible when a
        // form closes immediately after a successful create/update request.
        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public static void error(Activity activity, String message) {
        show(activity, message, R.color.dashboard_error, Snackbar.LENGTH_LONG);
    }

    public static void info(Activity activity, int messageRes) {
        info(activity, activity.getString(messageRes));
    }

    public static void info(Activity activity, String message) {
        show(activity, message, R.color.dashboard_primary, Snackbar.LENGTH_SHORT);
    }

    private static void show(Activity activity, String message, int backgroundColorRes, int duration) {
        View root = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(root, message, duration);
        snackbar.setTextColor(ContextCompat.getColor(activity, R.color.white));
        snackbar.setBackgroundTint(ContextCompat.getColor(activity, backgroundColorRes));
        snackbar.show();
    }
}
