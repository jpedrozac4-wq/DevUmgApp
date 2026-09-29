package gt.com.ro.devumgapp.academico.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocenteAcademicActivity extends AcademicBaseActivity {
    private AcademicoApiService api;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SessionManager.getInstance().isLoggedIn()) { SessionManager.handleUnauthorized(this); return; }
        setContentView(R.layout.activity_academic_overview);
        content = findViewById(R.id.academicContent);
        progress = findViewById(R.id.progressAcademic);
        MaterialToolbar toolbar = findViewById(R.id.toolbarAcademic);
        toolbar.setTitle("Mis cursos");
        toolbar.setNavigationOnClickListener(v -> finish());
        api = RetrofitClient.getClient().create(AcademicoApiService.class);
        load();
    }

    private void load() {
        loading(true);
        api.cursosDocente().enqueue(new Callback<JsonElement>() {
            @Override public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                loading(false); content.removeAllViews();
                if (!response.isSuccessful()) { showError(AcademicErrors.from(DocenteAcademicActivity.this, response)); return; }
                JsonArray courses = AcademicJson.array(response.body());
                content.addView(section("Cursos asignados"));
                if (courses.size() == 0) { content.addView(text("Aún no tienes cursos asignados", 16, false)); return; }
                for (JsonElement element : courses) {
                    if (!element.isJsonObject()) continue;
                    JsonObject course = element.getAsJsonObject();
                    long id = AcademicJson.id(course, "id", "cursoId");
                    String code = AcademicJson.text(course, "codigo", "cursoCodigo");
                    String name = AcademicJson.text(course, "nombre", "cursoNombre");
                    String career = AcademicJson.text(course, "carreraNombre");
                    MaterialCardView card = card(code + " · " + name, career);
                    card.setOnClickListener(v -> startActivity(new Intent(DocenteAcademicActivity.this, DocenteCourseActivity.class)
                            .putExtra(DocenteCourseActivity.EXTRA_CURSO_ID, id)
                            .putExtra(DocenteCourseActivity.EXTRA_CURSO_NOMBRE, name)));
                    content.addView(card);
                }
            }
            @Override public void onFailure(Call<JsonElement> call, Throwable error) { loading(false); showError("No se pudo conectar con el servidor."); }
        });
    }

    private void showError(String message) { content.addView(text(message, 16, false)); Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
