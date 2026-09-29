package gt.com.ro.devumgapp.academico.ui;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

import android.widget.LinearLayout;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Carrera;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Curso;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.CursoPlan;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.DocenteCurso;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.EstudiantePerfil;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Inscripcion;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Nota;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.PlanCarrera;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Promedio;
import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstudianteAcademicActivity extends AcademicBaseActivity {
    private static final int PAGE_SIZE = 200;
    private AcademicoApiService api;
    private int pending;
    private final Map<String, LinearLayout> sections = new LinkedHashMap<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!SessionManager.getInstance().isLoggedIn()) { SessionManager.handleUnauthorized(this); return; }
        if (!Permissions.hasRole("ESTUDIANTE")) { Toast.makeText(this, "Acceso exclusivo para estudiantes.", Toast.LENGTH_LONG).show(); finish(); return; }
        setContentView(R.layout.activity_academic_overview);
        content = findViewById(R.id.academicContent); progress = findViewById(R.id.progressAcademic);
        MaterialToolbar toolbar = findViewById(R.id.toolbarAcademic);
        toolbar.setTitle("Mi carrera"); toolbar.setNavigationOnClickListener(v -> finish());
        configureHero("Mi carrera", "Consulta tu plan, cursos inscritos, docentes, notas y progreso académico.",
                R.drawable.bg_estudiante_header, R.drawable.ic_student, R.color.dashboard_student);
        api = RetrofitClient.getClient().create(AcademicoApiService.class);
        load();
    }

    private void load() {
        content.removeAllViews(); sections.clear(); pending = 8; loading(true);
        prepareSection("Perfil académico");
        prepareSection("Mi carrera");
        prepareSection("Cursos del plan");
        prepareSection("Mis cursos inscritos");
        prepareSection("Mis docentes");
        prepareSection("Mis inscripciones");
        prepareSection("Mis notas");
        prepareSection("Mi promedio");
        request(api.estudianteMe(), this::renderProfile, "Perfil académico");
        request(api.carreraEstudiante(), this::renderCareer, "Mi carrera");
        request(api.planCarreraEstudiante(), this::renderPlan, "Plan de carrera");
        request(api.cursosInscritosEstudiante(), this::renderEnrolledCourses, "Mis cursos inscritos");
        request(api.docentesEstudiante(), this::renderTeachers, "Mis docentes");
        request(api.inscripcionesEstudiante(0, PAGE_SIZE), page -> renderEnrollments(items(page)), "Inscripciones");
        request(api.notasEstudiante(0, PAGE_SIZE), page -> renderGrades(items(page)), "Notas");
        request(api.promedioEstudiante(), this::renderAverage, "Promedio");
    }

    private <T> void request(Call<T> call, java.util.function.Consumer<T> renderer, String section) {
        call.enqueue(new Callback<T>() {
            @Override public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) renderer.accept(response.body());
                else renderError(section, AcademicErrors.from(EstudianteAcademicActivity.this, response));
                done();
            }
            @Override public void onFailure(Call<T> call, Throwable error) { renderError(section, "No se pudo cargar esta sección."); done(); }
        });
    }

    private synchronized void renderProfile(EstudiantePerfil profile) {
        block("Perfil académico").addView(card(safe(profile.codigo) + " · " + safe(profile.nombres) + " " + safe(profile.apellidos), safe(profile.correo)));
    }

    private synchronized void renderCareer(Carrera career) {
        block("Mi carrera").addView(card(safe(career.codigo) + " · " + safe(career.nombre),
                career.duracionAnios + " años · " + safe(career.descripcion)));
    }

    private synchronized void renderPlan(PlanCarrera plan) {
        LinearLayout target = block("Cursos del plan");
        target.addView(card("Créditos del plan: " + plan.totalCreditosPlan,
                "Créditos inscritos: " + plan.totalCreditosInscritos));
        List<CursoPlan> courses = plan.cursosDisponibles == null ? Collections.emptyList() : plan.cursosDisponibles;
        if (courses.isEmpty()) { target.addView(messageCard("No hay cursos en el plan de carrera.", false)); return; }
        for (CursoPlan course : courses) {
            String teacher = empty(course.docenteNombre) ? "Sin docente asignado" : "Docente: " + course.docenteNombre;
            String detail = course.creditos + " créditos · Ciclo " + course.cicloAnio + " · " + teacher
                    + " · " + (course.inscrito ? "Inscrito" : "No inscrito");
            target.addView(card(safe(course.codigo) + " · " + safe(course.nombre), detail));
        }
    }

    private synchronized void renderEnrolledCourses(List<Curso> courses) {
        LinearLayout target = block("Mis cursos inscritos");
        if (courses == null || courses.isEmpty()) { target.addView(messageCard("No tienes cursos inscritos.", false)); return; }
        for (Curso course : courses) target.addView(card(safe(course.codigo) + " · " + safe(course.nombre),
                course.creditos + " créditos · Ciclo " + course.cicloAnio));
    }

    private synchronized void renderTeachers(List<DocenteCurso> teachers) {
        LinearLayout target = block("Mis docentes");
        if (teachers == null || teachers.isEmpty()) { target.addView(messageCard("No hay docentes asociados a tus cursos.", false)); return; }
        for (DocenteCurso teacher : teachers) target.addView(card(safe(teacher.nombre) + " " + safe(teacher.apellido),
                safe(teacher.cursoCodigo) + " · " + safe(teacher.cursoNombre) + " · Ciclo " + teacher.cicloAnio));
    }

    private synchronized void renderEnrollments(List<Inscripcion> rows) {
        LinearLayout target = block("Mis inscripciones");
        if (rows.isEmpty()) { target.addView(messageCard("No tienes inscripciones.", false)); return; }
        for (Inscripcion row : rows) target.addView(card(safe(row.cursoCodigo) + " · " + safe(row.cursoNombre),
                "Ciclo " + row.cicloAnio + " · " + safe(row.grado) + " " + safe(row.seccion) + " · " + safe(row.estado)));
    }

    private synchronized void renderGrades(List<Nota> notes) {
        LinearLayout target = block("Mis notas");
        if (notes.isEmpty()) { target.addView(messageCard("Aún no tienes notas registradas.", false)); return; }
        for (Nota note : notes) target.addView(card(safe(note.cursoCodigo) + " · " + safe(note.cursoNombre),
                safe(note.tipoEvaluacion) + ": " + format(note.calificacion) + (empty(note.observaciones) ? "" : " · " + note.observaciones)));
    }

    private synchronized void renderAverage(Promedio average) {
        block("Mi promedio").addView(card(format(average.promedio), average.cantidadNotas + " notas registradas"));
    }

    private void prepareSection(String title) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.addView(section(title));
        sections.put(title, block);
        content.addView(block);
    }
    private LinearLayout block(String title) { return sections.get(title); }
    private synchronized void renderError(String section, String message) {
        String target = section.equals("Plan de carrera") ? "Cursos del plan"
                : section.equals("Inscripciones") ? "Mis inscripciones"
                : section.equals("Notas") ? "Mis notas"
                : section.equals("Promedio") ? "Mi promedio" : section;
        LinearLayout block = sections.get(target);
        if (block != null) block.addView(messageCard(message, true));
    }
    private synchronized void done() { if (--pending <= 0) loading(false); }
    private <T> List<T> items(PageResponse<T> page) { return page == null || page.content == null ? Collections.emptyList() : page.content; }
    private String safe(String value) { return value == null ? "" : value; }
    private boolean empty(String value) { return value == null || value.trim().isEmpty(); }
    private String format(double value) { return String.format(Locale.getDefault(), "%.2f", value); }
}
