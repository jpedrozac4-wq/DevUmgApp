package gt.com.ro.devumgapp.notificacion.ui;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Map;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.ui.DocenteAcademicActivity;
import gt.com.ro.devumgapp.academico.ui.EstudianteAcademicActivity;
import gt.com.ro.devumgapp.auth.ui.HomeActivity;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.session.SessionManager;
import gt.com.ro.devumgapp.notificacion.NotificationApi;
import gt.com.ro.devumgapp.notificacion.NotificationModels;
import gt.com.ro.devumgapp.notificacion.NotificationPermission;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationInboxActivity extends AppCompatActivity {
    public static final String EXTRA_NOTIFICATION_ID = "notificationId";
    private static final int REQ_PUSH = 881;
    private static volatile boolean visible;

    private LinearLayout rows;
    private ProgressBar progress;
    private NotificationApi api;
    private int page;
    private boolean loading;
    private MaterialButton loadMore;

    public static boolean isVisible() { return visible; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SessionManager.getInstance().isLoggedIn()) {
            SessionManager.handleUnauthorized(this);
            return;
        }
        api = RetrofitClient.getClient().create(NotificationApi.class);
        buildScreen();
        NotificationPermission.requestIfNeeded(this);
        load(true);
        String id = getIntent().getStringExtra(EXTRA_NOTIFICATION_ID);
        if (id != null) openPush(id);
    }

    private void buildScreen() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_background));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Notificaciones");
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.dashboard_text_primary));
        toolbar.setTitleTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_TitleLarge);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationContentDescription("Volver al dashboard");
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        toolbar.setElevation(dp(1));
        toolbar.setNavigationOnClickListener(v -> goToDashboard());
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(64)));

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(content);

        TextView intro = text("Mantente al día con la actividad de tu cuenta.", 14, false);
        intro.setTextColor(ContextCompat.getColor(this, R.color.dashboard_text_secondary));
        content.addView(intro, bottom(dp(16)));

        MaterialCardView actionsCard = new MaterialCardView(this);
        actionsCard.setRadius(dp(16));
        actionsCard.setCardElevation(0);
        actionsCard.setStrokeWidth(dp(1));
        actionsCard.setStrokeColor(ContextCompat.getColor(this, R.color.dashboard_border));
        actionsCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(dp(12), dp(8), dp(12), dp(8));
        MaterialButton readAll = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        readAll.setText("Marcar todas leídas");
        readAll.setTextSize(12);
        readAll.setInsetTop(0); readAll.setInsetBottom(0);
        readAll.setOnClickListener(v -> readAll());
        MaterialButton clear = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        clear.setText("Vaciar buzón");
        clear.setTextSize(12);
        clear.setInsetTop(0); clear.setInsetBottom(0);
        clear.setOnClickListener(v -> confirmClear());
        actions.addView(readAll, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        clearParams.leftMargin = dp(8);
        actions.addView(clear, clearParams);
        actionsCard.addView(actions);
        content.addView(actionsCard, bottom(dp(16)));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(progress, progressParams);

        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        content.addView(rows);
        loadMore = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        loadMore.setText("Cargar más");
        loadMore.setOnClickListener(v -> load(false));
        content.addView(loadMore, bottom(dp(4)));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private LinearLayout.LayoutParams bottom(int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = margin;
        return p;
    }

    private void goToDashboard() {
        Intent home = new Intent(this, HomeActivity.class)
                .putExtra(HomeActivity.EXTRA_CURRENT_DESTINATION, R.id.nav_inicio)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
    }

    @Override protected void onResume() { super.onResume(); visible = true; }
    @Override protected void onPause() { visible = false; super.onPause(); }

    private void askPushPermission() {
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        if (getPreferences(0).getBoolean("push_asked", false)) {
            // Already asked once, but the grant can still be missing: the OS resets
            // runtime permissions when the app is reinstalled, while `push_asked`
            // lives in shared preferences and survives. Re-issuing the request is
            // safe -- if the user denied permanently, the system answers immediately
            // without showing anything.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_PUSH);
            return;
        }
        new MaterialAlertDialogBuilder(this).setTitle("Recibir avisos de SGAU")
                .setMessage("Puedes activar avisos discretos cuando haya novedades. El buzón seguirá disponible si los omites.")
                .setNegativeButton("Ahora no", (d, w) -> getPreferences(0).edit().putBoolean("push_asked", true).apply())
                .setPositiveButton("Activar", (d, w) -> {
                    getPreferences(0).edit().putBoolean("push_asked", true).apply();
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_PUSH);
                }).show();
    }

    private void load(boolean reset) {
        if (loading) return;
        loading = true;
        progress.setVisibility(View.VISIBLE);
        if (reset) { page = 0; rows.removeAllViews(); }
        api.list(page, 20).enqueue(new Callback<PageResponse<NotificationModels.Item>>() {
            @Override public void onResponse(Call<PageResponse<NotificationModels.Item>> call, Response<PageResponse<NotificationModels.Item>> response) {
                loading = false; progress.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) { empty("No se pudo cargar el buzón.", true); return; }
                List<NotificationModels.Item> items = response.body().content;
                if (items == null || items.isEmpty()) {
                    if (page == 0) empty("Tu buzón está vacío.", false);
                    loadMore.setVisibility(View.GONE);
                    return;
                }
                for (NotificationModels.Item item : items) addItem(item);
                page++;
                loadMore.setVisibility(items.size() < 20 ? View.GONE : View.VISIBLE);
            }
            @Override public void onFailure(Call<PageResponse<NotificationModels.Item>> call, Throwable error) {
                loading = false; progress.setVisibility(View.GONE); empty("Sin conexión. Puedes reintentar.", true);
            }
        });
    }

    private void addItem(NotificationModels.Item n) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16)); card.setCardElevation(0); card.setStrokeWidth(dp(1));
        card.setStrokeColor(ContextCompat.getColor(this, n.leida ? R.color.dashboard_border : R.color.dashboard_primary));
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(15), dp(16), dp(12));
        LinearLayout heading = new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(n.titulo, 16, true); title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1)); heading.addView(title);
        TextView status = text(n.leida ? "LEÍDA" : "NUEVA", 10, true);
        status.setTextColor(ContextCompat.getColor(this, n.leida ? R.color.dashboard_text_secondary : R.color.dashboard_primary));
        status.setBackgroundResource(R.drawable.bg_notification_badge);
        status.setPadding(dp(9), dp(5), dp(9), dp(5)); heading.addView(status);
        box.addView(heading);
        TextView message = text(n.mensaje, 14, false); message.setTextColor(ContextCompat.getColor(this, R.color.dashboard_text_secondary));
        LinearLayout.LayoutParams messageParams = bottom(dp(8)); messageParams.topMargin = dp(8); box.addView(message, messageParams);
        TextView date = text(n.fechaCreacion, 12, false); date.setTextColor(ContextCompat.getColor(this, R.color.dashboard_text_secondary)); box.addView(date);
        LinearLayout buttons = new LinearLayout(this); buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); buttons.setPadding(0, dp(10), 0, 0);
        MaterialButton read = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        read.setText(n.leida ? "Leída" : "Marcar leída"); read.setEnabled(!n.leida); read.setTextSize(12); read.setInsetTop(0); read.setInsetBottom(0);
        read.setOnClickListener(v -> api.read(n.id).enqueue(simpleReload("No se pudo marcar como leída.")));
        MaterialButton delete = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        delete.setText("Eliminar"); delete.setTextSize(12); delete.setInsetTop(0); delete.setInsetBottom(0);
        delete.setOnClickListener(v -> api.delete(n.id).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) { load(true); }
            @Override public void onFailure(Call<Void> c, Throwable t) { Toast.makeText(NotificationInboxActivity.this, "No se pudo eliminar.", Toast.LENGTH_SHORT).show(); }
        }));
        buttons.addView(read); LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-2, -2); deleteParams.leftMargin = dp(8); buttons.addView(delete, deleteParams);
        box.addView(buttons); card.addView(box); card.setOnClickListener(v -> open(n));
        LinearLayout.LayoutParams cardParams = bottom(dp(12)); rows.addView(card, cardParams);
    }

    private Callback<NotificationModels.Item> simpleReload(String error) {
        return new Callback<NotificationModels.Item>() {
            @Override public void onResponse(Call<NotificationModels.Item> c, Response<NotificationModels.Item> r) { load(true); }
            @Override public void onFailure(Call<NotificationModels.Item> c, Throwable t) { Toast.makeText(NotificationInboxActivity.this, error, Toast.LENGTH_SHORT).show(); }
        };
    }

    private void openPush(String id) {
        try {
            api.get(Long.parseLong(id)).enqueue(new Callback<NotificationModels.Item>() {
                @Override public void onResponse(Call<NotificationModels.Item> c, Response<NotificationModels.Item> r) {
                    if (!r.isSuccessful() || r.body() == null) { empty("Esta notificación no está disponible para esta cuenta.", true); return; }
                    open(r.body());
                }
                @Override public void onFailure(Call<NotificationModels.Item> c, Throwable t) { empty("No se pudo verificar el aviso.", true); }
            });
        } catch (NumberFormatException e) { empty("El aviso no es válido.", true); }
    }

    private void open(NotificationModels.Item n) {
        api.read(n.id).enqueue(new Callback<NotificationModels.Item>() {
            @Override public void onResponse(Call<NotificationModels.Item> c, Response<NotificationModels.Item> r) {
                if (!r.isSuccessful()) { Toast.makeText(NotificationInboxActivity.this, "La notificación ya no está disponible.", Toast.LENGTH_LONG).show(); return; }
                if ("REVISION_PAGO".equals(n.destinoTipo)) {
                    if (!Permissions.hasRole("ADMIN") || !Permissions.has(Permissions.COLEGIATURAS_CAMBIAR_ESTADO)) { Toast.makeText(NotificationInboxActivity.this, "No tienes permiso para revisar pagos.", Toast.LENGTH_LONG).show(); return; }
                    Intent review = new Intent(NotificationInboxActivity.this, gt.com.ro.devumgapp.colegiatura.ui.ColegiaturaListActivity.class);
                    if (n.destinoId != null) review.putExtra("notificationPaymentId", n.destinoId);
                    startActivity(review); finish(); return;
                }
                int dest = R.id.nav_cursos;
                if ("NOTA".equals(n.destinoTipo)) dest = R.id.nav_notas;
                else if ("COLEGIATURA".equals(n.destinoTipo)) dest = R.id.nav_colegiaturas;
                Intent intent;
                if (Permissions.hasRole("ESTUDIANTE")) intent = new Intent(NotificationInboxActivity.this, EstudianteAcademicActivity.class).putExtra(HomeActivity.EXTRA_CURRENT_DESTINATION, dest);
                else if (Permissions.hasRole("DOCENTE")) intent = new Intent(NotificationInboxActivity.this, DocenteAcademicActivity.class).putExtra(HomeActivity.EXTRA_CURRENT_DESTINATION, dest);
                else intent = new Intent(NotificationInboxActivity.this, HomeActivity.class).putExtra(HomeActivity.EXTRA_CURRENT_DESTINATION, dest);
                if (n.destinoId != null) intent.putExtra("notificationDestinationId", n.destinoId);
                startActivity(intent); finish();
            }
            @Override public void onFailure(Call<NotificationModels.Item> c, Throwable t) { Toast.makeText(NotificationInboxActivity.this, "No se pudo abrir el destino.", Toast.LENGTH_LONG).show(); }
        });
    }

    private void readAll() {
        api.readAll().enqueue(new Callback<Map<String, Integer>>() {
            @Override public void onResponse(Call<Map<String, Integer>> c, Response<Map<String, Integer>> r) { load(true); }
            @Override public void onFailure(Call<Map<String, Integer>> c, Throwable t) { Toast.makeText(NotificationInboxActivity.this, "No se pudo actualizar el buzón.", Toast.LENGTH_SHORT).show(); }
        });
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(this).setTitle("Vaciar buzón")
                .setMessage("Se eliminarán únicamente tus notificaciones de SGAU. La auditoría académica no se modifica.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Vaciar", (d, w) -> api.clear().enqueue(new Callback<Map<String, Integer>>() {
                    @Override public void onResponse(Call<Map<String, Integer>> c, Response<Map<String, Integer>> r) { load(true); }
                    @Override public void onFailure(Call<Map<String, Integer>> c, Throwable t) { Toast.makeText(NotificationInboxActivity.this, "No se pudo vaciar el buzón.", Toast.LENGTH_SHORT).show(); }
                })).show();
    }

    private void empty(String message, boolean retry) {
        if (rows.getChildCount() == 0) {
            MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(16)); card.setCardElevation(0);
            card.setStrokeWidth(dp(1)); card.setStrokeColor(ContextCompat.getColor(this, R.color.dashboard_border));
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_surface));
            TextView label = text(message, 14, false); label.setGravity(Gravity.CENTER); label.setPadding(dp(20), dp(28), dp(20), dp(28)); card.addView(label);
            rows.addView(card, bottom(dp(12)));
            if (retry) { MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle); button.setText("Reintentar"); button.setOnClickListener(v -> load(true)); rows.addView(button, bottom(dp(12))); }
        }
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this); view.setText(value == null ? "" : value); view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(this, R.color.dashboard_text_primary));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
