package gt.com.ro.devumgapp.permiso.ui;

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

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.dto.EstadoRequest;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.permiso.dto.PermisoResponse;
import gt.com.ro.devumgapp.permiso.network.PermisoApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PermisoListActivity extends AppCompatActivity implements PermisoAdapter.Listener {

    private static final int PAGE_SIZE = 10;

    private MaterialToolbar toolbar;
    private TextInputEditText edtBuscar;
    private TextInputLayout tilBuscar;
    private MaterialButtonToggleGroup toggleEstado;
    private RecyclerView recyclerPermisos;
    private LinearProgressIndicator progressBar;
    private TextView txtEmptyState;
    private TextView txtErrorState;
    private TextView txtPageInfo;
    private View permisoHero;
    private View permisoFilters;
    private View permisoFiltersHeader;
    private View permisoFilterControls;
    private MaterialButton btnBuscar;
    private MaterialButton btnAgregar;
    private MaterialButton btnAnterior;
    private MaterialButton btnSiguiente;

    private PermisoApiService apiService;
    private PermisoAdapter adapter;
    private Call<PageResponse<PermisoResponse>> listCall;
    private final Map<Long, Call<PermisoResponse>> statusCalls = new HashMap<>();
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
        setContentView(R.layout.activity_permiso_list);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        apiService = RetrofitClient.getClient().create(PermisoApiService.class);
        formLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadPermisos(currentPage);
                    }
                });
        bindViews();
        setupToolbar();
        setupRecycler();
        setupFilters();
        setupActions();
        animateIntro();
        loadPermisos(0);
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
        loadPermisos(currentPage);
    }

    @Override
    public void onEdit(PermisoResponse permiso) {
        Intent intent = new Intent(this, PermisoFormActivity.class);
        intent.putExtra(PermisoFormActivity.EXTRA_ID, permiso.id);
        formLauncher.launch(intent);
    }

    @Override
    public void onToggleStatus(PermisoResponse permiso) {
        if (statusCalls.containsKey(permiso.id)) {
            return;
        }
        adapter.setStatusChanging(permiso.id, true);
        EstadoRequest estadoRequest = new EstadoRequest();
        estadoRequest.activo = !permiso.activo;
        Call<PermisoResponse> call = apiService.cambiarEstado(permiso.id, estadoRequest);
        statusCalls.put(permiso.id, call);
        call.enqueue(new Callback<PermisoResponse>() {
            @Override
            public void onResponse(Call<PermisoResponse> call, Response<PermisoResponse> response) {
                adapter.setStatusChanging(permiso.id, false);
                statusCalls.remove(permiso.id);
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        adapter.replace(response.body());
                    } else {
                        loadPermisos(currentPage);
                    }
                    UiNotifier.success(PermisoListActivity.this, R.string.permiso_estado_actualizado);
                    return;
                }
                UiNotifier.error(
                        PermisoListActivity.this,
                        PermisoErrorMapper.fromResponse(PermisoListActivity.this, response));
            }

            @Override
            public void onFailure(Call<PermisoResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                adapter.setStatusChanging(permiso.id, false);
                statusCalls.remove(permiso.id);
                UiNotifier.error(
                        PermisoListActivity.this,
                        PermisoErrorMapper.fromFailure(PermisoListActivity.this, throwable));
            }
        });
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarPermisos);
        tilBuscar = findViewById(R.id.tilBuscarPermiso);
        edtBuscar = findViewById(R.id.edtBuscarPermiso);
        toggleEstado = findViewById(R.id.toggleEstadoPermisos);
        recyclerPermisos = findViewById(R.id.recyclerPermisos);
        progressBar = findViewById(R.id.progressPermisos);
        txtEmptyState = findViewById(R.id.txtPermisosEmpty);
        txtErrorState = findViewById(R.id.txtPermisosError);
        txtPageInfo = findViewById(R.id.txtPermisosPageInfo);
        permisoHero = findViewById(R.id.permisoHero);
        permisoFilters = findViewById(R.id.permisoFilters);
        permisoFiltersHeader = findViewById(R.id.permisoFiltersHeader);
        permisoFilterControls = findViewById(R.id.permisoFilterControls);
        btnBuscar = findViewById(R.id.btnBuscarPermiso);
        btnAgregar = findViewById(R.id.btnAgregarPermiso);
        btnAnterior = findViewById(R.id.btnPermisosAnterior);
        btnSiguiente = findViewById(R.id.btnPermisosSiguiente);
    }

    private void setupToolbar() {
        toolbar.setTitle("");
        toolbar.setNavigationOnClickListener(view -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void animateIntro() {
        permisoHero.setAlpha(0f);
        permisoHero.setTranslationY(20f);
        permisoHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        permisoFilters.setAlpha(0f);
        permisoFilters.setTranslationY(16f);
        permisoFilters.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(110)
                .setDuration(300)
                .start();
    }

    private void setupRecycler() {
        adapter = new PermisoAdapter(null, this);
        recyclerPermisos.setLayoutManager(new LinearLayoutManager(this));
        recyclerPermisos.setAdapter(adapter);
    }

    private void setupFilters() {
        toggleEstado.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            Boolean nextFilter = resolveActiveFilter(checkedId);
            if (activeFilter == nextFilter || (activeFilter != null && activeFilter.equals(nextFilter))) {
                return;
            }
            activeFilter = nextFilter;
            loadPermisos(0);
        });
    }

    private void setupActions() {
        permisoFiltersHeader.setOnClickListener(view -> setFiltersExpanded(!filtersExpanded));
        btnBuscar.setOnClickListener(view -> loadPermisos(0));
        tilBuscar.setEndIconOnClickListener(view -> loadPermisos(0));
        edtBuscar.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadPermisos(0);
                return true;
            }
            return false;
        });
        btnAgregar.setOnClickListener(view ->
                formLauncher.launch(new Intent(this, PermisoFormActivity.class)));
        btnAnterior.setOnClickListener(view -> {
            if (currentPage > 0) {
                loadPermisos(currentPage - 1);
            }
        });
        btnSiguiente.setOnClickListener(view -> {
            if (currentPage + 1 < totalPages) {
                loadPermisos(currentPage + 1);
            }
        });
    }

    private void loadPermisos(int page) {
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
        listCall.enqueue(new Callback<PageResponse<PermisoResponse>>() {
            @Override
            public void onResponse(
                    Call<PageResponse<PermisoResponse>> call,
                    Response<PageResponse<PermisoResponse>> response) {
                setLoading(false);
                listCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    renderPage(response.body());
                    return;
                }
                showError(PermisoErrorMapper.fromResponse(PermisoListActivity.this, response));
            }

            @Override
            public void onFailure(Call<PageResponse<PermisoResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                listCall = null;
                showError(PermisoErrorMapper.fromFailure(PermisoListActivity.this, throwable));
            }
        });
    }

    private void renderPage(PageResponse<PermisoResponse> page) {
        currentPage = page.number;
        totalPages = page.totalPages;
        List<PermisoResponse> content = page.content;
        adapter.submitList(content);
        boolean empty = content == null || content.isEmpty();
        txtEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerPermisos.setVisibility(empty ? View.GONE : View.VISIBLE);
        updatePaginationControls();
    }

    private void showError(String message) {
        adapter.submitList(null);
        recyclerPermisos.setVisibility(View.GONE);
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
            permisoFilterControls.setVisibility(View.VISIBLE);
            permisoFilterControls.setAlpha(0f);
            permisoFilterControls.setTranslationY(-8f);
            permisoFilterControls.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
            return;
        }
        permisoFilterControls.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(140)
                .withEndAction(() -> permisoFilterControls.setVisibility(View.GONE))
                .start();
    }

    private void updatePaginationControls() {
        int visiblePage = totalPages == 0 ? 0 : currentPage + 1;
        txtPageInfo.setText(getString(R.string.permiso_pagina_info, visiblePage, totalPages));
        btnAnterior.setEnabled(!loading && currentPage > 0);
        btnSiguiente.setEnabled(!loading && currentPage + 1 < totalPages);
    }

    private Boolean resolveActiveFilter(int checkedId) {
        if (checkedId == R.id.btnFiltroActivosPermisos) {
            return Boolean.TRUE;
        }
        if (checkedId == R.id.btnFiltroInactivosPermisos) {
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
        for (Call<PermisoResponse> call : statusCalls.values()) {
            call.cancel();
        }
        statusCalls.clear();
    }
}
