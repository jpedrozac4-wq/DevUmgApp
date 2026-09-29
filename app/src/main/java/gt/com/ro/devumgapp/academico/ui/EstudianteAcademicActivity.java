package gt.com.ro.devumgapp.academico.ui;

import android.os.Bundle;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstudianteAcademicActivity extends AcademicBaseActivity {
    private AcademicoApiService api;
    private int pending;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SessionManager.getInstance().isLoggedIn()) { SessionManager.handleUnauthorized(this); return; }
        setContentView(R.layout.activity_academic_overview);
        content = findViewById(R.id.academicContent); progress = findViewById(R.id.progressAcademic);
        MaterialToolbar toolbar = findViewById(R.id.toolbarAcademic);
        toolbar.setTitle("Mi información académica"); toolbar.setNavigationOnClickListener(v -> finish());
        api = RetrofitClient.getClient().create(AcademicoApiService.class);
        load();
    }

    private void load() {
        content.removeAllViews(); pending = 7; loading(true);
        request("Mi carrera", api.estudianteMe(), false);
        request("Cursos inscritos", api.cursosEstudiante(), true);
        request("Inscripciones", api.inscripcionesEstudiante(), true);
        request("Notas", api.notasEstudiante(), true);
        request("Promedio", api.promedioEstudiante(), false);
        request("Colegiaturas", api.colegiaturasEstudiante(), true);
        request("Estado de cuenta", api.estadoCuentaEstudiante(), false);
    }

    private void request(String title, Call<JsonElement> call, boolean collection) {
        call.enqueue(new Callback<JsonElement>() {
            @Override public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful()) render(title, response.body(), collection);
                else renderMessage(title, AcademicErrors.from(EstudianteAcademicActivity.this, response));
                done();
            }
            @Override public void onFailure(Call<JsonElement> call, Throwable error) { renderMessage(title, "No se pudo cargar esta sección."); done(); }
        });
    }

    private synchronized void render(String title, JsonElement data, boolean collection) {
        content.addView(section(title));
        if (collection) {
            JsonArray array = AcademicJson.array(data);
            if (array.size() == 0) { content.addView(text("Sin datos", 15, false)); return; }
            for (JsonElement element : array) if (element.isJsonObject()) {
                JsonObject item = element.getAsJsonObject();
                content.addView(card(primary(title, item), secondary(title, item)));
            }
        } else {
            JsonObject item = AcademicJson.object(data);
            content.addView(card(primary(title, item), secondary(title, item)));
        }
    }

    private String primary(String section, JsonObject item) {
        if (section.equals("Mi carrera")) return value(item, "carreraNombre", "carrera", "nombreCarrera", "nombreCompleto");
        if (section.equals("Promedio")) return "Promedio: " + value(item, "promedioGeneral", "promedio");
        if (section.equals("Estado de cuenta")) return "Saldo pendiente: " + value(item, "saldoPendiente", "saldo", "totalPendiente");
        if (section.equals("Notas")) return value(item, "cursoNombre", "nombreCurso") + " · " + value(item, "tipoEvaluacion", "tipo") + ": " + value(item, "calificacion", "nota");
        if (section.equals("Colegiaturas")) return value(item, "concepto") + " · " + value(item, "estado");
        return value(item, "cursoNombre", "nombre", "codigo", "estado");
    }

    private String secondary(String section, JsonObject item) {
        List<String> values = new ArrayList<>();
        add(values, value(item, "cursoCodigo", "codigoCurso", "codigo"));
        add(values, value(item, "carreraNombre"));
        if (section.equals("Notas")) add(values, value(item, "observaciones"));
        if (section.equals("Colegiaturas") || section.equals("Estado de cuenta")) {
            add(values, label("Total", value(item, "montoTotal", "total")));
            add(values, label("Pagado", value(item, "montoPagado", "totalPagado")));
        }
        return String.join(" · ", values);
    }

    private String value(JsonObject item, String... keys) { String value = AcademicJson.text(item, keys); return value.isEmpty() ? "No disponible" : value; }
    private String label(String label, String value) { return value.isEmpty() ? "" : label + ": " + value; }
    private void add(List<String> values, String value) { if (!value.isEmpty() && !"No disponible".equals(value)) values.add(value); }
    private synchronized void renderMessage(String title, String message) { content.addView(section(title)); content.addView(text(message, 15, false)); }
    private synchronized void done() { if (--pending <= 0) loading(false); }
}
