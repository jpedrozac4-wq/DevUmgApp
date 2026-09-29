package gt.com.ro.devumgapp.academico.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
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
    private long courseId;
    private AcademicoApiService academicApi;
    private NotaApiService gradeApi;
    private final List<StudentChoice> students = new ArrayList<>();
    private int pending = 0;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SessionManager.getInstance().isLoggedIn()) { SessionManager.handleUnauthorized(this); return; }
        courseId = getIntent().getLongExtra(EXTRA_CURSO_ID, -1);
        if (courseId <= 0) { finish(); return; }
        setContentView(R.layout.activity_academic_overview);
        content = findViewById(R.id.academicContent); progress = findViewById(R.id.progressAcademic);
        MaterialToolbar toolbar = findViewById(R.id.toolbarAcademic);
        toolbar.setTitle(getIntent().getStringExtra(EXTRA_CURSO_NOMBRE));
        toolbar.setNavigationOnClickListener(v -> finish());
        academicApi = RetrofitClient.getClient().create(AcademicoApiService.class);
        gradeApi = RetrofitClient.getClient().create(NotaApiService.class);
        loadAll();
    }

    private void loadAll() {
        content.removeAllViews(); students.clear(); pending = 2; loading(true);
        content.addView(section("Estudiantes"));
        academicApi.estudiantesCurso(courseId).enqueue(callback(this::renderStudents));
        content.addView(section("Notas"));
        academicApi.notasCurso(courseId).enqueue(callback(this::renderGrades));
    }

    private Callback<JsonElement> callback(java.util.function.Consumer<JsonArray> success) {
        return new Callback<JsonElement>() {
            @Override public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful()) success.accept(AcademicJson.array(response.body()));
                else content.addView(text(AcademicErrors.from(DocenteCourseActivity.this, response), 15, false));
                done();
            }
            @Override public void onFailure(Call<JsonElement> call, Throwable error) { content.addView(text("No se pudo cargar una sección.", 15, false)); done(); }
        };
    }

    private void done() { if (--pending <= 0) loading(false); }

    private void renderStudents(JsonArray array) {
        if (array.size() == 0) { content.addView(text("No hay estudiantes inscritos en este curso.", 15, false), 1); return; }
        int position = 1;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            long id = AcademicJson.id(item, "id", "estudianteId");
            String code = AcademicJson.text(item, "codigoEstudiantil", "codigo");
            String name = AcademicJson.text(item, "nombreCompleto");
            if (name.isEmpty()) name = AcademicJson.text(item, "nombres", "nombre") + " " + AcademicJson.text(item, "apellidos", "apellido");
            students.add(new StudentChoice(id, (code + " · " + name).trim()));
            content.addView(card((code + " · " + name).trim(), "Estudiante del curso"), position++);
        }
        if (Permissions.has("NOTAS_CREAR")) {
            MaterialButton add = new MaterialButton(this);
            add.setText("Crear nota"); add.setIconResource(R.drawable.ic_add);
            add.setOnClickListener(v -> showGradeDialog(null));
            content.addView(add, position);
        }
    }

    private void renderGrades(JsonArray array) {
        if (array.size() == 0) { content.addView(text("No hay notas registradas.", 15, false)); return; }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            String type = AcademicJson.text(item, "tipoEvaluacion", "tipo");
            String score = AcademicJson.text(item, "calificacion", "nota");
            String student = AcademicJson.text(item, "estudianteNombre", "nombreEstudiante", "codigoEstudiantil");
            MaterialCardView card = card(type + " · " + score, student);
            if (Permissions.has("NOTAS_EDITAR")) card.setOnClickListener(v -> showGradeDialog(item));
            content.addView(card);
        }
    }

    private void showGradeDialog(JsonObject existing) {
        boolean edit = existing != null;
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), 0, dp(20), 0);
        Spinner studentSpinner = new Spinner(this);
        List<String> labels = new ArrayList<>(); for (StudentChoice student : students) labels.add(student.label);
        studentSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        studentSpinner.setEnabled(!edit); form.addView(studentSpinner);
        EditText type = input("Tipo de evaluación"); EditText score = input("Calificación (0-100)"); EditText observations = input("Observaciones");
        score.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(type); form.addView(score); form.addView(observations);
        if (edit) { type.setText(AcademicJson.text(existing, "tipoEvaluacion")); score.setText(AcademicJson.text(existing, "calificacion")); observations.setText(AcademicJson.text(existing, "observaciones")); }
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(edit ? "Editar nota" : "Crear nota").setView(form).setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> saveGrade(dialog, existing, studentSpinner, type, score, observations)));
        dialog.show();
    }

    private EditText input(String hint) { EditText input = new EditText(this); input.setHint(hint); input.setSingleLine(); return input; }

    private void saveGrade(androidx.appcompat.app.AlertDialog dialog, JsonObject existing, Spinner spinner, EditText type, EditText score, EditText observations) {
        double value;
        try { value = Double.parseDouble(score.getText().toString().trim()); } catch (Exception error) { score.setError("Ingresa una calificación válida."); return; }
        if (type.getText().toString().trim().isEmpty() || value < 0 || value > 100) { type.setError("Requerido"); return; }
        Call<NotaResponse> call;
        if (existing != null) call = gradeApi.actualizarNota(AcademicJson.id(existing, "id", "notaId"), new NotaUpdateRequest(type.getText().toString().trim(), value, observations.getText().toString().trim()));
        else {
            if (students.isEmpty() || spinner.getSelectedItemPosition() < 0) { Toast.makeText(this, "No hay estudiantes disponibles.", Toast.LENGTH_LONG).show(); return; }
            call = gradeApi.crearNota(new NotaRequest(students.get(spinner.getSelectedItemPosition()).id, courseId, Calendar.getInstance().get(Calendar.YEAR), type.getText().toString().trim(), value, observations.getText().toString().trim()));
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

    private static class StudentChoice { final long id; final String label; StudentChoice(long id, String label) { this.id = id; this.label = label; } }
}
