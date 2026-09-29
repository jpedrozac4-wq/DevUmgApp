package gt.com.ro.devumgapp.usuario.ui;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.IdentityConflictMessage;
import gt.com.ro.devumgapp.core.network.ApiCallLogger;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.docente.dto.DocenteRequest;
import gt.com.ro.devumgapp.docente.dto.DocenteResponse;
import gt.com.ro.devumgapp.docente.network.DocenteApiService;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteRequest;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import gt.com.ro.devumgapp.rol.dto.RolResponse;
import gt.com.ro.devumgapp.rol.dto.RolResumenResponse;
import gt.com.ro.devumgapp.rol.network.RolApiService;
import gt.com.ro.devumgapp.usuario.dto.UsuarioAltaConjuntaRequest;
import gt.com.ro.devumgapp.usuario.dto.UsuarioAltaConjuntaResponse;
import gt.com.ro.devumgapp.usuario.dto.UsuarioRequest;
import gt.com.ro.devumgapp.usuario.dto.UsuarioResponse;
import gt.com.ro.devumgapp.usuario.network.UsuarioApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuarioFormActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "usuarioId";
    private static final long NEW_ID = -1L;
    private MaterialToolbar toolbar;
    private TextInputLayout tilUsername, tilEmail, tilPassword, tilNombre, tilApellido;
    private TextInputLayout tilTeacherCode, tilTeacherPhone, tilTeacherSpecialty;
    private TextInputLayout tilStudentCode, tilStudentId, tilStudentBirth, tilStudentPhone, tilStudentAddress;
    private TextInputEditText username, email, password, nombre, apellido;
    private TextInputEditText teacherCode, teacherPhone, teacherSpecialty;
    private TextInputEditText studentCode, studentIdNumber, studentBirth, studentPhone, studentAddress;
    private LinearLayout rolesLayout;
    private TextView rolesStatus, heroTitle;
    private View teacherPanel, studentPanel, hero, panel;
    private MaterialButton saveButton, cancelButton;
    private LinearProgressIndicator progress;
    private UsuarioApiService usuarioApi;
    private RolApiService rolApi;
    private DocenteApiService docenteApi;
    private EstudianteApiService estudianteApi;
    private final Map<Long, RolResponse> roles = new LinkedHashMap<>();
    private final Map<Long, CompoundButton> roleChecks = new LinkedHashMap<>();
    private final Set<Long> originalRoleIds = new LinkedHashSet<>();
    private final List<Call<?>> calls = new ArrayList<>();
    private long usuarioId = NEW_ID;
    private boolean editMode, loading, rolesReady, profilesReady;
    private boolean renderingRoles;
    private Long selectedRoleId;
    private int profileLookups;
    private UsuarioResponse loadedUsuario;
    private DocenteResponse linkedTeacher;
    private EstudianteResponse linkedStudent;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String permission = getIntent().hasExtra(EXTRA_ID) ? "USUARIOS_EDITAR" : "USUARIOS_CREAR";
        if (!Permissions.requireAll(this, Permissions.USUARIOS_LEER, permission)) return;
        setContentView(R.layout.activity_usuario_form);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(true); bars.setAppearanceLightNavigationBars(true);
        usuarioId = getIntent().getLongExtra(EXTRA_ID, NEW_ID); editMode = usuarioId != NEW_ID;
        usuarioApi = RetrofitClient.getClient().create(UsuarioApiService.class);
        rolApi = RetrofitClient.getClient().create(RolApiService.class);
        docenteApi = RetrofitClient.getClient().create(DocenteApiService.class);
        estudianteApi = RetrofitClient.getClient().create(EstudianteApiService.class);
        bindViews(); setupUi(); loadInitialData();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarUsuarioForm);
        tilUsername = findViewById(R.id.tilUsuarioUsername); tilEmail = findViewById(R.id.tilUsuarioEmail);
        tilPassword = findViewById(R.id.tilUsuarioPassword); tilNombre = findViewById(R.id.tilUsuarioNombre); tilApellido = findViewById(R.id.tilUsuarioApellido);
        username = findViewById(R.id.edtUsuarioUsername); email = findViewById(R.id.edtUsuarioEmail); password = findViewById(R.id.edtUsuarioPassword);
        nombre = findViewById(R.id.edtUsuarioNombre); apellido = findViewById(R.id.edtUsuarioApellido);
        rolesLayout = findViewById(R.id.layoutUsuarioRoles); rolesStatus = findViewById(R.id.txtUsuarioRolesEstado);
        teacherPanel = findViewById(R.id.panelUsuarioDocente); studentPanel = findViewById(R.id.panelUsuarioEstudiante);
        tilTeacherCode = findViewById(R.id.tilUsuarioCodigoDocente); tilTeacherPhone = findViewById(R.id.tilUsuarioTelefonoDocente);
        tilTeacherSpecialty = findViewById(R.id.tilUsuarioEspecialidadDocente); teacherCode = findViewById(R.id.edtUsuarioCodigoDocente);
        teacherPhone = findViewById(R.id.edtUsuarioTelefonoDocente); teacherSpecialty = findViewById(R.id.edtUsuarioEspecialidadDocente);
        tilStudentCode = findViewById(R.id.tilUsuarioCodigoEstudiante); tilStudentId = findViewById(R.id.tilUsuarioIdentificacionEstudiante);
        tilStudentBirth = findViewById(R.id.tilUsuarioFechaEstudiante); tilStudentPhone = findViewById(R.id.tilUsuarioTelefonoEstudiante);
        tilStudentAddress = findViewById(R.id.tilUsuarioDireccionEstudiante); studentCode = findViewById(R.id.edtUsuarioCodigoEstudiante);
        studentIdNumber = findViewById(R.id.edtUsuarioIdentificacionEstudiante); studentBirth = findViewById(R.id.edtUsuarioFechaEstudiante);
        studentPhone = findViewById(R.id.edtUsuarioTelefonoEstudiante); studentAddress = findViewById(R.id.edtUsuarioDireccionEstudiante);
        saveButton = findViewById(R.id.btnGuardarUsuario); cancelButton = findViewById(R.id.btnCancelarUsuario);
        progress = findViewById(R.id.progressUsuarioForm); heroTitle = findViewById(R.id.txtUsuarioFormHeroTitle);
        hero = findViewById(R.id.usuarioFormHero); panel = findViewById(R.id.cardUsuarioForm);
    }

    private void setupUi() {
        int title = editMode ? R.string.usuario_form_titulo_editar : R.string.usuario_form_titulo_crear;
        toolbar.setTitle(title); heroTitle.setText(title);
        tilPassword.setHint(editMode ? R.string.usuario_password_editar_hint : R.string.usuario_password_hint);
        toolbar.setNavigationOnClickListener(v -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) { @Override public void handleOnBackPressed() { finish(); } });
        saveButton.setOnClickListener(v -> save()); cancelButton.setOnClickListener(v -> finish());
        apellido.setOnEditorActionListener((v, action, event) -> { if (action == EditorInfo.IME_ACTION_DONE) { save(); return true; } return false; });
        hero.setAlpha(0f); hero.setTranslationY(20f); hero.animate().alpha(1f).translationY(0f).setDuration(360).start();
        panel.setAlpha(0f); panel.setTranslationY(18f); panel.animate().alpha(1f).translationY(0f).setStartDelay(120).setDuration(320).start();
    }

    private void loadInitialData() {
        setLoading(true); rolesReady = false; profilesReady = !editMode; loadRolesPage(0); if (editMode) loadUser();
    }

    private void loadRolesPage(int page) {
        Call<PageResponse<RolResponse>> call = rolApi.listar(null, true, page, 100); track(call);
        call.enqueue(new Callback<PageResponse<RolResponse>>() {
            @Override public void onResponse(Call<PageResponse<RolResponse>> c, Response<PageResponse<RolResponse>> r) {
                untrack(c); if (!r.isSuccessful() || r.body() == null) { fatal(UsuarioErrorMapper.fromResponse(UsuarioFormActivity.this, r)); return; }
                if (r.body().content != null) for (RolResponse role : r.body().content) if (role != null && role.activo) roles.put(role.id, role);
                if (!r.body().last && page + 1 < r.body().totalPages) loadRolesPage(page + 1);
                else { rolesReady = true; renderRoles(); finishInitialLoadIfReady(); }
            }
            @Override public void onFailure(Call<PageResponse<RolResponse>> c, Throwable t) { untrack(c); if (!c.isCanceled()) fatal(UsuarioErrorMapper.fromFailure(UsuarioFormActivity.this, t)); }
        });
    }

    private void loadUser() {
        Call<UsuarioResponse> call = usuarioApi.obtener(usuarioId); track(call);
        call.enqueue(new Callback<UsuarioResponse>() {
            @Override public void onResponse(Call<UsuarioResponse> c, Response<UsuarioResponse> r) {
                untrack(c); if (!r.isSuccessful() || r.body() == null) { fatal(UsuarioErrorMapper.fromResponse(UsuarioFormActivity.this, r)); return; }
                loadedUsuario = r.body(); fillIdentity(loadedUsuario); captureAssignedRoles(loadedUsuario.roles); renderRoles(); loadProfiles();
            }
            @Override public void onFailure(Call<UsuarioResponse> c, Throwable t) { untrack(c); if (!c.isCanceled()) fatal(UsuarioErrorMapper.fromFailure(UsuarioFormActivity.this, t)); }
        });
    }

    private void fillIdentity(UsuarioResponse u) {
        username.setText(u.username); email.setText(u.email); nombre.setText(u.nombre); apellido.setText(u.apellido); password.setText("");
    }

    private void captureAssignedRoles(List<RolResumenResponse> assigned) {
        originalRoleIds.clear(); if (assigned == null) return;
        for (RolResumenResponse role : assigned) {
            if (role == null) continue; originalRoleIds.add(role.id);
            if (!roles.containsKey(role.id)) { RolResponse inactive = new RolResponse(); inactive.id = role.id; inactive.codigo = role.codigo; inactive.nombre = role.nombre; roles.put(role.id, inactive); }
        }
    }

    private void renderRoles() {
        if (!rolesReady || (editMode && loadedUsuario == null)) return;
        renderingRoles = true;
        rolesLayout.removeAllViews(); roleChecks.clear();
        for (RolResponse role : roles.values()) {
            CompoundButton box = editMode ? new CheckBox(this) : new RadioButton(this);
            String code = role.codigo == null ? "" : role.codigo;
            String label = role.nombre == null || role.nombre.trim().isEmpty() ? code : code + " — " + role.nombre;
            if (!role.activo) label += " (inactivo, asignado)";
            box.setText(label);
            box.setChecked(editMode ? originalRoleIds.contains(role.id) : selectedRoleId != null && selectedRoleId == role.id);
            box.setEnabled(!editMode && role.activo && !loading);
            box.setOnCheckedChangeListener((button, checked) -> onRoleChanged(role, box, checked));
            rolesLayout.addView(box); roleChecks.put(role.id, box);
        }
        renderingRoles = false;
        if (roles.isEmpty()) { rolesStatus.setText("No hay roles activos disponibles."); rolesStatus.setVisibility(View.VISIBLE); }
        else if (editMode && originalRoleIds.size() > 1) {
            rolesStatus.setText("Esta cuenta tiene varios roles. Se conservan todos; la selección única solo aplica a altas nuevas.");
            rolesStatus.setVisibility(View.VISIBLE);
        } else if (!editMode) rolesStatus.setVisibility(View.GONE);
        updateAcademicVisibility();
    }

    private void onRoleChanged(RolResponse role, CompoundButton box, boolean checked) {
        if (renderingRoles || editMode) return;
        if (!checked) { box.setChecked(true); return; }
        if (selectedRoleId != null && selectedRoleId != role.id) clearProfileForRole(selectedRoleId);
        selectedRoleId = role.id;
        renderingRoles = true;
        for (Map.Entry<Long, CompoundButton> entry : roleChecks.entrySet()) {
            if (entry.getKey() != role.id) entry.getValue().setChecked(false);
        }
        renderingRoles = false;
        updateAcademicVisibility();
    }

    private void clearProfileForRole(long oldRoleId) {
        RolResponse oldRole = roles.get(oldRoleId);
        if (oldRole == null) return;
        if (isTeacher(oldRole)) {
            teacherCode.setText(""); teacherPhone.setText(""); teacherSpecialty.setText("");
            tilTeacherCode.setError(null); tilTeacherPhone.setError(null); tilTeacherSpecialty.setError(null);
        }
        if (isStudent(oldRole)) {
            studentCode.setText(""); studentIdNumber.setText(""); studentBirth.setText(""); studentPhone.setText(""); studentAddress.setText("");
            tilStudentCode.setError(null); tilStudentId.setError(null); tilStudentBirth.setError(null); tilStudentPhone.setError(null); tilStudentAddress.setError(null);
        }
    }

    private void blockRoleRemoval(String role, String profile) {
        String message = "No se puede retirar " + role + " mientras exista el perfil " + profile + " vinculado. El backend no ofrece una desvinculación segura desde este formulario.";
        rolesStatus.setText(message); rolesStatus.setVisibility(View.VISIBLE); UiNotifier.error(this, message);
    }

    private boolean isTeacher(RolResponse r) { return matchesCode(r.codigo, "DOCENTE"); }
    private boolean isStudent(RolResponse r) { return matchesCode(r.codigo, "ESTUDIANTE"); }
    private boolean matchesCode(String actual, String expected) { return actual != null && (actual.equalsIgnoreCase(expected) || actual.equalsIgnoreCase("ROLE_" + expected)); }
    private boolean roleSelected(String code) {
        for (Map.Entry<Long, RolResponse> entry : roles.entrySet()) { CompoundButton box = roleChecks.get(entry.getKey()); if (box != null && box.isChecked() && matchesCode(entry.getValue().codigo, code)) return true; }
        return false;
    }
    private void updateAcademicVisibility() {
        teacherPanel.setVisibility(roleSelected("DOCENTE") ? View.VISIBLE : View.GONE);
        studentPanel.setVisibility(roleSelected("ESTUDIANTE") ? View.VISIBLE : View.GONE);
    }

    private void loadProfiles() { linkedTeacher = null; linkedStudent = null; profileLookups = 0; findTeacherPage(0); findStudentPage(0); }
    private void findTeacherPage(int page) {
        profileLookups++; Call<PageResponse<DocenteResponse>> call = docenteApi.listarDocentes(null, null, page, 100); track(call);
        call.enqueue(new Callback<PageResponse<DocenteResponse>>() {
            @Override public void onResponse(Call<PageResponse<DocenteResponse>> c, Response<PageResponse<DocenteResponse>> r) {
                untrack(c); profileLookups--; if (!r.isSuccessful() || r.body() == null) { fatal("No se pudo verificar el perfil docente vinculado."); return; }
                if (r.body().content != null) for (DocenteResponse p : r.body().content) if (p != null && p.usuarioId != null && p.usuarioId == usuarioId) { linkedTeacher = p; break; }
                if (linkedTeacher == null && !r.body().last && page + 1 < r.body().totalPages) findTeacherPage(page + 1); finishProfileLookup();
            }
            @Override public void onFailure(Call<PageResponse<DocenteResponse>> c, Throwable t) { untrack(c); profileLookups--; if (!c.isCanceled()) fatal("No se pudo verificar el perfil docente vinculado."); }
        });
    }
    private void findStudentPage(int page) {
        profileLookups++; Call<PageResponse<EstudianteResponse>> call = estudianteApi.listarEstudiantes(null, null, page, 100); track(call);
        call.enqueue(new Callback<PageResponse<EstudianteResponse>>() {
            @Override public void onResponse(Call<PageResponse<EstudianteResponse>> c, Response<PageResponse<EstudianteResponse>> r) {
                untrack(c); profileLookups--; if (!r.isSuccessful() || r.body() == null) { fatal("No se pudo verificar el perfil estudiantil vinculado."); return; }
                if (r.body().content != null) for (EstudianteResponse p : r.body().content) if (p != null && p.usuarioId != null && p.usuarioId == usuarioId) { linkedStudent = p; break; }
                if (linkedStudent == null && !r.body().last && page + 1 < r.body().totalPages) findStudentPage(page + 1); finishProfileLookup();
            }
            @Override public void onFailure(Call<PageResponse<EstudianteResponse>> c, Throwable t) { untrack(c); profileLookups--; if (!c.isCanceled()) fatal("No se pudo verificar el perfil estudiantil vinculado."); }
        });
    }
    private void finishProfileLookup() {
        if (profileLookups != 0) return; profilesReady = true;
        if (linkedTeacher != null) { teacherCode.setText(linkedTeacher.codigoDocente); teacherPhone.setText(linkedTeacher.telefono); teacherSpecialty.setText(linkedTeacher.especialidad); }
        if (linkedStudent != null) { studentCode.setText(linkedStudent.codigoEstudiantil); studentIdNumber.setText(linkedStudent.numeroIdentificacion); studentBirth.setText(linkedStudent.fechaNacimiento); studentPhone.setText(linkedStudent.telefono); studentAddress.setText(linkedStudent.direccion); }
        renderRoles(); finishInitialLoadIfReady();
    }
    private void finishInitialLoadIfReady() { if (rolesReady && profilesReady && (!editMode || loadedUsuario != null)) setLoading(false); }

    private void save() {
        if (loading || !rolesReady || !profilesReady || !validate()) return;
        SgauDialog.confirmSave(this, editMode, "el usuario \"" + text(username).trim() + "\"", this::saveIdentity);
    }
    private void saveIdentity() {
        if (!editMode) { submitJointCreate(); return; }
        setLoading(true); String pwd = text(password);
        UsuarioRequest request = new UsuarioRequest(text(username).trim(), !editMode || !pwd.trim().isEmpty() ? pwd : null, text(email).trim(), text(nombre).trim(), text(apellido).trim());
        Call<UsuarioResponse> call = usuarioApi.actualizar(usuarioId, request); track(call);
        call.enqueue(new Callback<UsuarioResponse>() {
            @Override public void onResponse(Call<UsuarioResponse> c, Response<UsuarioResponse> r) { untrack(c); if (!r.isSuccessful() || r.body() == null || r.body().id <= 0) { operationError(r); return; } usuarioId = r.body().id; saveTeacherProfile(); }
            @Override public void onFailure(Call<UsuarioResponse> c, Throwable t) { untrack(c); if (!c.isCanceled()) networkError(); }
        });
    }

    private void submitJointCreate() {
        RolResponse selectedRole = selectedRoleId == null ? null : roles.get(selectedRoleId);
        if (selectedRole == null || !selectedRole.activo) { rolesStatus.setText("Selecciona un rol activo."); rolesStatus.setVisibility(View.VISIBLE); setLoading(false); return; }
        UsuarioAltaConjuntaRequest request = new UsuarioAltaConjuntaRequest();
        request.username = text(username).trim(); request.password = text(password);
        request.nombre = text(nombre).trim(); request.apellido = text(apellido).trim(); request.correo = text(email).trim();
        request.rolIds = new ArrayList<>(); request.rolIds.add(selectedRole.id);
        if (isTeacher(selectedRole)) {
            request.docente = new UsuarioAltaConjuntaRequest.Docente();
            request.docente.codigoDocente = text(teacherCode).trim(); request.docente.telefono = text(teacherPhone).trim();
            request.docente.especialidad = text(teacherSpecialty).trim();
        } else if (isStudent(selectedRole)) {
            request.estudiante = new UsuarioAltaConjuntaRequest.Estudiante();
            request.estudiante.codigoEstudiantil = text(studentCode).trim(); request.estudiante.numeroIdentificacion = text(studentIdNumber).trim();
            request.estudiante.fechaNacimiento = text(studentBirth).trim(); request.estudiante.telefono = text(studentPhone).trim();
            request.estudiante.direccion = text(studentAddress).trim();
        }
        setLoading(true);
        Call<UsuarioAltaConjuntaResponse> call = usuarioApi.altaConjunta(request); track(call);
        call.enqueue(new Callback<UsuarioAltaConjuntaResponse>() {
            @Override public void onResponse(Call<UsuarioAltaConjuntaResponse> c, Response<UsuarioAltaConjuntaResponse> r) {
                untrack(c);
                if (r.code() == 400) { showJointValidationErrors(r); return; }
                if (r.code() != 201 || r.body() == null || !hasExpectedJointIds(r.body(), selectedRole)) {
                    setLoading(false); UiNotifier.error(UsuarioFormActivity.this, r.isSuccessful()
                            ? "El backend no confirmó una respuesta completa del alta conjunta. No se confirmó el alta."
                            : IdentityConflictMessage.fromResponse(r)); return;
                }
                usuarioId = r.body().usuarioId;
                successJoint(r.body());
            }
            @Override public void onFailure(Call<UsuarioAltaConjuntaResponse> c, Throwable t) {
                untrack(c); if (!c.isCanceled()) networkError();
            }
        });
    }

    private boolean hasExpectedJointIds(UsuarioAltaConjuntaResponse response, RolResponse selectedRole) {
        if (response.usuarioId <= 0 || response.roles == null || response.roles.size() != 1
                || response.roles.get(0) == null || response.roles.get(0).id != selectedRole.id) return false;
        if (isTeacher(selectedRole)) return response.docenteId != null && response.docenteId > 0 && response.estudianteId == null;
        if (isStudent(selectedRole)) return response.estudianteId != null && response.estudianteId > 0 && response.docenteId == null;
        return response.docenteId == null && response.estudianteId == null;
    }

    private void showJointValidationErrors(Response<?> response) {
        String body = ApiCallLogger.readAndLogErrorBody("UsuarioApi", response);
        String message = "La solicitud contiene datos inválidos.";
        try {
            JsonObject root = new JsonParser().parse(body == null ? "{}" : body).getAsJsonObject();
            JsonElement backendMessage = root.get("message");
            if (backendMessage != null && !backendMessage.isJsonNull()) message = backendMessage.getAsString();
            JsonElement errorsElement = root.get("fieldErrors");
            if (errorsElement != null && errorsElement.isJsonObject()) {
                for (Map.Entry<String, JsonElement> error : errorsElement.getAsJsonObject().entrySet()) {
                    String detail = error.getValue().isJsonNull() ? "Dato inválido." : error.getValue().getAsString();
                    applyFieldError(error.getKey(), detail);
                    message += "\n" + error.getKey() + ": " + detail;
                }
            }
        } catch (Exception ignored) { }
        setLoading(false);
        UiNotifier.error(this, message);
    }

    private void applyFieldError(String field, String message) {
        switch (field) {
            case "username": tilUsername.setError(message); break;
            case "password": tilPassword.setError(message); break;
            case "nombre": tilNombre.setError(message); break;
            case "apellido": tilApellido.setError(message); break;
            case "correo": case "email": tilEmail.setError(message); break;
            case "rolIds": rolesStatus.setText(message); rolesStatus.setVisibility(View.VISIBLE); break;
            case "docente.codigoDocente": case "codigoDocente": tilTeacherCode.setError(message); break;
            case "docente.telefono": case "telefono": if (roleSelected("DOCENTE")) tilTeacherPhone.setError(message); else tilStudentPhone.setError(message); break;
            case "docente.especialidad": case "especialidad": tilTeacherSpecialty.setError(message); break;
            case "estudiante.codigoEstudiantil": case "codigoEstudiantil": tilStudentCode.setError(message); break;
            case "estudiante.numeroIdentificacion": case "numeroIdentificacion": tilStudentId.setError(message); break;
            case "estudiante.fechaNacimiento": case "fechaNacimiento": tilStudentBirth.setError(message); break;
            case "estudiante.direccion": case "direccion": tilStudentAddress.setError(message); break;
            default: rolesStatus.setText(field + ": " + message); rolesStatus.setVisibility(View.VISIBLE);
        }
    }

    private void successJoint(UsuarioAltaConjuntaResponse created) {
        setLoading(false);
        Intent result = new Intent().putExtra(EXTRA_ID, created.usuarioId)
                .putExtra("docenteId", created.docenteId == null ? -1L : created.docenteId)
                .putExtra("estudianteId", created.estudianteId == null ? -1L : created.estudianteId);
        setResult(RESULT_OK, result);
        String message = created.docenteId != null ? "Usuario y perfil docente vinculados correctamente."
                : created.estudianteId != null ? "Usuario y perfil estudiantil vinculados correctamente."
                : "Usuario y rol creados correctamente.";
        UiNotifier.success(this, message); finish();
    }
    private Set<Long> selectedRoleIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (Map.Entry<Long, CompoundButton> e : roleChecks.entrySet()) if (e.getValue().isChecked()) ids.add(e.getKey());
        return ids;
    }

    private void saveTeacherProfile() {
        if (!roleSelected("DOCENTE")) { saveStudentProfile(); return; }
        DocenteRequest req = new DocenteRequest();
        req.codigoDocente = text(teacherCode).trim();
        req.telefono = text(teacherPhone).trim();
        req.especialidad = text(teacherSpecialty).trim();
        Call<DocenteResponse> call;
        if (linkedTeacher == null) {
            req.nombre = text(nombre).trim(); req.apellido = text(apellido).trim(); req.email = text(email).trim();
            req.usuarioId = usuarioId; req.accesoApp = true; call = docenteApi.crearDocente(req);
        } else call = docenteApi.actualizarDocente(linkedTeacher.id, req);
        track(call); call.enqueue(new Callback<DocenteResponse>() {
            @Override public void onResponse(Call<DocenteResponse> c, Response<DocenteResponse> r) { untrack(c); if (!r.isSuccessful()) { operationError(r); return; } if (r.body() != null) linkedTeacher = r.body(); saveStudentProfile(); }
            @Override public void onFailure(Call<DocenteResponse> c, Throwable t) { untrack(c); if (!c.isCanceled()) networkError(); }
        });
    }
    private void saveStudentProfile() {
        if (!roleSelected("ESTUDIANTE")) { verifySavedState(); return; }
        EstudianteRequest req = new EstudianteRequest(); req.codigoEstudiantil = text(studentCode).trim(); req.numeroIdentificacion = text(studentIdNumber).trim();
        req.fechaNacimiento = text(studentBirth).trim(); req.telefono = text(studentPhone).trim(); req.direccion = text(studentAddress).trim();
        Call<EstudianteResponse> call;
        if (linkedStudent == null) {
            req.nombres = text(nombre).trim(); req.apellidos = text(apellido).trim(); req.correo = text(email).trim();
            req.usuarioId = usuarioId; req.accesoApp = true; call = estudianteApi.crearEstudiante(req);
        } else call = estudianteApi.actualizarEstudiante(linkedStudent.id, req);
        track(call); call.enqueue(new Callback<EstudianteResponse>() {
            @Override public void onResponse(Call<EstudianteResponse> c, Response<EstudianteResponse> r) { untrack(c); if (!r.isSuccessful()) { operationError(r); return; } if (r.body() != null) linkedStudent = r.body(); verifySavedState(); }
            @Override public void onFailure(Call<EstudianteResponse> c, Throwable t) { untrack(c); if (!c.isCanceled()) networkError(); }
        });
    }

    private void verifySavedState() {
        Call<UsuarioResponse> call = usuarioApi.obtener(usuarioId); track(call);
        call.enqueue(new Callback<UsuarioResponse>() {
            @Override public void onResponse(Call<UsuarioResponse> c, Response<UsuarioResponse> r) { untrack(c); if (!r.isSuccessful() || r.body() == null || !sameRoles(r.body().roles)) { failVerification("El servidor no confirmó los roles guardados."); return; } verifyTeacher(); }
            @Override public void onFailure(Call<UsuarioResponse> c, Throwable t) { untrack(c); if (!c.isCanceled()) failVerification("No se pudo volver a consultar el usuario."); }
        });
    }
    private boolean sameRoles(List<RolResumenResponse> actual) { Set<Long> ids = new LinkedHashSet<>(); if (actual != null) for (RolResumenResponse r : actual) if (r != null) ids.add(r.id); return ids.equals(selectedRoleIds()); }
    private void verifyTeacher() {
        if (!roleSelected("DOCENTE") || linkedTeacher == null || linkedTeacher.id <= 0) { if (roleSelected("DOCENTE")) failVerification("No se confirmó el perfil docente."); else verifyStudent(); return; }
        Call<DocenteResponse> call = docenteApi.obtenerDocente(linkedTeacher.id); track(call);
        call.enqueue(new Callback<DocenteResponse>() {
            @Override public void onResponse(Call<DocenteResponse> c, Response<DocenteResponse> r) { untrack(c); DocenteResponse p = r.body(); if (!r.isSuccessful() || p == null || p.usuarioId == null || p.usuarioId != usuarioId || !same(text(teacherCode), p.codigoDocente) || !same(text(teacherPhone), p.telefono) || !same(text(teacherSpecialty), p.especialidad)) { failVerification("El servidor no confirmó todos los datos del perfil docente vinculado."); return; } verifyStudent(); }
            @Override public void onFailure(Call<DocenteResponse> c, Throwable t) { untrack(c); if (!c.isCanceled()) failVerification("No se pudo volver a consultar el perfil docente."); }
        });
    }
    private void verifyStudent() {
        if (!roleSelected("ESTUDIANTE") || linkedStudent == null || linkedStudent.id <= 0) { if (roleSelected("ESTUDIANTE")) failVerification("No se confirmó el perfil estudiantil."); else success(); return; }
        Call<EstudianteResponse> call = estudianteApi.obtenerEstudianteDetalle(linkedStudent.id); track(call);
        call.enqueue(new Callback<EstudianteResponse>() {
            @Override public void onResponse(Call<EstudianteResponse> c, Response<EstudianteResponse> r) { untrack(c); EstudianteResponse p = r.body(); if (!r.isSuccessful() || p == null || p.usuarioId == null || p.usuarioId != usuarioId || !same(text(studentCode), p.codigoEstudiantil) || !same(text(studentIdNumber), p.numeroIdentificacion) || !same(text(studentBirth), p.fechaNacimiento) || !same(text(studentPhone), p.telefono) || !same(text(studentAddress), p.direccion)) { failVerification("El servidor no confirmó todos los datos del perfil estudiantil vinculado."); return; } success(); }
            @Override public void onFailure(Call<EstudianteResponse> c, Throwable t) { untrack(c); if (!c.isCanceled()) failVerification("No se pudo volver a consultar el perfil estudiantil."); }
        });
    }
    private void success() { setLoading(false); UiNotifier.success(this, editMode ? R.string.usuario_actualizado : R.string.usuario_creado); setResult(RESULT_OK); finish(); }

    private boolean validate() {
        boolean ok = true; ok = required(tilUsername, username, "Ingresa el nombre de usuario.") && ok; ok = required(tilEmail, email, "Ingresa el correo.") && ok;
        ok = required(tilNombre, nombre, "Ingresa el nombre.") && ok; ok = required(tilApellido, apellido, "Ingresa el apellido.") && ok;
        if (!editMode) ok = required(tilPassword, password, "Ingresa la contraseña.") && ok;
        if (!text(email).trim().isEmpty() && !looksLikeEmail(text(email).trim())) { tilEmail.setError("Ingresa un correo válido."); ok = false; }
        if ((!editMode || !text(password).trim().isEmpty()) && text(password).length() < 8) { tilPassword.setError("Usa al menos 8 caracteres."); ok = false; }
        Set<Long> chosenRoles = selectedRoleIds();
        if (chosenRoles.isEmpty() || (!editMode && chosenRoles.size() != 1)) { rolesStatus.setText("Selecciona exactamente un rol activo."); rolesStatus.setVisibility(View.VISIBLE); ok = false; }
        if (roleSelected("DOCENTE")) { ok = required(tilTeacherCode, teacherCode, "Ingresa el código docente.") && ok; ok = required(tilTeacherPhone, teacherPhone, "Ingresa el teléfono docente.") && ok; ok = required(tilTeacherSpecialty, teacherSpecialty, "Ingresa la especialidad.") && ok; }
        if (roleSelected("ESTUDIANTE")) { ok = required(tilStudentCode, studentCode, "Ingresa el código estudiantil.") && ok; ok = required(tilStudentId, studentIdNumber, "Ingresa la identificación.") && ok; ok = required(tilStudentBirth, studentBirth, "Ingresa la fecha de nacimiento.") && ok; ok = required(tilStudentPhone, studentPhone, "Ingresa el teléfono.") && ok; ok = required(tilStudentAddress, studentAddress, "Ingresa la dirección.") && ok; }
        return ok;
    }
    private boolean required(TextInputLayout layout, TextInputEditText field, String error) { boolean present = !text(field).trim().isEmpty(); layout.setError(present ? null : error); return present; }
    private boolean looksLikeEmail(String value) { int at = value.indexOf('@'); return at > 0 && value.indexOf('.', at + 1) > at + 1; }
    private String text(TextInputEditText field) { return field.getText() == null ? "" : field.getText().toString(); }
    private boolean same(String expected, String actual) { return expected.trim().equals(actual == null ? "" : actual.trim()); }
    private void setLoading(boolean value) {
        loading = value; progress.setVisibility(value ? View.VISIBLE : View.GONE); saveButton.setEnabled(!value && rolesReady && profilesReady); cancelButton.setEnabled(!value);
        TextInputLayout[] layouts = {tilUsername, tilEmail, tilPassword, tilNombre, tilApellido, tilTeacherCode, tilTeacherPhone, tilTeacherSpecialty, tilStudentCode, tilStudentId, tilStudentBirth, tilStudentPhone, tilStudentAddress};
        for (TextInputLayout layout : layouts) layout.setEnabled(!value);
        for (Map.Entry<Long, CompoundButton> entry : roleChecks.entrySet()) { RolResponse role = roles.get(entry.getKey()); entry.getValue().setEnabled(!value && !editMode && role != null && role.activo); }
    }
    private void operationError(Response<?> response) { setLoading(false); UiNotifier.error(this, IdentityConflictMessage.fromResponse(response)); }
    private void networkError() { setLoading(false); UiNotifier.error(this, "No se pudo conectar con el servidor. Verifica el estado antes de reintentar."); }
    private void failVerification(String message) { setLoading(false); UiNotifier.error(this, message + " No se anunció el guardado como exitoso."); }
    private void fatal(String message) { setLoading(false); UiNotifier.error(this, message); finish(); }
    private void track(Call<?> call) { calls.add(call); }
    private void untrack(Call<?> call) { calls.remove(call); }
    @Override protected void onDestroy() { for (Call<?> call : new ArrayList<>(calls)) call.cancel(); calls.clear(); super.onDestroy(); }
}
