package gt.com.ro.devumgapp.academico.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Collections;
import java.util.List;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Curso;
import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocenteAcademicActivity extends AcademicBaseActivity {
    private AcademicoApiService api;
    private EditText cycleInput;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SessionManager.getInstance().isLoggedIn()) { SessionManager.handleUnauthorized(this); return; }
        if (!Permissions.hasRole("DOCENTE")) { Toast.makeText(this, "Acceso exclusivo para docentes.", Toast.LENGTH_LONG).show(); finish(); return; }
        setContentView(R.layout.activity_academic_overview);
        content = findViewById(R.id.academicContent);
        progress = findViewById(R.id.progressAcademic);
        MaterialToolbar toolbar = findViewById(R.id.toolbarAcademic);
        toolbar.setTitle("Mis cursos");
        toolbar.setNavigationOnClickListener(v -> finish());
        configureHero("Mis cursos", "Consulta tus cursos asignados y administra las notas de tus estudiantes.",
                R.drawable.bg_curso_header, R.drawable.ic_teacher, R.color.dashboard_course);
        api = RetrofitClient.getClient().create(AcademicoApiService.class);
        load(null);
    }

    private void load(Integer cycle) {
        loading(true);
        content.removeAllViews();
        addCycleFilter(cycle);
        api.cursosDocente(cycle).enqueue(new Callback<List<Curso>>() {
            @Override public void onResponse(Call<List<Curso>> call, Response<List<Curso>> response) {
                loading(false);
                if (!response.isSuccessful()) { showError(AcademicErrors.from(DocenteAcademicActivity.this, response)); return; }
                List<Curso> courses = response.body() == null ? Collections.emptyList() : response.body();
                content.addView(section("Cursos asignados"));
                if (courses.isEmpty()) { content.addView(messageCard("Aún no tienes cursos asignados para este ciclo.", false)); return; }
                for (Curso course : courses) {
                    String detail = safe(course.carreraNombre) + " · Ciclo " + course.cicloAnio + " · " + course.creditos + " créditos";
                    MaterialCardView card = card(safe(course.codigo) + " · " + safe(course.nombre), detail);
                    card.setOnClickListener(v -> startActivity(new Intent(DocenteAcademicActivity.this, DocenteCourseActivity.class)
                            .putExtra(DocenteCourseActivity.EXTRA_CURSO_ID, course.id)
                            .putExtra(DocenteCourseActivity.EXTRA_CURSO_NOMBRE, course.nombre)
                            .putExtra(DocenteCourseActivity.EXTRA_CICLO_ANIO, course.cicloAnio)));
                    content.addView(card);
                }
            }
            @Override public void onFailure(Call<List<Curso>> call, Throwable error) { loading(false); showError("No se pudo conectar con el servidor."); }
        });
    }

    private void addCycleFilter(Integer cycle) {
        MaterialCardView filterCard = new MaterialCardView(this);
        filterCard.setCardBackgroundColor(getColor(R.color.dashboard_surface));
        filterCard.setCardElevation(dp(4));
        filterCard.setRadius(dp(8));
        filterCard.setStrokeWidth(dp(1));
        filterCard.setStrokeColor(getColor(R.color.dashboard_border));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        box.addView(text("Filtrar cursos", 17, true));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint("Ciclo académico");
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        cycleInput = new EditText(this);
        cycleInput.setSingleLine();
        cycleInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (cycle != null) cycleInput.setText(String.valueOf(cycle));
        inputLayout.addView(cycleInput, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, -2, 1);
        inputParams.topMargin = dp(10);
        inputParams.rightMargin = dp(8);
        row.addView(inputLayout, inputParams);
        MaterialButton apply = new MaterialButton(this);
        apply.setText("Filtrar");
        apply.setIconResource(R.drawable.ic_search);
        apply.setCornerRadius(dp(8));
        apply.setOnClickListener(v -> load(readCycle()));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-2, dp(56));
        buttonParams.topMargin = dp(10);
        row.addView(apply, buttonParams);
        box.addView(row);
        filterCard.addView(box);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(12);
        filterCard.setLayoutParams(cardParams);
        content.addView(filterCard);
    }

    private Integer readCycle() {
        try { return Integer.valueOf(cycleInput.getText().toString().trim()); }
        catch (Exception ignored) { return null; }
    }

    private void showError(String message) { content.addView(messageCard(message, true)); Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private String safe(String value) { return value == null ? "" : value; }
}
