package gt.com.ro.devumgapp.rol.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.permiso.dto.PermisoResumenResponse;
import gt.com.ro.devumgapp.permiso.network.PermisoApiService;
import gt.com.ro.devumgapp.rol.dto.PermisosRequest;
import gt.com.ro.devumgapp.rol.dto.RolRequest;
import gt.com.ro.devumgapp.rol.dto.RolResponse;
import gt.com.ro.devumgapp.rol.network.RolApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RolFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "rolId";
    public static final String EXTRA_PERMISSION_ONLY = "permissionOnly";
    private static final long NEW_ROL_ID = -1L;

    private MaterialToolbar toolbar;
    private TextInputLayout tilCodigo;
    private TextInputLayout tilNombre;
    private TextInputLayout tilDescripcion;
    private TextInputEditText edtCodigo;
    private TextInputEditText edtNombre;
    private TextInputEditText edtDescripcion;
    private TextInputLayout tilBuscarPermiso;
    private TextInputEditText edtBuscarPermiso;
    private RecyclerView recyclerPermisos;
    private PermissionSelectionAdapter permissionAdapter;
    private TextView txtPermisosSeleccionados;
    private TextView txtPermisosNota;
    private TextView txtPermisosVacio;
    private MaterialButton btnGuardar;
    private MaterialButton btnCancelar;
    private LinearProgressIndicator progressBar;
    private TextView txtHeroTitle;
    private View rolFormHero;
    private View rolFormPanel;

    private RolApiService apiService;
    private PermisoApiService permisoApiService;
    private Call<RolResponse> loadRolCall;
    private Call<List<PermisoResumenResponse>> loadActivesCall;
    private Call<List<PermisoResumenResponse>> loadAssignedCall;
    private Call<RolResponse> saveCall;
    private Call<RolResponse> assignCall;

    private long rolId = NEW_ROL_ID;
    private boolean editMode;
    private boolean loading;
    private boolean permissionsReady;
    private boolean permissionOnly;

    private List<PermisoResumenResponse> activePermissions = new ArrayList<>();
    private List<PermisoResumenResponse> assignedPermissions = new ArrayList<>();
    private final PermissionSelectionState permissionSelection = new PermissionSelectionState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String permission = getIntent().hasExtra(EXTRA_ID) ? "ROLES_EDITAR" : "ROLES_CREAR";
        permissionOnly = getIntent().getBooleanExtra(EXTRA_PERMISSION_ONLY, false);
        if (!Permissions.requireAll(this, Permissions.ROLES_LEER,
                permissionOnly ? "ROLES_ASIGNAR_PERMISOS" : permission)) return;
        setContentView(R.layout.activity_rol_form);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        rolId = getIntent().getLongExtra(EXTRA_ID, NEW_ROL_ID);
        editMode = rolId != NEW_ROL_ID;
        apiService = RetrofitClient.getClient().create(RolApiService.class);
        permisoApiService = RetrofitClient.getClient().create(PermisoApiService.class);
        bindViews();
        if (permissionOnly) {
            tilCodigo.setEnabled(false);
            tilNombre.setEnabled(false);
            tilDescripcion.setEnabled(false);
        }
        setupToolbar();
        setupActions();
        animateIntro();
        startLoading();
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarRolForm);
        tilCodigo = findViewById(R.id.tilRolCodigo);
        tilNombre = findViewById(R.id.tilRolNombre);
        tilDescripcion = findViewById(R.id.tilRolDescripcion);
        edtCodigo = findViewById(R.id.edtRolCodigo);
        edtNombre = findViewById(R.id.edtRolNombre);
        edtDescripcion = findViewById(R.id.edtRolDescripcion);
        tilBuscarPermiso = findViewById(R.id.tilBuscarPermisoRol);
        edtBuscarPermiso = findViewById(R.id.edtBuscarPermisoRol);
        recyclerPermisos = findViewById(R.id.recyclerPermisosRol);
        txtPermisosSeleccionados = findViewById(R.id.txtRolPermisosSeleccionados);
        txtPermisosNota = findViewById(R.id.txtRolPermisosNota);
        txtPermisosVacio = findViewById(R.id.txtRolPermisosVacio);
        btnGuardar = findViewById(R.id.btnGuardarRol);
        btnCancelar = findViewById(R.id.btnCancelarRol);
        progressBar = findViewById(R.id.progressRolForm);
        txtHeroTitle = findViewById(R.id.txtRolFormHeroTitle);
        rolFormHero = findViewById(R.id.rolFormHero);
        rolFormPanel = findViewById(R.id.cardRolForm);
        permissionAdapter = new PermissionSelectionAdapter(permissionSelection, this::updateSelectedCount);
        recyclerPermisos.setLayoutManager(new LinearLayoutManager(this));
        recyclerPermisos.setAdapter(permissionAdapter);
        recyclerPermisos.setHasFixedSize(false);
        if (!Permissions.has("ROLES_ASIGNAR_PERMISOS")) {
            tilBuscarPermiso.setVisibility(View.GONE);
            recyclerPermisos.setVisibility(View.GONE);
            txtPermisosSeleccionados.setVisibility(View.GONE);
            txtPermisosNota.setVisibility(View.GONE);
            txtPermisosVacio.setVisibility(View.GONE);
            permissionsReady = true;
        }
    }

    private void applyModeTitle() {
        int title = editMode
                ? R.string.rol_form_titulo_editar
                : R.string.rol_form_titulo_crear;
        toolbar.setTitle(title);
        txtHeroTitle.setText(title);
    }

    private void setupToolbar() {
        applyModeTitle();
        toolbar.setNavigationOnClickListener(view -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void animateIntro() {
        rolFormHero.setAlpha(0f);
        rolFormHero.setTranslationY(20f);
        rolFormHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        rolFormPanel.setAlpha(0f);
        rolFormPanel.setTranslationY(18f);
        rolFormPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(320)
                .start();
    }

    private void setupActions() {
        btnGuardar.setOnClickListener(view -> saveRol());
        btnCancelar.setOnClickListener(view -> finish());
        edtDescripcion.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveRol();
                return true;
            }
            return false;
        });
        edtBuscarPermiso.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                permissionAdapter.filter(value == null ? "" : value.toString());
                updatePermissionEmptyState();
            }
            @Override public void afterTextChanged(Editable value) { }
        });
    }

    private void startLoading() {
        if (editMode) {
            loadRolDetails();
            return;
        }
        if (Permissions.has("ROLES_ASIGNAR_PERMISOS")) loadActivePermissions();
        else permissionsReady = true;
    }

    private void loadRolDetails() {
        setLoading(true);
        loadRolCall = apiService.obtener(rolId);
        loadRolCall.enqueue(new Callback<RolResponse>() {
            @Override
            public void onResponse(Call<RolResponse> call, Response<RolResponse> response) {
                setLoading(false);
                loadRolCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    fillForm(response.body());
                    if (Permissions.has("ROLES_ASIGNAR_PERMISOS")) loadActivePermissions();
                    else permissionsReady = true;
                    return;
                }
                showErrorAndFinish(RolErrorMapper.fromResponse(RolFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<RolResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                loadRolCall = null;
                showErrorAndFinish(RolErrorMapper.fromFailure(RolFormActivity.this, throwable));
            }
        });
    }

    private void loadActivePermissions() {
        setLoading(true);
        loadActivesCall = permisoApiService.listarActivos();
        loadActivesCall.enqueue(new Callback<List<PermisoResumenResponse>>() {
            @Override
            public void onResponse(
                    Call<List<PermisoResumenResponse>> call,
                    Response<List<PermisoResumenResponse>> response) {
                setLoading(false);
                loadActivesCall = null;
                if (response.isSuccessful()) {
                    activePermissions = response.body() == null
                            ? new ArrayList<>()
                            : response.body();
                    if (editMode) {
                        loadAssignedPermissions();
                    } else {
                        renderPermissionCheckboxes();
                    }
                    return;
                }
                showPermissionsLoadError(
                        RolErrorMapper.fromResponse(RolFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<List<PermisoResumenResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                loadActivesCall = null;
                showPermissionsLoadError(
                        RolErrorMapper.fromFailure(RolFormActivity.this, throwable));
            }
        });
    }

    private void loadAssignedPermissions() {
        setLoading(true);
        loadAssignedCall = apiService.listarPermisos(rolId);
        loadAssignedCall.enqueue(new Callback<List<PermisoResumenResponse>>() {
            @Override
            public void onResponse(
                    Call<List<PermisoResumenResponse>> call,
                    Response<List<PermisoResumenResponse>> response) {
                setLoading(false);
                loadAssignedCall = null;
                if (response.isSuccessful()) {
                    assignedPermissions = response.body() == null
                            ? new ArrayList<>()
                            : response.body();
                    Set<Long> loadedIds = new HashSet<>();
                    for (PermisoResumenResponse permission : assignedPermissions) {
                        if (permission != null) {
                            loadedIds.add(permission.id);
                        }
                    }
                    permissionSelection.replace(loadedIds);
                    renderPermissionCheckboxes();
                    return;
                }
                showPermissionsLoadError(
                        RolErrorMapper.fromResponse(RolFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<List<PermisoResumenResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                loadAssignedCall = null;
                showPermissionsLoadError(
                        RolErrorMapper.fromFailure(RolFormActivity.this, throwable));
            }
        });
    }

    private void renderPermissionCheckboxes() {
        List<PermisoResumenResponse> merged = mergePermissions();
        boolean hasInactiveAssigned = false;
        Set<Long> inactiveIds = new HashSet<>();
        for (PermisoResumenResponse permission : merged) {
            boolean inactive = !isActivePermission(permission.id);
            if (inactive) { hasInactiveAssigned = true; inactiveIds.add(permission.id); }
        }
        permissionAdapter.submit(merged, inactiveIds);
        permissionsReady = true;
        txtPermisosNota.setVisibility(hasInactiveAssigned ? View.VISIBLE : View.GONE);
        txtPermisosVacio.setText(R.string.rol_permisos_vacio);
        updatePermissionEmptyState();
        updateSelectedCount();
        setLoading(false);
    }

    /**
     * The checkbox list is the union of the active permissions and the permissions the role
     * already has. {@code PUT /api/roles/{id}/permisos} replaces the whole set, so rendering only
     * the active list would silently drop an assignment to a permission that is now inactive.
     */
    private List<PermisoResumenResponse> mergePermissions() {
        Map<Long, PermisoResumenResponse> byId = new LinkedHashMap<>();
        for (PermisoResumenResponse permission : activePermissions) {
            if (permission != null) {
                byId.put(permission.id, permission);
            }
        }
        for (PermisoResumenResponse permission : assignedPermissions) {
            if (permission != null && !byId.containsKey(permission.id)) {
                byId.put(permission.id, permission);
            }
        }
        return new ArrayList<>(byId.values());
    }

    private boolean isActivePermission(long permisoId) {
        for (PermisoResumenResponse permission : activePermissions) {
            if (permission != null && permission.id == permisoId) {
                return true;
            }
        }
        return false;
    }

    private void showPermissionsLoadError(String message) {
        permissionsReady = false;
        permissionAdapter.submit(new ArrayList<>(), new HashSet<>());
        txtPermisosNota.setVisibility(View.GONE);
        txtPermisosVacio.setText(message);
        txtPermisosVacio.setVisibility(View.VISIBLE);
        setLoading(false);
        UiNotifier.error(this, message);
    }

    private void saveRol() {
        if (permissionOnly) {
            if (!Permissions.has("ROLES_ASIGNAR_PERMISOS") || loading || !permissionsReady) return;
            SgauDialog.confirm(this, R.drawable.ic_save,
                    getString(R.string.dialog_title_assign),
                    "Se actualizarán los permisos del rol seleccionado.",
                    getString(R.string.dialog_assign), () -> assignPermissions(false));
            return;
        }
        if (loading || !permissionsReady || !validateForm()) {
            return;
        }

        SgauDialog.confirmSave(this, rolId != NEW_ROL_ID,
                "el rol \"" + getText(edtNombre).trim() + "\"", this::submitRol);
    }

    private void submitRol() {
        if (loading) return;

        boolean createRequest = rolId == NEW_ROL_ID;
        setLoading(true);
        RolRequest request = new RolRequest(
                getText(edtCodigo).trim(),
                getText(edtNombre).trim(),
                getText(edtDescripcion).trim());
        saveCall = createRequest
                ? apiService.crear(request)
                : apiService.actualizar(rolId, request);
        saveCall.enqueue(new Callback<RolResponse>() {
            @Override
            public void onResponse(Call<RolResponse> call, Response<RolResponse> response) {
                setLoading(false);
                saveCall = null;
                if (response.isSuccessful()) {
                    if (createRequest) {
                        RolResponse created = response.body();
                        if (created == null || created.id <= 0) {
                            UiNotifier.error(
                                    RolFormActivity.this,
                                    getString(R.string.rol_error_server));
                            return;
                        }
                        rolId = created.id;
                    }
                    if (Permissions.has("ROLES_ASIGNAR_PERMISOS")) {
                        assignPermissions(createRequest);
                    } else {
                        UiNotifier.success(RolFormActivity.this,
                                createRequest ? R.string.rol_creado : R.string.rol_actualizado);
                        setResult(RESULT_OK);
                        finish();
                    }
                    return;
                }
                UiNotifier.error(
                        RolFormActivity.this,
                        RolErrorMapper.fromResponse(RolFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<RolResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                saveCall = null;
                UiNotifier.error(
                        RolFormActivity.this,
                        RolErrorMapper.fromFailure(RolFormActivity.this, throwable));
            }
        });
    }

    private void assignPermissions(boolean createdNow) {
        setLoading(true);
        PermisosRequest permisosRequest = new PermisosRequest(collectCheckedIds());
        assignCall = apiService.asignarPermisos(rolId, permisosRequest);
        assignCall.enqueue(new Callback<RolResponse>() {
            @Override
            public void onResponse(Call<RolResponse> call, Response<RolResponse> response) {
                setLoading(false);
                assignCall = null;
                if (response.isSuccessful()) {
                    UiNotifier.success(
                            RolFormActivity.this,
                            createdNow ? R.string.rol_creado : R.string.rol_actualizado);
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                reportPermissionAssignmentFailure(
                        createdNow,
                        RolErrorMapper.fromResponse(RolFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<RolResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                assignCall = null;
                reportPermissionAssignmentFailure(
                        createdNow,
                        RolErrorMapper.fromFailure(RolFormActivity.this, throwable));
            }
        });
    }

    private void reportPermissionAssignmentFailure(boolean createdNow, String message) {
        if (createdNow) {
            // The role was created but its permission set was not saved. The role now exists,
            // so switch the form to edit mode and keep it open: the user can retry the
            // assignment without losing the permissions already selected. RESULT_OK is set so
            // the list still refreshes (and shows the created role) if the user backs out.
            editMode = true;
            applyModeTitle();
            setResult(RESULT_OK);
            UiNotifier.error(
                    this,
                    getString(R.string.rol_creado_permisos_error, message));
            return;
        }
        // The role data was updated but the permission set was not: keep the screen open
        // so the user can retry the assignment.
        UiNotifier.error(
                this,
                getString(R.string.rol_actualizado_permisos_error, message));
    }

    private Set<Long> collectCheckedIds() {
        return permissionSelection.snapshot();
    }

    private void fillForm(RolResponse rol) {
        edtCodigo.setText(rol.codigo);
        edtNombre.setText(rol.nombre);
        edtDescripcion.setText(rol.descripcion);
    }

    private boolean validateForm() {
        boolean valid = true;
        String codigo = getText(edtCodigo).trim();
        String nombre = getText(edtNombre).trim();
        String descripcion = getText(edtDescripcion).trim();

        if (codigo.isEmpty()) {
            tilCodigo.setError(getString(R.string.rol_error_codigo_requerido));
            valid = false;
        } else if (codigo.length() < 3 || codigo.length() > 50) {
            tilCodigo.setError(getString(R.string.rol_error_codigo_longitud));
            valid = false;
        } else if (!codigo.matches("[A-Za-z0-9_]+")) {
            tilCodigo.setError(getString(R.string.rol_error_codigo_formato));
            valid = false;
        } else {
            tilCodigo.setError(null);
        }

        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.rol_error_nombre_requerido));
            valid = false;
        } else if (nombre.length() < 3 || nombre.length() > 100) {
            tilNombre.setError(getString(R.string.rol_error_nombre_longitud));
            valid = false;
        } else {
            tilNombre.setError(null);
        }

        if (descripcion.length() > 300) {
            tilDescripcion.setError(getString(R.string.rol_error_descripcion_longitud));
            valid = false;
        } else {
            tilDescripcion.setError(null);
        }

        return valid;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!loading && permissionsReady);
        btnCancelar.setEnabled(!loading);
        tilCodigo.setEnabled(!loading && !permissionOnly);
        tilNombre.setEnabled(!loading && !permissionOnly);
        tilDescripcion.setEnabled(!loading && !permissionOnly);
        tilBuscarPermiso.setEnabled(!loading);
        permissionAdapter.setItemsEnabled(!loading);
    }

    private void updateSelectedCount() {
        int count = permissionSelection.size();
        txtPermisosSeleccionados.setText(getResources().getQuantityString(
                R.plurals.rol_permisos_seleccionados, count, count));
    }

    private void updatePermissionEmptyState() {
        if (!permissionsReady) return;
        boolean empty = permissionAdapter.visiblePermissionCount() == 0;
        txtPermisosVacio.setText((edtBuscarPermiso.getText() != null && edtBuscarPermiso.getText().length() > 0)
                ? R.string.rol_permisos_sin_resultados : R.string.rol_permisos_vacio);
        txtPermisosVacio.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showErrorAndFinish(String message) {
        UiNotifier.error(this, message);
        finish();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private void cancelCalls() {
        if (loadRolCall != null) {
            loadRolCall.cancel();
        }
        if (loadActivesCall != null) {
            loadActivesCall.cancel();
        }
        if (loadAssignedCall != null) {
            loadAssignedCall.cancel();
        }
        if (saveCall != null) {
            saveCall.cancel();
        }
        if (assignCall != null) {
            assignCall.cancel();
        }
    }
}
