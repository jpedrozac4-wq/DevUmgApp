package gt.com.ro.devumgapp.docente.ui;

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
import java.util.Map;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.carrera.dto.EstadoRequest;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.core.ui.ModuleNavigation;
import gt.com.ro.devumgapp.docente.dto.DocenteResponse;
import gt.com.ro.devumgapp.docente.network.DocenteApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocenteListActivity extends AppCompatActivity implements DocenteAdapter.Listener {

    private static final int PAGE_SIZE = 10;

    private TextInputLayout tilBuscar;
    private TextInputEditText edtBuscar;
    private MaterialButtonToggleGroup toggleEstado;
    private RecyclerView recyclerDocentes;
    private LinearProgressIndicator progressBar;
    private TextView txtEmptyState;
    private TextView txtErrorState;
    private TextView txtPageInfo;
    private View docenteHero;
    private View docenteFilters;
    private View docenteFiltersHeader;
    private View docenteFilterControls;
    private MaterialButton btnBuscar;
    private MaterialButton btnAgregar;
    private MaterialButton btnAnterior;
    private MaterialButton btnSiguiente;

    private DocenteApiService docenteApiService;
    private DocenteAdapter adapter;
    private ActivityResultLauncher<Intent> formLauncher;
    private Call<PageResponse<DocenteResponse>> listCall;
    private final Map<Long, Call<DocenteResponse>> itemCalls = new HashMap<>();
    private int currentPage;
    private int totalPages;
    private boolean loading;
    private Boolean activeFilter;
    private boolean firstResume = true;
    private boolean filtersExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "DOCENTES_LEER")) return;
        setContentView(R.layout.activity_docente_list);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        docenteApiService = RetrofitClient.getClient().create(DocenteApiService.class);
        formLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadDocentes(currentPage);
                    }
                });
        bindViews();
        setupToolbar();
        setupRecycler();
        setupFilters();
        setupActions();
        animateIntro();
        loadDocentes(0);
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
        loadDocentes(currentPage);
    }

    @Override
    public void onEdit(DocenteResponse docente) {
        Intent intent = new Intent(this, DocenteFormActivity.class);
        intent.putExtra(DocenteFormActivity.EXTRA_DOCENTE_ID, docente.id);
        formLauncher.launch(intent);
    }

    @Override
    public void onToggleStatus(DocenteResponse docente) {
        SgauDialog.confirmState(this, !docente.activo,
                "el docente \"" + docente.nombre + " " + docente.apellido + "\"",
                () -> enqueueItemCall(docente.id,
                        docenteApiService.cambiarEstado(docente.id, new EstadoRequest(!docente.activo)),
                        docente.activo ? R.string.docente_desactivado : R.string.docente_activado));
    }


    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarDocentes);
        toolbar.setTitle("");
        tilBuscar = findViewById(R.id.tilBuscarDocente);
        edtBuscar = findViewById(R.id.edtBuscarDocente);
        toggleEstado = findViewById(R.id.toggleEstadoDocente);
        recyclerDocentes = findViewById(R.id.recyclerDocentes);
        progressBar = findViewById(R.id.progressDocentes);
        txtEmptyState = findViewById(R.id.txtDocentesEmpty);
        txtErrorState = findViewById(R.id.txtDocentesError);
        txtPageInfo = findViewById(R.id.txtDocentesPageInfo);
        docenteHero = findViewById(R.id.docenteHero);
        docenteFilters = findViewById(R.id.docenteFilters);
        docenteFiltersHeader = findViewById(R.id.docenteFiltersHeader);
        docenteFilterControls = findViewById(R.id.docenteFilterControls);
        btnBuscar = findViewById(R.id.btnBuscarDocente);
        btnAgregar = findViewById(R.id.btnAgregarDocente);
        btnAgregar.setVisibility(Permissions.has("DOCENTES_CREAR") ? View.VISIBLE : View.GONE);
        btnAnterior = findViewById(R.id.btnDocentesAnterior);
        btnSiguiente = findViewById(R.id.btnDocentesSiguiente);
        ModuleNavigation.attach(this, toolbar);
    }

    private void setupToolbar() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void setupRecycler() {
        adapter = new DocenteAdapter(this);
        recyclerDocentes.setLayoutManager(new LinearLayoutManager(this));
        recyclerDocentes.setAdapter(adapter);
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
            loadDocentes(0);
        });
    }

    private void setupActions() {
        docenteFiltersHeader.setOnClickListener(view -> setFiltersExpanded(!filtersExpanded));
        btnBuscar.setOnClickListener(view -> loadDocentes(0));
        tilBuscar.setEndIconOnClickListener(view -> loadDocentes(0));
        edtBuscar.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadDocentes(0);
                return true;
            }
            return false;
        });
        btnAgregar.setOnClickListener(view -> formLauncher.launch(new Intent(this, DocenteFormActivity.class)));
        btnAnterior.setOnClickListener(view -> {
            if (currentPage > 0) {
                loadDocentes(currentPage - 1);
            }
        });
        btnSiguiente.setOnClickListener(view -> {
            if (currentPage + 1 < totalPages) {
                loadDocentes(currentPage + 1);
            }
        });
    }

    private void loadDocentes(int page) {
        if (loading) {
            return;
        }
        if (listCall != null) {
            listCall.cancel();
        }
        setLoading(true);
        txtErrorState.setVisibility(View.GONE);
        listCall = docenteApiService.listarDocentes(
                nullableText(getText(edtBuscar)),
                activeFilter,
                Math.max(page, 0),
                PAGE_SIZE);
        listCall.enqueue(new Callback<PageResponse<DocenteResponse>>() {
            @Override
            public void onResponse(
                    Call<PageResponse<DocenteResponse>> call,
                    Response<PageResponse<DocenteResponse>> response) {
                setLoading(false);
                listCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    renderPage(response.body());
                    return;
                }
                showSnackError(DocenteErrorMapper.fromResponse(DocenteListActivity.this, response));
            }

            @Override
            public void onFailure(Call<PageResponse<DocenteResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                listCall = null;
                showSnackError(DocenteErrorMapper.fromFailure(DocenteListActivity.this, throwable));
            }
        });
    }

    private void enqueueItemCall(long docenteId, Call<DocenteResponse> call, int successMessageRes) {
        if (itemCalls.containsKey(docenteId)) {
            return;
        }
        itemCalls.put(docenteId, call);
        adapter.setBusy(docenteId, true);
        call.enqueue(new Callback<DocenteResponse>() {
            @Override
            public void onResponse(Call<DocenteResponse> call, Response<DocenteResponse> response) {
                itemCalls.remove(docenteId);
                adapter.setBusy(docenteId, false);
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        adapter.replace(response.body());
                    } else {
                        loadDocentes(currentPage);
                    }
                    showSuccess(getString(successMessageRes));
                    return;
                }
                showListError(DocenteErrorMapper.fromResponse(DocenteListActivity.this, response));
            }

            @Override
            public void onFailure(Call<DocenteResponse> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    itemCalls.remove(docenteId);
                    adapter.setBusy(docenteId, false);
                    showListError(DocenteErrorMapper.fromFailure(DocenteListActivity.this, throwable));
                }
            }
        });
    }

    private void renderPage(PageResponse<DocenteResponse> page) {
        currentPage = page.number;
        totalPages = page.totalPages;
        adapter.submitList(page.content);
        boolean empty = page.content == null || page.content.isEmpty();
        txtEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerDocentes.setVisibility(empty ? View.GONE : View.VISIBLE);
        updatePaginationControls();
    }

    private void showListError(String message) {
        adapter.submitList(null);
        recyclerDocentes.setVisibility(View.GONE);
        txtEmptyState.setVisibility(View.GONE);
        txtErrorState.setText(message);
        txtErrorState.setVisibility(View.VISIBLE);
        currentPage = 0;
        totalPages = 0;
        updatePaginationControls();
        showSnackError(message);
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
            docenteFilterControls.setVisibility(View.VISIBLE);
            docenteFilterControls.setAlpha(0f);
            docenteFilterControls.setTranslationY(-8f);
            docenteFilterControls.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
            return;
        }
        docenteFilterControls.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(140)
                .withEndAction(() -> docenteFilterControls.setVisibility(View.GONE))
                .start();
    }

    private void updatePaginationControls() {
        int visiblePage = totalPages == 0 ? 0 : currentPage + 1;
        txtPageInfo.setText(getString(R.string.docente_pagina_info, visiblePage, totalPages));
        btnAnterior.setEnabled(!loading && currentPage > 0);
        btnSiguiente.setEnabled(!loading && currentPage + 1 < totalPages);
    }

    private void animateIntro() {
        docenteHero.setAlpha(0f);
        docenteHero.setTranslationY(20f);
        docenteHero.animate().alpha(1f).translationY(0f).setDuration(360).start();
        docenteFilters.setAlpha(0f);
        docenteFilters.setTranslationY(16f);
        docenteFilters.animate().alpha(1f).translationY(0f).setStartDelay(110).setDuration(320).start();
    }

    private Boolean resolveActiveFilter(int checkedId) {
        if (checkedId == R.id.btnFiltroDocentesActivos) {
            return Boolean.TRUE;
        }
        if (checkedId == R.id.btnFiltroDocentesInactivos) {
            return Boolean.FALSE;
        }
        return null;
    }

    private String nullableText(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private void showSuccess(String message) {
        UiNotifier.success(this, message);
    }

    private void showSnackError(String message) {
        UiNotifier.error(this, message);
    }

    private void cancelCalls() {
        if (listCall != null) {
            listCall.cancel();
        }
        for (Call<DocenteResponse> call : itemCalls.values()) {
            call.cancel();
        }
        itemCalls.clear();
    }
}
