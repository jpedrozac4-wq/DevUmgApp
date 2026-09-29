package gt.com.ro.devumgapp.auditoria.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.ui.AcademicErrors;
import gt.com.ro.devumgapp.academico.ui.AcademicJson;
import gt.com.ro.devumgapp.auditoria.network.AuditoriaApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuditoriaActivity extends androidx.appcompat.app.AppCompatActivity {
    private LinearLayout filters, items;
    private View progress;
    private TextView pageInfo;
    private MaterialButton previous, next;
    private final List<EditText> inputs = new ArrayList<>();
    private int page;
    private int totalPages = 1;
    private AuditoriaApiService api;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!isAdmin() || !Permissions.require(this, Permissions.AUDITORIA_LEER)) return;
        setContentView(R.layout.activity_auditoria);
        ((MaterialToolbar) findViewById(R.id.toolbarAuditoria)).setNavigationOnClickListener(v -> finish());
        filters = findViewById(R.id.auditFilters); items = findViewById(R.id.auditItems); progress = findViewById(R.id.progressAuditoria);
        pageInfo = findViewById(R.id.txtAuditPage); previous = findViewById(R.id.btnAuditPrevious); next = findViewById(R.id.btnAuditNext);
        for (String hint : new String[]{"Fecha desde (AAAA-MM-DD)", "Fecha hasta (AAAA-MM-DD)", "Usuario", "Módulo", "Acción", "Tipo de entidad", "ID de entidad"}) {
            EditText input = new EditText(this); input.setHint(hint); input.setSingleLine(); inputs.add(input); filters.addView(input);
        }
        inputs.get(6).setInputType(InputType.TYPE_CLASS_NUMBER);
        MaterialButton search = new MaterialButton(this); search.setText("Aplicar filtros"); search.setIconResource(R.drawable.ic_search);
        search.setOnClickListener(v -> { page = 0; load(); }); filters.addView(search);
        previous.setOnClickListener(v -> { if (page > 0) { page--; load(); } });
        next.setOnClickListener(v -> { if (page + 1 < totalPages) { page++; load(); } });
        api = RetrofitClient.getClient().create(AuditoriaApiService.class); load();
    }

    private boolean isAdmin() {
        Set<String> roles = SessionManager.getInstance().getRoles();
        for (String role : roles) if ("ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role)) return true;
        Toast.makeText(this, "Acceso denegado.", Toast.LENGTH_LONG).show(); finish(); return false;
    }

    private void load() {
        progress.setVisibility(View.VISIBLE); items.removeAllViews();
        api.listar(value(0), value(1), value(2), value(3), value(4), value(5), longValue(6), page, 20).enqueue(new Callback<JsonElement>() {
            @Override public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                progress.setVisibility(View.GONE);
                if (!response.isSuccessful()) { show(AcademicErrors.from(AuditoriaActivity.this, response)); return; }
                JsonElement body = response.body(); JsonArray array = AcademicJson.array(body);
                if (body != null && body.isJsonObject()) {
                    String pages = AcademicJson.text(body.getAsJsonObject(), "totalPages");
                    try { totalPages = Math.max(1, Integer.parseInt(pages)); } catch (Exception ignored) { totalPages = array.size() < 20 ? page + 1 : page + 2; }
                }
                if (array.size() == 0) show("No hay registros de auditoría para estos filtros.");
                for (JsonElement element : array) if (element.isJsonObject()) render(element.getAsJsonObject());
                updatePager();
            }
            @Override public void onFailure(Call<JsonElement> call, Throwable error) { progress.setVisibility(View.GONE); show("No se pudo conectar con el servidor."); }
        });
    }

    private void render(JsonObject row) {
        String date = AcademicJson.text(row, "fecha", "fechaHora", "timestamp", "fechaCreacion");
        String actor = AcademicJson.text(row, "actor", "usuario", "username", "actorNombre");
        String action = AcademicJson.text(row, "accion"); String module = AcademicJson.text(row, "modulo");
        String entity = AcademicJson.text(row, "tipoEntidad", "entidad"); String entityId = AcademicJson.text(row, "entidadId", "idEntidad");
        String result = AcademicJson.text(row, "resultado"); String description = AcademicJson.text(row, "descripcion", "detalle");
        TextView text = new TextView(this); text.setText(String.format(Locale.getDefault(), "%s\n%s · %s\n%s · %s %s\n%s\n%s", date, actor, action, module, entity, entityId, result, description));
        text.setTextColor(getColor(R.color.dashboard_text_primary)); text.setTextSize(14); text.setPadding(dp(16), dp(12), dp(16), dp(12));
        MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(14)); card.setStrokeWidth(dp(1)); card.setStrokeColor(getColor(R.color.dashboard_border)); card.addView(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(10); card.setLayoutParams(params); items.addView(card);
    }

    /* Deliberadamente no se renderizan valorAnterior/valorNuevo: pueden contener secretos o datos personales. */
    private String value(int index) { String value = inputs.get(index).getText().toString().trim(); return value.isEmpty() ? null : value; }
    private Long longValue(int index) { try { return Long.valueOf(value(index)); } catch (Exception ignored) { return null; } }
    private void show(String message) { TextView view = new TextView(this); view.setText(message); view.setPadding(0, dp(16), 0, dp(16)); items.addView(view); }
    private void updatePager() { pageInfo.setText("Página " + (page + 1) + " de " + totalPages); previous.setEnabled(page > 0); next.setEnabled(page + 1 < totalPages); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
