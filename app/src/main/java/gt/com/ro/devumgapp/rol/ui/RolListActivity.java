package gt.com.ro.devumgapp.rol.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.dto.EstadoRequest;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.core.ui.ModuleNavigation;
import gt.com.ro.devumgapp.rol.dto.RolResponse;
import gt.com.ro.devumgapp.rol.network.RolApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RolListActivity extends AppCompatActivity implements RolAdapter.Listener {

    private static final int PAGE_SIZE = 10;

    private MaterialToolbar toolbar;
    private TextInputEditText edtBuscar;
    private TextInputLayout tilBuscar;
    private MaterialButtonToggleGroup toggleEstado;
    private RecyclerView recyclerRoles;
    private LinearProgressIndicator progressBar;
    private TextView txtEmptyState;
    private TextView txtErrorState;
    private TextView txtPageInfo;
    private View rolHero;
    private View rolFilters;
    private View rolFiltersHeader;
    private View rolFilterControls;
    private MaterialButton btnBuscar;
    private MaterialButton btnAgregar;
    private MaterialButton btnAnterior;
    private MaterialButton btnSiguiente;

    private RolApiService apiService;
    private RolAdapter adapter;
    private Call<PageResponse<RolResponse>> listCall;
    private final Map<Long, Call<RolResponse>> statusCalls = new HashMap<>();
    private ActivityResultLauncher<Intent> formLauncher;

    private int currentPage;
    private int totalPages;
    private boolean loading;
    private Boolean activeFilter;
    private boolean firstResume = true;
    private boolean filtersExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "ROLES_LEER")) return;
        setContentView(R.layout.activity_rol_list);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        apiService = RetrofitClient.getClient().create(RolApiService.class);
        formLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadRoles(currentPage);
                    }
                });
        bindViews();
        setupToolbar();
        setupRecycler();
        setupFilters();
        setupActions();
        animateIntro();
        loadRoles(0);
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
            return;
        }
        loadRoles(currentPage);
    }

    @Override
    public void onEdit(RolResponse rol) {
        Intent intent = new Intent(this, RolFormActivity.class);
        intent.putExtra(RolFormActivity.EXTRA_ID, rol.id);
        formLauncher.launch(intent);
    }

    @Override
    public void onManagePermissions(RolResponse rol) {
        if (!Permissions.has("ROLES_ASIGNAR_PERMISOS")) return;
        Intent intent = new Intent(this, RolFormActivity.class);
        intent.putExtra(RolFormActivity.EXTRA_ID, rol.id);
        intent.putExtra(RolFormActivity.EXTRA_PERMISSION_ONLY, true);
        formLauncher.launch(intent);
    }

    @Override
    public void onToggleStatus(RolResponse rol) {
        if (statusCalls.containsKey(rol.id)) {
            return;
        }
        SgauDialog.confirmState(this, !rol.activo,
                "el rol \"" + rol.nombre + "\"", () -> changeRolStatus(rol));
    }

    private void changeRolStatus(RolResponse rol) {
        if (statusCalls.containsKey(rol.id)) return;
        adapter.setStatusChanging(rol.id, true);
        EstadoRequest estadoRequest = new EstadoRequest();
        estadoRequest.activo = !rol.activo;
        Call<RolResponse> call = apiService.cambiarEstado(rol.id, estadoRequest);
        statusCalls.put(rol.id, call);
        call.enqueue(new Callback<RolResponse>() {
            @Override
            public void onResponse(Call<RolResponse> call, Response<RolResponse> response) {
                adapter.setStatusChanging(rol.id, false);
                statusCalls.remove(rol.id);
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        adapter.replace(response.body());
                    } else {
                        loadRoles(currentPage);
                    }
                    UiNotifier.success(RolListActivity.this, rol.activo
                            ? R.string.rol_desactivado
                            : R.string.rol_activado);
                    return;
                }
                UiNotifier.error(
                        RolListActivity.this,
                        RolErrorMapper.fromResponse(RolListActivity.this, response));
            }

            @Override
            public void onFailure(Call<RolResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                adapter.setStatusChanging(rol.id, false);
                statusCalls.remove(rol.id);
                UiNotifier.error(
                        RolListActivity.this,
                        RolErrorMapper.fromFailure(RolListActivity.this, throwable));
            }
        });
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarRoles);
        tilBuscar = findViewById(R.id.tilBuscarRol);
        edtBuscar = findViewById(R.id.edtBuscarRol);
        toggleEstado = findViewById(R.id.toggleEstadoRoles);
        recyclerRoles = findViewById(R.id.recyclerRoles);
        progressBar = findViewById(R.id.progressRoles);
        txtEmptyState = findViewById(R.id.txtRolesEmpty);
        txtErrorState = findViewById(R.id.txtRolesError);
        txtPageInfo = findViewById(R.id.txtRolesPageInfo);
        rolHero = findViewById(R.id.rolHero);
        rolFilters = findViewById(R.id.rolFilters);
        rolFiltersHeader = findViewById(R.id.rolFiltersHeader);
        rolFilterControls = findViewById(R.id.rolFilterControls);
        btnBuscar = findViewById(R.id.btnBuscarRol);
        btnAgregar = findViewById(R.id.btnAgregarRol);
        btnAgregar.setVisibility(Permissions.has("ROLES_CREAR") ? View.VISIBLE : View.GONE);
        btnAnterior = findViewById(R.id.btnRolesAnterior);
        btnSiguiente = findViewById(R.id.btnRolesSiguiente);
    }

    private void setupToolbar() {
        toolbar.setTitle("");
        ModuleNavigation.attach(this, toolbar);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void animateIntro() {
        rolHero.setAlpha(0f);
        rolHero.setTranslationY(20f);
        rolHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        rolFilters.setAlpha(0f);
        rolFilters.setTranslationY(16f);
        rolFilters.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(110)
                .setDuration(300)
                .start();
    }

    private void setupRecycler() {
        adapter = new RolAdapter(null, this);
        recyclerRoles.setLayoutManager(new LinearLayoutManager(this));
        recyclerRoles.setAdapter(adapter);
    }

    private void setupFilters() {
        toggleEstado.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            Boolean nextFilter = resolveActiveFilter(checkedId);
            if (Objects.equals(activeFilter, nextFilter)) {
                return;
            }
            activeFilter = nextFilter;
            loadRoles(0);
        });
    }

    private void setupActions() {
        rolFiltersHeader.setOnClickListener(view -> setFiltersExpanded(!filtersExpanded));
        btnBuscar.setOnClickListener(view -> loadRoles(0));
        tilBuscar.setEndIconOnClickListener(view -> loadRoles(0));
        edtBuscar.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadRoles(0);
                return true;
            }
            return false;
        });
        btnAgregar.setOnClickListener(view ->
                formLauncher.launch(new Intent(this, RolFormActivity.class)));
        btnAnterior.setOnClickListener(view -> {
            if (currentPage > 0) {
                loadRoles(currentPage - 1);
            }
        });
        btnSiguiente.setOnClickListener(view -> {
            if (currentPage + 1 < totalPages) {
                loadRoles(currentPage + 1);
            }
        });
    }

    private void loadRoles(int page) {
        if (loading) {
            return;
        }
        if (listCall != null) {
            listCall.cancel();
        }
        setLoading(true);
        txtErrorState.setVisibility(View.GONE);

        String searchText = getText(edtBuscar).trim();
        listCall = apiService.listar(
                searchText.isEmpty() ? null : searchText,
                activeFilter,
                Math.max(page, 0),
                PAGE_SIZE);
        listCall.enqueue(new Callback<PageResponse<RolResponse>>() {
            @Override
            public void onResponse(
                    Call<PageResponse<RolResponse>> call,
                    Response<PageResponse<RolResponse>> response) {
                setLoading(false);
                listCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    renderPage(response.body());
                    return;
                }
                showError(RolErrorMapper.fromResponse(RolListActivity.this, response));
            }

            @Override
            public void onFailure(Call<PageResponse<RolResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                listCall = null;
                showError(RolErrorMapper.fromFailure(RolListActivity.this, throwable));
            }
        });
    }

    private void renderPage(PageResponse<RolResponse> page) {
        currentPage = page.number;
        totalPages = page.totalPages;
        List<RolResponse> content = page.content;
        adapter.submitList(content);
        boolean empty = content == null || content.isEmpty();
        txtEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerRoles.setVisibility(empty ? View.GONE : View.VISIBLE);
        updatePaginationControls();
    }

    private void showError(String message) {
        adapter.submitList(null);
        recyclerRoles.setVisibility(View.GONE);
        txtEmptyState.setVisibility(View.GONE);
        txtErrorState.setText(message);
        txtErrorState.setVisibility(View.VISIBLE);
        currentPage = 0;
        totalPages = 0;
        updatePaginationControls();
        UiNotifier.error(this, message);
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnBuscar.setEnabled(!loading);
        btnAgregar.setEnabled(!loading);
        btnAnterior.setEnabled(!loading && currentPage > 0);
        btnSiguiente.setEnabled(!loading && currentPage + 1 < totalPages);
        toggleEstado.setEnabled(!loading);
        edtBuscar.setEnabled(!loading);
    }

    private void setFiltersExpanded(boolean expanded) {
        filtersExpanded = expanded;
        if (expanded) {
            rolFilterControls.setVisibility(View.VISIBLE);
            rolFilterControls.setAlpha(0f);
            rolFilterControls.setTranslationY(-8f);
            rolFilterControls.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
            return;
        }
        rolFilterControls.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(140)
                .withEndAction(() -> rolFilterControls.setVisibility(View.GONE))
                .start();
    }

    private void updatePaginationControls() {
        int visiblePage = totalPages == 0 ? 0 : currentPage + 1;
        txtPageInfo.setText(getString(R.string.rol_pagina_info, visiblePage, totalPages));
        btnAnterior.setEnabled(!loading && currentPage > 0);
        btnSiguiente.setEnabled(!loading && currentPage + 1 < totalPages);
    }

    private Boolean resolveActiveFilter(int checkedId) {
        if (checkedId == R.id.btnFiltroActivosRoles) {
            return Boolean.TRUE;
        }
        if (checkedId == R.id.btnFiltroInactivosRoles) {
            return Boolean.FALSE;
        }
        return null;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private void cancelCalls() {
        if (listCall != null) {
            listCall.cancel();
        }
        for (Call<RolResponse> call : statusCalls.values()) {
            call.cancel();
        }
        statusCalls.clear();
    }
}
