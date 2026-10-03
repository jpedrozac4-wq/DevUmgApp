package gt.com.ro.devumgapp.academico.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Collections;
import java.util.List;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.auth.ui.HomeActivity;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import gt.com.ro.devumgapp.auth.network.AuthApiService;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Curso;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Inscripcion;
import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.session.SessionManager;
import gt.com.ro.devumgapp.core.ui.ModuleNavigation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocenteAcademicActivity extends AcademicBaseActivity {
    private AcademicoApiService api;
    private EditText cycleInput;
    private LinearLayout filterControls;
    private ImageView filterChevron;
    private boolean filtersExpanded;
    private int destination;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SessionManager.getInstance().isLoggedIn()) { SessionManager.handleUnauthorized(this); return; }
        if (!Permissions.hasRole("DOCENTE")) { Toast.makeText(this, "Acceso exclusivo para docentes.", Toast.LENGTH_LONG).show(); finish(); return; }
        setContentView(R.layout.activity_academic_overview);
        content = findViewById(R.id.academicContent);
        progress = findViewById(R.id.progressAcademic);
        destination = getIntent().getIntExtra(HomeActivity.EXTRA_CURRENT_DESTINATION, R.id.nav_cursos);
        MaterialToolbar toolbar = findViewById(R.id.toolbarAcademic);
        toolbar.setTitle(screenTitle());
        ModuleNavigation.attach(this, toolbar);
        boolean gradesMode = destination == R.id.nav_notas;
        configureHero(screenTitle(), screenSubtitle(),
                gradesMode ? R.drawable.bg_nota_header : R.drawable.bg_curso_header,
                gradesMode ? R.drawable.ic_grade : R.drawable.ic_teacher,
                gradesMode ? R.color.dashboard_grade : R.color.dashboard_course);
        api = RetrofitClient.getClient().create(AcademicoApiService.class);
        refreshPermissionsAndLoad();
    }

    private String screenTitle() {
        if (destination == R.id.nav_notas) return "Notas";
        if (destination == R.id.nav_estudiantes) return "Estudiantes";
        if (destination == R.id.nav_inscripciones) return "Inscripciones";
        return "Mis cursos";
    }

    private String screenSubtitle() {
        if (destination == R.id.nav_notas) return "Selecciona un curso para consultar y administrar sus calificaciones.";
        if (destination == R.id.nav_estudiantes) return "Selecciona un curso para ver únicamente tus estudiantes asignados.";
        if (destination == R.id.nav_inscripciones) return "Selecciona un curso para consultar sus inscripciones activas.";
        return "Consulta tus cursos asignados y administra las notas de tus estudiantes.";
    }

    private void refreshPermissionsAndLoad() {
        loading(true);
        RetrofitClient.getClient().create(AuthApiService.class).me().enqueue(new Callback<LoginResponse>() {
            @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.code() == 401) { SessionManager.handleUnauthorized(DocenteAcademicActivity.this); return; }
                if (response.isSuccessful() && response.body() != null) {
                    SessionManager.getInstance().refreshProfile(response.body());
                }
                load(null);
            }
            @Override public void onFailure(Call<LoginResponse> call, Throwable error) { load(null); }
        });
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
                if (destination == R.id.nav_estudiantes || destination == R.id.nav_inscripciones) {
                    renderStudentsFromCourses(courses, cycle);
                    return;
                }
                content.addView(section(destination == R.id.nav_cursos ? "Cursos asignados" : "Selecciona un curso"));
                if (courses.isEmpty()) { content.addView(messageCard("Aún no tienes cursos asignados para este ciclo.", false)); return; }
                long requestedCourse = getIntent().getLongExtra("notificationDestinationId", -1);
                for (Curso course : courses) {
                    String detail = safe(course.carreraNombre) + " · Ciclo " + course.cicloAnio + " · " + course.creditos + " créditos";
                    MaterialCardView card = card(safe(course.codigo) + " · " + safe(course.nombre), detail);
                    card.setOnClickListener(v -> startActivity(new Intent(DocenteAcademicActivity.this, DocenteCourseActivity.class)
                            .putExtra(DocenteCourseActivity.EXTRA_CURSO_ID, course.id)
                            .putExtra(DocenteCourseActivity.EXTRA_CURSO_NOMBRE, course.nombre)
                            .putExtra(DocenteCourseActivity.EXTRA_CICLO_ANIO, course.cicloAnio)));
                    content.addView(card);
                    if (requestedCourse == course.id) card.performClick();
                }
            }
            @Override public void onFailure(Call<List<Curso>> call, Throwable error) { loading(false); showError("No se pudo conectar con el servidor."); }
        });
    }

    private void renderStudentsFromCourses(List<Curso> courses, Integer selectedCycle) {
        content.addView(section(destination == R.id.nav_estudiantes
                ? "Estudiantes asignados" : "Inscripciones de mis cursos"));
        if (courses.isEmpty()) {
            content.addView(messageCard("No tienes cursos asignados para este ciclo.", false));
            return;
        }

        loading(true);
        int[] pendingCourses = {courses.size()};
        int[] renderedStudents = {0};
        for (Curso course : courses) {
            Integer cycle = selectedCycle != null ? selectedCycle : course.cicloAnio;
            api.estudiantesCurso(course.id, cycle, 0, 200).enqueue(new Callback<PageResponse<Inscripcion>>() {
                @Override public void onResponse(Call<PageResponse<Inscripcion>> call,
                        Response<PageResponse<Inscripcion>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().content != null) {
                        for (Inscripcion enrollment : response.body().content) {
                            renderedStudents[0]++;
                            String title = safe(enrollment.estudianteCodigo) + " · " + safe(enrollment.estudianteNombre);
                            String detail = safe(course.codigo) + " · " + safe(course.nombre)
                                    + "\n" + safe(enrollment.grado) + " " + safe(enrollment.seccion)
                                    + " · " + safe(enrollment.estado);
                            content.addView(card(title, detail));
                        }
                    }
                    finishStudentsLoad(pendingCourses, renderedStudents);
                }

                @Override public void onFailure(Call<PageResponse<Inscripcion>> call, Throwable error) {
                    finishStudentsLoad(pendingCourses, renderedStudents);
                }
            });
        }
    }

    private void finishStudentsLoad(int[] pendingCourses, int[] renderedStudents) {
        if (--pendingCourses[0] > 0) return;
        loading(false);
        if (renderedStudents[0] == 0) {
            content.addView(messageCard("No hay estudiantes inscritos en tus cursos para este ciclo.", false));
        }
    }

    private void addCycleFilter(Integer cycle) {
        filtersExpanded = false;
        MaterialCardView filterCard = new MaterialCardView(this);
        filterCard.setCardBackgroundColor(getColor(R.color.dashboard_surface));
        filterCard.setCardElevation(dp(4));
        filterCard.setRadius(dp(8));
        filterCard.setStrokeWidth(dp(1));
        filterCard.setStrokeColor(getColor(R.color.dashboard_border));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout header = new LinearLayout(this);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView headerTitle = text("Filtrar por ciclo", 17, true);
        headerTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(headerTitle);
        filterChevron = new ImageView(this);
        filterChevron.setImageResource(R.drawable.ic_chevron_right);
        header.addView(filterChevron, new LinearLayout.LayoutParams(dp(24), dp(24)));
        header.setContentDescription("Mostrar filtros");
        header.setClickable(true);
        header.setFocusable(true);
        box.addView(header);

        filterControls = new LinearLayout(this);
        filterControls.setOrientation(LinearLayout.VERTICAL);
        filterControls.setVisibility(View.GONE);
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
        filterControls.addView(row);
        box.addView(filterControls);
        gt.com.ro.devumgapp.core.ui.FilterPanelTouch.bind(header, filterControls, () -> setFiltersExpanded(!filtersExpanded, header));
        filterCard.addView(box);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(12);
        filterCard.setLayoutParams(cardParams);
        content.addView(filterCard);
    }

    private void setFiltersExpanded(boolean expanded, View header) {
        filtersExpanded = expanded;
        header.setContentDescription(expanded ? "Ocultar filtros" : "Mostrar filtros");
        filterChevron.animate().rotation(expanded ? 90f : 0f).setDuration(180).start();
        if (expanded) {
            filterControls.setVisibility(View.VISIBLE);
            filterControls.setAlpha(0f);
            filterControls.setTranslationY(-dp(8));
            filterControls.animate().alpha(1f).translationY(0f).setDuration(180).start();
            return;
        }
        filterControls.animate().alpha(0f).translationY(-dp(8)).setDuration(140)
                .withEndAction(() -> filterControls.setVisibility(View.GONE)).start();
    }

    private Integer readCycle() {
        try { return Integer.valueOf(cycleInput.getText().toString().trim()); }
        catch (Exception ignored) { return null; }
    }

    private void showError(String message) { content.addView(messageCard(message, true)); Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private String safe(String value) { return value == null ? "" : value; }
}
