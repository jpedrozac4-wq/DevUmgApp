package gt.com.ro.devumgapp.usuario.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.rol.dto.RolResumenResponse;
import gt.com.ro.devumgapp.rol.dto.RolResponse;
import gt.com.ro.devumgapp.rol.network.RolApiService;
import gt.com.ro.devumgapp.usuario.dto.RolesRequest;
import gt.com.ro.devumgapp.usuario.dto.UsuarioResponse;
import gt.com.ro.devumgapp.usuario.network.UsuarioApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuarioRolesActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "usuarioId";
    private static final long NO_USUARIO_ID = -1L;
    private static final int CATALOG_PAGE_SIZE = 100;

    private MaterialToolbar toolbar;
    private LinearLayout layoutRoles;
    private TextView txtRolesNota;
    private TextView txtRolesVacio;
    private MaterialButton btnGuardar;
    private MaterialButton btnCancelar;
    private LinearProgressIndicator progressBar;
    private View usuarioRolesHero;
    private View usuarioRolesPanel;

    private UsuarioApiService usuarioApiService;
    private RolApiService rolApiService;
    private Call<PageResponse<RolResponse>> loadCatalogueCall;
    private Call<List<RolResumenResponse>> loadAssignedCall;
    private Call<UsuarioResponse> assignCall;

    private long usuarioId = NO_USUARIO_ID;
    private boolean loading;
    private boolean rolesReady;

    private List<RolResponse> catalogueRoles = new ArrayList<>();
    private List<RolResumenResponse> assignedRoles = new ArrayList<>();
    private final Set<Long> assignedIds = new HashSet<>();
    private final Map<Long, MaterialCheckBox> checkboxById = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuario_roles);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        usuarioId = getIntent().getLongExtra(EXTRA_ID, NO_USUARIO_ID);
        usuarioApiService = RetrofitClient.getClient().create(UsuarioApiService.class);
        rolApiService = RetrofitClient.getClient().create(RolApiService.class);
        bindViews();
        setupToolbar();
        setupActions();
        animateIntro();
        if (usuarioId == NO_USUARIO_ID) {
            showErrorAndFinish(getString(R.string.usuario_error_not_found));
            return;
        }
        loadCatalogue();
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarUsuarioRoles);
        layoutRoles = findViewById(R.id.layoutRoles);
        txtRolesNota = findViewById(R.id.txtUsuarioRolesNota);
        txtRolesVacio = findViewById(R.id.txtUsuarioRolesVacio);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        progressBar = findViewById(R.id.progressUsuarioRoles);
        usuarioRolesHero = findViewById(R.id.usuarioRolesHero);
        usuarioRolesPanel = findViewById(R.id.cardUsuarioRoles);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(view -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void animateIntro() {
        usuarioRolesHero.setAlpha(0f);
        usuarioRolesHero.setTranslationY(20f);
        usuarioRolesHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        usuarioRolesPanel.setAlpha(0f);
        usuarioRolesPanel.setTranslationY(18f);
        usuarioRolesPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(320)
                .start();
    }

    private void setupActions() {
        btnGuardar.setOnClickListener(view -> saveRoles());
        btnCancelar.setOnClickListener(view -> finish());
    }

    private void loadCatalogue() {
        setLoading(true);
        loadCatalogueCall = rolApiService.listar(null, Boolean.TRUE, 0, CATALOG_PAGE_SIZE);
        loadCatalogueCall.enqueue(new Callback<PageResponse<RolResponse>>() {
            @Override
            public void onResponse(
                    Call<PageResponse<RolResponse>> call,
                    Response<PageResponse<RolResponse>> response) {
                setLoading(false);
                loadCatalogueCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    List<RolResponse> content = response.body().content;
                    catalogueRoles = content == null ? new ArrayList<>() : content;
                    loadAssignedRoles();
                    return;
                }
                showRolesLoadError(
                        UsuarioErrorMapper.fromResponse(UsuarioRolesActivity.this, response));
            }

            @Override
            public void onFailure(Call<PageResponse<RolResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                loadCatalogueCall = null;
                showRolesLoadError(
                        UsuarioErrorMapper.fromFailure(UsuarioRolesActivity.this, throwable));
            }
        });
    }

    private void loadAssignedRoles() {
        setLoading(true);
        loadAssignedCall = usuarioApiService.listarRoles(usuarioId);
        loadAssignedCall.enqueue(new Callback<List<RolResumenResponse>>() {
            @Override
            public void onResponse(
                    Call<List<RolResumenResponse>> call,
                    Response<List<RolResumenResponse>> response) {
                setLoading(false);
                loadAssignedCall = null;
                if (response.isSuccessful()) {
                    assignedRoles = response.body() == null
                            ? new ArrayList<>()
                            : response.body();
                    assignedIds.clear();
                    for (RolResumenResponse rol : assignedRoles) {
                        if (rol != null) {
                            assignedIds.add(rol.id);
                        }
                    }
                    renderRoleCheckboxes();
                    return;
                }
                showRolesLoadError(
                        UsuarioErrorMapper.fromResponse(UsuarioRolesActivity.this, response));
            }

            @Override
            public void onFailure(Call<List<RolResumenResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                loadAssignedCall = null;
                showRolesLoadError(
                        UsuarioErrorMapper.fromFailure(UsuarioRolesActivity.this, throwable));
            }
        });
    }

    private void renderRoleCheckboxes() {
        layoutRoles.removeAllViews();
        checkboxById.clear();

        List<RolResumenResponse> merged = mergeRoles();
        boolean hasInactiveAssigned = false;
        for (RolResumenResponse rol : merged) {
            boolean inactive = !isActiveRole(rol.id);
            if (inactive) {
                hasInactiveAssigned = true;
            }
            MaterialCheckBox checkBox = new MaterialCheckBox(this);
            checkBox.setId(View.generateViewId());
            checkBox.setText(buildRoleLabel(rol, inactive));
            checkBox.setChecked(assignedIds.contains(rol.id));
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layoutRoles.addView(checkBox);
            checkboxById.put(rol.id, checkBox);
        }

        txtRolesNota.setVisibility(hasInactiveAssigned ? View.VISIBLE : View.GONE);
        txtRolesVacio.setText(R.string.usuario_roles_vacio);
        txtRolesVacio.setVisibility(merged.isEmpty() ? View.VISIBLE : View.GONE);
        rolesReady = true;
        setLoading(false);
    }

    /**
     * The checkbox list is the union of the active role catalogue and the roles the user
     * already has. {@code PUT /api/usuarios/{id}/roles} replaces the whole set, so rendering
     * only the active catalogue would silently drop an assignment to a role that is now
     * inactive.
     */
    private List<RolResumenResponse> mergeRoles() {
        Map<Long, RolResumenResponse> byId = new LinkedHashMap<>();
        for (RolResponse rol : catalogueRoles) {
            if (rol != null) {
                byId.put(rol.id, new RolResumenResponse(rol.id, rol.codigo, rol.nombre));
            }
        }
        for (RolResumenResponse rol : assignedRoles) {
            if (rol != null && !byId.containsKey(rol.id)) {
                byId.put(rol.id, rol);
            }
        }
        return new ArrayList<>(byId.values());
    }

    private boolean isActiveRole(long rolId) {
        for (RolResponse rol : catalogueRoles) {
            if (rol != null && rol.id == rolId) {
                return true;
            }
        }
        return false;
    }

    private String buildRoleLabel(RolResumenResponse rol, boolean inactive) {
        String codigo = rol.codigo == null ? "" : rol.codigo;
        String nombre = rol.nombre == null ? "" : rol.nombre;
        String label;
        if (codigo.isEmpty()) {
            label = nombre;
        } else if (nombre.isEmpty()) {
            label = codigo;
        } else {
            label = getString(R.string.usuario_roles_item_formato, codigo, nombre);
        }
        if (inactive) {
            label = getString(
                    R.string.usuario_roles_item_inactivo_formato,
                    label,
                    getString(R.string.usuario_estado_inactivo));
        }
        return label;
    }

    private void showRolesLoadError(String message) {
        rolesReady = false;
        layoutRoles.removeAllViews();
        checkboxById.clear();
        txtRolesNota.setVisibility(View.GONE);
        txtRolesVacio.setText(message);
        txtRolesVacio.setVisibility(View.VISIBLE);
        setLoading(false);
        UiNotifier.error(this, message);
    }

    private void saveRoles() {
        if (loading || !rolesReady) {
            return;
        }
        setLoading(true);
        RolesRequest request = new RolesRequest(collectCheckedIds());
        assignCall = usuarioApiService.asignarRoles(usuarioId, request);
        assignCall.enqueue(new Callback<UsuarioResponse>() {
            @Override
            public void onResponse(Call<UsuarioResponse> call, Response<UsuarioResponse> response) {
                setLoading(false);
                assignCall = null;
                if (response.isSuccessful()) {
                    UiNotifier.success(
                            UsuarioRolesActivity.this,
                            R.string.usuario_roles_actualizados);
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                UiNotifier.error(
                        UsuarioRolesActivity.this,
                        UsuarioErrorMapper.fromResponse(UsuarioRolesActivity.this, response));
            }

            @Override
            public void onFailure(Call<UsuarioResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                assignCall = null;
                UiNotifier.error(
                        UsuarioRolesActivity.this,
                        UsuarioErrorMapper.fromFailure(UsuarioRolesActivity.this, throwable));
            }
        });
    }

    private Set<Long> collectCheckedIds() {
        Set<Long> checkedIds = new HashSet<>();
        for (Map.Entry<Long, MaterialCheckBox> entry : checkboxById.entrySet()) {
            if (entry.getValue().isChecked()) {
                checkedIds.add(entry.getKey());
            }
        }
        return checkedIds;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!loading && rolesReady);
        btnCancelar.setEnabled(!loading);
        layoutRoles.setEnabled(!loading);
        for (MaterialCheckBox checkBox : checkboxById.values()) {
            checkBox.setEnabled(!loading);
        }
    }

    private void showErrorAndFinish(String message) {
        UiNotifier.error(this, message);
        finish();
    }

    private void cancelCalls() {
        if (loadCatalogueCall != null) {
            loadCatalogueCall.cancel();
        }
        if (loadAssignedCall != null) {
            loadAssignedCall.cancel();
        }
        if (assignCall != null) {
            assignCall.cancel();
        }
    }
}
