package gt.com.ro.devumgapp.academico.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Inscripcion;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Nota;
import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.session.SessionManager;
import gt.com.ro.devumgapp.nota.dto.NotaRequest;
import gt.com.ro.devumgapp.nota.dto.NotaResponse;
import gt.com.ro.devumgapp.nota.dto.NotaUpdateRequest;
import gt.com.ro.devumgapp.nota.network.NotaApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocenteCourseActivity extends AcademicBaseActivity {
    public static final String EXTRA_CURSO_ID = "cursoId";
    public static final String EXTRA_CURSO_NOMBRE = "cursoNombre";
    public static final String EXTRA_CICLO_ANIO = "cicloAnio";
    private static final int PAGE_SIZE = 200;
    private long courseId;
    private int cycleYear;
    private AcademicoApiService academicApi;
    private NotaApiService gradeApi;
    private final List<StudentChoice> students = new ArrayList<>();
    private int pending;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SessionManager.getInstance().isLoggedIn()) { SessionManager.handleUnauthorized(this); return; }
        if (!Permissions.hasRole("DOCENTE")) { Toast.makeText(this, "Acceso exclusivo para docentes.", Toast.LENGTH_LONG).show(); finish(); return; }
        courseId = getIntent().getLongExtra(EXTRA_CURSO_ID, -1);
        cycleYear = getIntent().getIntExtra(EXTRA_CICLO_ANIO, 0);
        if (courseId <= 0) { finish(); return; }
        setContentView(R.layout.activity_academic_overview);
        content = findViewById(R.id.academicContent);
        progress = findViewById(R.id.progressAcademic);
        MaterialToolbar toolbar = findViewById(R.id.toolbarAcademic);
        toolbar.setTitle(getIntent().getStringExtra(EXTRA_CURSO_NOMBRE));
        toolbar.setNavigationOnClickListener(v -> finish());
        configureHero(getIntent().getStringExtra(EXTRA_CURSO_NOMBRE),
                "Estudiantes inscritos y notas del ciclo " + cycleYear + ".",
                R.drawable.bg_nota_header, R.drawable.ic_grade, R.color.dashboard_grade);
        academicApi = RetrofitClient.getClient().create(AcademicoApiService.class);
        gradeApi = RetrofitClient.getClient().create(NotaApiService.class);
        loadAll();
    }

    private void loadAll() {
        content.removeAllViews(); students.clear(); pending = 2; loading(true);
        content.addView(section("Estudiantes del curso"));
        academicApi.estudiantesCurso(courseId, cycle(), 0, PAGE_SIZE).enqueue(new Callback<PageResponse<Inscripcion>>() {
            @Override public void onResponse(Call<PageResponse<Inscripcion>> call, Response<PageResponse<Inscripcion>> response) {
                if (response.isSuccessful()) renderStudents(items(response.body()));
                else showSectionError(AcademicErrors.from(DocenteCourseActivity.this, response));
                done();
            }
            @Override public void onFailure(Call<PageResponse<Inscripcion>> call, Throwable error) { showSectionError("No se pudieron cargar los estudiantes."); done(); }
        });
        content.addView(section("Notas del curso"));
        academicApi.notasCurso(courseId, cycle(), 0, PAGE_SIZE).enqueue(new Callback<PageResponse<Nota>>() {
            @Override public void onResponse(Call<PageResponse<Nota>> call, Response<PageResponse<Nota>> response) {
                if (response.isSuccessful()) renderGrades(items(response.body()));
                else showSectionError(AcademicErrors.from(DocenteCourseActivity.this, response));
                done();
            }
            @Override public void onFailure(Call<PageResponse<Nota>> call, Throwable error) { showSectionError("No se pudieron cargar las notas."); done(); }
        });
    }

    private Integer cycle() { return cycleYear > 0 ? cycleYear : null; }
    private <T> List<T> items(PageResponse<T> page) { return page == null || page.content == null ? Collections.emptyList() : page.content; }
    private void done() { if (--pending <= 0) loading(false); }
    private void showSectionError(String message) { content.addView(messageCard(message, true)); }

    private void renderStudents(List<Inscripcion> rows) {
        if (rows.isEmpty()) { content.addView(messageCard("No hay estudiantes inscritos en este curso.", false), 1); return; }
        int position = 1;
        for (Inscripcion row : rows) {
            students.add(new StudentChoice(row.estudianteId, safe(row.estudianteCodigo) + " · " + safe(row.estudianteNombre)));
            content.addView(card(safe(row.estudianteCodigo) + " · " + safe(row.estudianteNombre),
                    safe(row.grado) + " " + safe(row.seccion) + " · " + safe(row.estado)), position++);
        }
        if (Permissions.has("NOTAS_CREAR")) {
            MaterialButton add = new MaterialButton(this);
            add.setText("Crear nota"); add.setIconResource(R.drawable.ic_add);
            add.setCornerRadius(dp(8));
            add.setOnClickListener(v -> showGradeDialog(null));
            content.addView(add, position);
        }
    }

    private void renderGrades(List<Nota> notes) {
        if (notes.isEmpty()) { content.addView(messageCard("No hay notas registradas.", false)); return; }
        for (Nota note : notes) {
            MaterialCardView card = card(safe(note.tipoEvaluacion) + " · " + note.calificacion,
                    safe(note.estudianteCodigo) + " · " + safe(note.estudianteNombre));
            if (Permissions.has("NOTAS_EDITAR")) card.setOnClickListener(v -> showGradeDialog(note));
            content.addView(card);
        }
    }

    private void showGradeDialog(Nota existing) {
        boolean edit = existing != null;
        if (!edit && students.isEmpty()) { Toast.makeText(this, "No hay estudiantes inscritos disponibles.", Toast.LENGTH_LONG).show(); return; }
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), 0, dp(20), 0);
        Spinner studentSpinner = new Spinner(this);
        List<String> labels = new ArrayList<>(); for (StudentChoice student : students) labels.add(student.label);
        studentSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        studentSpinner.setEnabled(!edit); form.addView(studentSpinner);
        EditText type = input("Tipo de evaluación");
        EditText score = input("Calificación (0-100)");
        EditText observations = input("Observaciones");
        score.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(type); form.addView(score); form.addView(observations);
        if (edit) { type.setText(existing.tipoEvaluacion); score.setText(String.valueOf(existing.calificacion)); observations.setText(existing.observaciones); }
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(edit ? "Editar nota" : "Crear nota").setView(form).setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> saveGrade(dialog, existing, studentSpinner, type, score, observations)));
        dialog.show();
    }

    private EditText input(String hint) { EditText input = new EditText(this); input.setHint(hint); input.setSingleLine(); return input; }

    private void saveGrade(androidx.appcompat.app.AlertDialog dialog, Nota existing, Spinner spinner, EditText type, EditText score, EditText observations) {
        double value;
        try { value = Double.parseDouble(score.getText().toString().trim()); } catch (Exception error) { score.setError("Ingresa una calificación válida."); return; }
        String concept = type.getText().toString().trim();
        if (concept.isEmpty()) { type.setError("Requerido"); return; }
        if (value < 0 || value > 100) { score.setError("Debe estar entre 0 y 100"); return; }
        Call<NotaResponse> call;
        if (existing != null) call = gradeApi.actualizarNota(existing.id, new NotaUpdateRequest(concept, value, observations.getText().toString().trim()));
        else {
            StudentChoice student = students.get(spinner.getSelectedItemPosition());
            call = gradeApi.crearNota(new NotaRequest(student.id, courseId, cycleYear, concept, value, observations.getText().toString().trim()));
        }
        dialog.getButton(-1).setEnabled(false);
        call.enqueue(new Callback<NotaResponse>() {
            @Override public void onResponse(Call<NotaResponse> call, Response<NotaResponse> response) {
                if (response.isSuccessful()) { dialog.dismiss(); Toast.makeText(DocenteCourseActivity.this, "Nota guardada.", Toast.LENGTH_SHORT).show(); loadAll(); }
                else { dialog.getButton(-1).setEnabled(true); Toast.makeText(DocenteCourseActivity.this, AcademicErrors.from(DocenteCourseActivity.this, response), Toast.LENGTH_LONG).show(); }
            }
            @Override public void onFailure(Call<NotaResponse> call, Throwable error) { dialog.getButton(-1).setEnabled(true); Toast.makeText(DocenteCourseActivity.this, "No se pudo conectar con el servidor.", Toast.LENGTH_LONG).show(); }
        });
    }

    private String safe(String value) { return value == null ? "" : value; }
    private static class StudentChoice { final long id; final String label; StudentChoice(long id, String label) { this.id = id; this.label = label; } }
}
