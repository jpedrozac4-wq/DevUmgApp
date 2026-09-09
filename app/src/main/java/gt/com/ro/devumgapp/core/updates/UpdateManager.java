package gt.com.ro.devumgapp.core.updates;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gt.com.ro.devumgapp.R;

/**
 * Checks for app updates reading the Firestore document {@code versiones/actual}
 * (fields: versionCode, versionName, apkUrl) and, when a newer version exists,
 * downloads and installs the APK through a FileProvider.
 */
public class UpdateManager {

    private static final String TAG = "UpdateManager";
    private static final String COLLECTION = "versiones";
    private static final String DOCUMENT = "actual";

    private final Context context;
    private final FirebaseFirestore db;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface UpdateCheckCallback {
        void onNoUpdate();

        void onError(Exception e);
    }

    public UpdateManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Queries Firestore and, when a newer version is available, shows the update dialog.
     * The callback is only invoked when there is nothing to install or on error.
     */
    public void checkForUpdates(UpdateCheckCallback callback) {
        db.collection(COLLECTION).document(DOCUMENT)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        processUpdateInfo(documentSnapshot, callback);
                    } else {
                        Log.e(TAG, "Firestore document '" + COLLECTION + "/" + DOCUMENT + "' does not exist");
                        callback.onNoUpdate();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error reading update info from Firestore", e);
                    callback.onError(e);
                });
    }

    private void processUpdateInfo(DocumentSnapshot document, UpdateCheckCallback callback) {
        Long firebaseVersionCode = document.getLong("versionCode");
        String versionName = document.getString("versionName");
        String apkUrl = document.getString("apkUrl");

        if (firebaseVersionCode == null || apkUrl == null || apkUrl.isEmpty()) {
            Log.e(TAG, "Update document has incomplete data");
            callback.onNoUpdate();
            return;
        }

        int currentVersionCode = getCurrentVersionCode();
        if (firebaseVersionCode > currentVersionCode) {
            showUpdateDialog(versionName, apkUrl);
        } else {
            callback.onNoUpdate();
        }
    }

    private int getCurrentVersionCode() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) pInfo.getLongVersionCode();
            }
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error reading current versionCode", e);
            return -1;
        }
    }

    private void showUpdateDialog(String newVersionName, String apkUrl) {
        String currentVersionName = "";
        try {
            currentVersionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            // Keep the empty fallback.
        }

        String message = context.getString(R.string.actualizacion_mensaje, currentVersionName, newVersionName);

        new AlertDialog.Builder(context)
                .setTitle(R.string.actualizacion_titulo)
                .setMessage(message)
                .setPositiveButton(R.string.actualizacion_boton, (dialog, which) -> downloadAndInstallApk(apkUrl))
                .setCancelable(false)
                .show();
    }

    private void downloadAndInstallApk(String apkUrl) {
        AlertDialog progressDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.actualizacion_descarga_titulo)
                .setMessage(R.string.actualizacion_descarga_iniciando)
                .setCancelable(false)
                .create();

        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setPadding(40, 20, 40, 20);

        TextView progressText = new TextView(context);
        progressText.setText(context.getString(R.string.actualizacion_progreso, 0));
        progressText.setPadding(40, 0, 40, 20);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(progressBar);
        layout.addView(progressText);

        progressDialog.setView(layout);
        progressDialog.show();

        executorService.execute(() -> {
            File result = doDownload(apkUrl, progress ->
                    mainHandler.post(() -> {
                        progressBar.setProgress(progress);
                        progressText.setText(context.getString(R.string.actualizacion_progreso, progress));
                    }));

            mainHandler.post(() -> {
                progressDialog.dismiss();
                if (result != null && result.exists()) {
                    installApk(context, result);
                } else {
                    Toast.makeText(context, R.string.actualizacion_descarga_error, Toast.LENGTH_LONG).show();
                }
                executorService.shutdown();
            });
        });
    }

    private interface ProgressListener {
        void onProgress(int progress);
    }

    private File doDownload(String urlPath, ProgressListener listener) {
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        try {
            URL url = new URL(urlPath);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP " + connection.getResponseCode());
                return null;
            }

            int fileLength = connection.getContentLength();
            input = new BufferedInputStream(url.openStream());

            File cacheDir = context.getExternalCacheDir();
            if (cacheDir == null) {
                Log.e(TAG, "External cache dir is unavailable");
                return null;
            }

            File apkFile = new File(cacheDir, "update.apk");
            output = new FileOutputStream(apkFile);

            byte[] data = new byte[1024];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                if (fileLength > 0) {
                    listener.onProgress((int) (total * 100 / fileLength));
                }
                output.write(data, 0, count);
            }
            return apkFile;
        } catch (Exception e) {
            Log.e(TAG, "Error downloading APK", e);
            return null;
        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
            } catch (Exception ignored) {
                // Best-effort cleanup.
            }
            if (connection != null) connection.disconnect();
        }
    }

    private static void installApk(Context context, File file) {
        try {
            Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting installer", e);
            Toast.makeText(context, R.string.actualizacion_instalar_error, Toast.LENGTH_LONG).show();
        }
    }
}
