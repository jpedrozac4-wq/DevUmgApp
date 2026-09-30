package gt.com.ro.devumgapp.auditoria.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
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
import gt.com.ro.devumgapp.core.ui.ModuleNavigation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuditoriaActivity extends androidx.appcompat.app.AppCompatActivity {
    private static final int PAGE_SIZE = 20;
    private LinearLayout filters, items;
    private View filtersHeader, filterControls;
    private ImageView filtersChevron;
    private View progress;
    private TextView pageInfo, summary;
    private MaterialButton previous, next, search;
    private final List<EditText> inputs = new ArrayList<>();
    private int page;
    private int totalPages = 1;
    private AuditoriaApiService api;
    private Call<JsonElement> listCall;
    private boolean filtersExpanded;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!isAdmin() || !Permissions.require(this, Permissions.AUDITORIA_LEER)) return;
        setContentView(R.layout.activity_auditoria);
        configureSystemBars();
        MaterialToolbar toolbar = findViewById(R.id.toolbarAuditoria);
        toolbar.setTitle("");
        ModuleNavigation.attach(this, toolbar);
        filters = findViewById(R.id.auditFilters);
        filtersHeader = findViewById(R.id.auditFiltersHeader);
        filterControls = findViewById(R.id.auditFilterControls);
        filtersChevron = findViewById(R.id.imgAuditFiltersChevron);
        items = findViewById(R.id.auditItems);
        progress = findViewById(R.id.progressAuditoria);
        pageInfo = findViewById(R.id.txtAuditPage);
        summary = findViewById(R.id.txtAuditSummary);
        previous = findViewById(R.id.btnAuditPrevious);
        next = findViewById(R.id.btnAuditNext);
        createFilters();
        gt.com.ro.devumgapp.core.ui.FilterPanelTouch.bind(filtersHeader, filterControls, () -> setFiltersExpanded(!filtersExpanded));
        previous.setOnClickListener(v -> { if (page > 0) load(page - 1); });
        next.setOnClickListener(v -> { if (page + 1 < totalPages) load(page + 1); });
        api = RetrofitClient.getClient().create(AuditoriaApiService.class);
        animateIntro();
        load(0);
    }

    @Override protected void onDestroy() {
        if (listCall != null) listCall.cancel();
        super.onDestroy();
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void createFilters() {
        String[] hints = {"Fecha desde (AAAA-MM-DD)", "Fecha hasta (AAAA-MM-DD)", "Usuario", "Módulo", "Acción", "Tipo de entidad", "ID de entidad"};
        for (int index = 0; index < hints.length; index++) {
            TextInputLayout container = new TextInputLayout(this);
            container.setHint(hints[index]);
            container.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(10);
            container.setLayoutParams(params);
            EditText input = new EditText(this);
            input.setSingleLine();
            input.setTextSize(14);
            input.setInputType(index == 6 ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
            container.addView(input, new LinearLayout.LayoutParams(-1, -2));
            inputs.add(input);
            filters.addView(container);
        }
        search = new MaterialButton(this);
        search.setText("Aplicar filtros");
        search.setIconResource(R.drawable.ic_search);
        search.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        search.setOnClickListener(v -> load(0));
        filters.addView(search, new LinearLayout.LayoutParams(-1, -2));
    }

    private boolean isAdmin() {
        Set<String> roles = SessionManager.getInstance().getRoles();
        for (String role : roles) if ("ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role)) return true;
        Toast.makeText(this, "Acceso denegado.", Toast.LENGTH_LONG).show();
        finish();
        return false;
    }

    private void load(int requestedPage) {
        if (listCall != null) listCall.cancel();
        setLoading(true);
        items.removeAllViews();
        listCall = api.listar(value(0), value(1), value(2), value(3), value(4), value(5), longValue(6), requestedPage, PAGE_SIZE);
        listCall.enqueue(new Callback<JsonElement>() {
            @Override public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (call.isCanceled()) return;
                listCall = null;
                setLoading(false);
                if (!response.isSuccessful()) {
                    showState(AcademicErrors.from(AuditoriaActivity.this, response), true);
                    return;
                }
                page = requestedPage;
                JsonElement body = response.body();
                JsonArray array = AcademicJson.array(body);
                totalPages = resolveTotalPages(body, array.size(), requestedPage);
                summary.setText(array.size() == 1 ? "1 actividad en esta página" : array.size() + " actividades en esta página");
                if (array.size() == 0) showState("No encontramos actividad con los filtros seleccionados.", false);
                else for (JsonElement element : array) if (element.isJsonObject()) render(element.getAsJsonObject());
                updatePager();
            }

            @Override public void onFailure(Call<JsonElement> call, Throwable error) {
                if (call.isCanceled()) return;
                listCall = null;
                setLoading(false);
                showState("No se pudo conectar con el servidor. Intenta nuevamente.", true);
            }
        });
    }

    private int resolveTotalPages(JsonElement body, int itemCount, int requestedPage) {
        if (body != null && body.isJsonObject()) {
            String pages = AcademicJson.text(body.getAsJsonObject(), "totalPages");
            try { return Math.max(1, Integer.parseInt(pages)); } catch (Exception ignored) { }
        }
        return itemCount < PAGE_SIZE ? requestedPage + 1 : requestedPage + 2;
    }

    private void render(JsonObject row) {
        String date = AcademicJson.text(row, "fecha", "fechaHora", "timestamp", "fechaCreacion");
        String actor = AcademicJson.text(row, "actor", "usuario", "username", "actorNombre");
        String rawAction = AcademicJson.text(row, "accion");
        String action = friendlyAction(rawAction, row);
        String module = AcademicJson.text(row, "modulo");
        String entity = AcademicJson.text(row, "tipoEntidad", "entidad");
        String entityId = AcademicJson.text(row, "entidadId", "idEntidad");
        String result = AcademicJson.text(row, "resultado");
        String description = AuditTextFormatter.visibleDescription(
                AcademicJson.text(row, "descripcion", "detalle"));

        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        card.setRadius(dp(16));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.dashboard_border));
        card.setCardElevation(0);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(15), dp(16), dp(15));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(label(action.isEmpty() ? "Actividad" : action, true, 15, R.color.dashboard_primary), new LinearLayout.LayoutParams(0, -2, 1));
        TextView dateView = label(formatDate(date), false, 12, R.color.dashboard_text_secondary);
        dateView.setGravity(Gravity.END);
        heading.addView(dateView, new LinearLayout.LayoutParams(-2, -2));
        content.addView(heading);

        TextView actorView = label(actor.isEmpty() ? "Usuario no identificado" : actor, true, 16, R.color.dashboard_text_primary);
        LinearLayout.LayoutParams actorParams = new LinearLayout.LayoutParams(-1, -2);
        actorParams.topMargin = dp(10);
        content.addView(actorView, actorParams);
        String context = joinDetails(module, entity, entityId);
        if (!context.isEmpty()) addText(content, context, 4, 13, R.color.dashboard_text_secondary);
        if (!description.isEmpty()) addText(content, description, 12, 14, R.color.dashboard_text_primary);
        if (!result.isEmpty()) {
            int color = resultColor(result);
            TextView resultView = label(friendlyResult(result), true, 12, color);
            resultView.setPadding(dp(10), dp(5), dp(10), dp(5));
            resultView.setBackground(pillBackground(color));
            LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(-2, -2);
            resultParams.topMargin = dp(12);
            content.addView(resultView, resultParams);
        }
        card.addView(content);
        items.addView(card);
    }

    private void addText(LinearLayout parent, String text, int marginTop, int size, int color) {
        TextView view = label(text, false, size, color);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(marginTop);
        parent.addView(view, params);
    }

    private TextView label(String value, boolean bold, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(this, color));
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable pillBackground(int colorResource) {
        int color = ContextCompat.getColor(this, colorResource);
        GradientDrawable background = new GradientDrawable();
        background.setColor((color & 0x00FFFFFF) | (28 << 24));
        background.setCornerRadius(dp(20));
        return background;
    }

    private int resultColor(String result) {
        String value = result.toUpperCase(Locale.ROOT);
        return value.contains("ERROR") || value.contains("FALL") || value.contains("DENEG")
                ? R.color.dashboard_error : R.color.dashboard_primary;
    }

    private String friendlyAction(String raw, JsonObject row) {
        if (raw == null) return "";
        String action = raw.trim().toUpperCase(Locale.ROOT);
        String method = AuditTextFormatter.httpMethod(raw);
        if (method.equals("POST") || action.contains("CREAR") || action.contains("CREATE")) return "Creó un registro";
        if (method.equals("PUT") || method.equals("PATCH") || action.contains("ACTUALIZ") || action.contains("UPDATE")) {
            String fields = AuditTextFormatter.editedFields(row);
            return fields.isEmpty() ? "Editó información" : "Editó " + fields;
        }
        if (method.equals("DELETE") || action.contains("ELIMIN")) return "Eliminó un registro";
        if (method.equals("GET") || action.contains("CONSULT") || action.contains("READ")) return "Consultó información";
        if (action.contains("LOGIN") || action.contains("INICIO_SESION")) return "Inició sesión";
        if (action.contains("LOGOUT") || action.contains("CIERRE_SESION")) return "Cerró sesión";
        return titleCase(raw.replace('_', ' '));
    }

    private String friendlyResult(String raw) {
        String result = raw.trim().toUpperCase(Locale.ROOT);
        if (result.equals("SUCCESS") || result.equals("OK") || result.equals("EXITOSO")) return "Completado";
        if (result.equals("ERROR") || result.equals("FAILED") || result.equals("FALLIDO")) return "No completado";
        return titleCase(raw.replace('_', ' '));
    }

    private String titleCase(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String text = value.trim().toLowerCase(Locale.ROOT);
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private String formatDate(String value) {
        if (value == null || value.trim().isEmpty()) return "Sin fecha";
        return value.replace('T', ' ').replace("Z", "");
    }

    private String joinDetails(String module, String entity, String entityId) {
        List<String> parts = new ArrayList<>();
        if (!module.isEmpty()) parts.add(titleCase(module.replace('_', ' ')));
        if (!entity.isEmpty()) parts.add(titleCase(entity.replace('_', ' ')) + (entityId.isEmpty() ? "" : " #" + entityId));
        return android.text.TextUtils.join(" · ", parts);
    }

    private void showState(String message, boolean error) {
        items.removeAllViews();
        TextView view = label(message, false, 14, error ? R.color.dashboard_error : R.color.dashboard_text_secondary);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(20), dp(28), dp(20), dp(28));
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        card.setRadius(dp(16));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.dashboard_border));
        card.addView(view);
        items.addView(card);
        summary.setText(error ? "No se pudo cargar la actividad" : "Sin resultados");
        updatePager();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (search != null) search.setEnabled(!loading);
        previous.setEnabled(!loading && page > 0);
        next.setEnabled(!loading && page + 1 < totalPages);
    }

    private void setFiltersExpanded(boolean expanded) {
        filtersExpanded = expanded;
        filtersHeader.setContentDescription(expanded ? "Ocultar filtros" : "Mostrar filtros");
        filtersChevron.animate().rotation(expanded ? 90f : 0f).setDuration(180).start();
        if (expanded) {
            filterControls.setVisibility(View.VISIBLE);
            filterControls.setAlpha(0f);
            filterControls.setTranslationY(-dp(8));
            filterControls.animate().alpha(1f).translationY(0f).setDuration(180).start();
            return;
        }
        filterControls.animate()
                .alpha(0f)
                .translationY(-dp(8))
                .setDuration(140)
                .withEndAction(() -> filterControls.setVisibility(View.GONE))
                .start();
    }

    private void animateIntro() {
        View hero = findViewById(R.id.auditHero);
        hero.setAlpha(0f);
        hero.setTranslationY(dp(16));
        hero.animate().alpha(1f).translationY(0).setDuration(320).start();
    }

    private String value(int index) {
        String value = inputs.get(index).getText().toString().trim();
        return value.isEmpty() ? null : value;
    }

    private Long longValue(int index) {
        try { return Long.valueOf(value(index)); } catch (Exception ignored) { return null; }
    }

    private void updatePager() {
        pageInfo.setText("Página " + (page + 1) + " de " + totalPages);
        previous.setEnabled(listCall == null && page > 0);
        next.setEnabled(listCall == null && page + 1 < totalPages);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
