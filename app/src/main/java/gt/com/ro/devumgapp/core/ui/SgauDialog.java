package gt.com.ro.devumgapp.core.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.Window;
import android.view.WindowManager;
import java.util.WeakHashMap;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import gt.com.ro.devumgapp.R;

/** Single SGAU confirmation component for every data-changing operation. */
public final class SgauDialog {
    private static final WeakHashMap<Activity, AlertDialog> visibleDialogs = new WeakHashMap<>();
    private SgauDialog() {}

    public static AlertDialog confirm(
            Activity activity,
            @DrawableRes int icon,
            CharSequence title,
            CharSequence description,
            CharSequence confirmLabel,
            Runnable onConfirm) {
        if (activity.isFinishing() || activity.isDestroyed()) return null;
        AlertDialog visible = visibleDialogs.get(activity);
        if (visible != null && visible.isShowing()) return visible;
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setIcon(icon)
                .setTitle(title)
                .setMessage(description)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(confirmLabel, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setWindowAnimations(R.style.Animation_SGAU_Dialog);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
                if (!view.isEnabled()) return;
                view.setEnabled(false); // Also guards against a double tap during dismissal.
                dialog.dismiss();
                onConfirm.run();
            });
        });
        dialog.setOnDismissListener(ignored -> visibleDialogs.remove(activity));
        dialog.show();
        visibleDialogs.put(activity, dialog);
        return dialog;
    }

    public static AlertDialog confirmSave(Activity activity, boolean editing, CharSequence recordName, Runnable action) {
        String operation = activity.getString(editing ? R.string.dialog_operation_update : R.string.dialog_operation_create);
        return confirm(activity, R.drawable.ic_save,
                activity.getString(editing ? R.string.dialog_title_update : R.string.dialog_title_create),
                activity.getString(R.string.dialog_message_save, operation, recordName),
                activity.getString(editing ? R.string.dialog_update : R.string.dialog_create), action);
    }

    public static AlertDialog confirmState(Activity activity, boolean activating, CharSequence recordName, Runnable action) {
        return confirm(activity, R.drawable.ic_power,
                activity.getString(activating ? R.string.dialog_title_activate : R.string.dialog_title_deactivate),
                activity.getString(activating ? R.string.dialog_message_activate : R.string.dialog_message_deactivate, recordName),
                activity.getString(activating ? R.string.dialog_activate : R.string.dialog_deactivate), action);
    }
}
