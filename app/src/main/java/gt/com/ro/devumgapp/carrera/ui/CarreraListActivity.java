package gt.com.ro.devumgapp.carrera.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
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
import gt.com.ro.devumgapp.carrera.dto.CarreraResponse;
import gt.com.ro.devumgapp.carrera.dto.EstadoRequest;
import gt.com.ro.devumgapp.carrera.network.CarreraApiService;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CarreraListActivity extends AppCompatActivity implements CarreraAdapter.Listener {

    private static final int PAGE_SIZE = 10;

    private MaterialToolbar toolbar;
    private TextInputEditText edtBuscar;
    private TextInputLayout tilBuscar;
    private MaterialButtonToggleGroup toggleEstado;
    private RecyclerView recyclerCarreras;
    private LinearProgressIndicator progressBar;
    private TextView txtEmptyState;
    private TextView txtErrorState;
    private TextView txtPageInfo;
    private View carreraHero;
    private View carreraFilters;
    private View carreraFiltersHeader;
    private View carreraFilterControls;
    private MaterialButton btnBuscar;
    private MaterialButton btnAgregar;
    private MaterialButton btnAnterior;
    private MaterialButton btnSiguiente;

    private CarreraApiService apiService;
    private CarreraAdapter adapter;
    private Call<PageResponse<CarreraResponse>> listCall;
    private final Map<Long, Call<CarreraResponse>> statusCalls = new HashMap<>();
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
        setContentView(R.layout.activity_carrera_list);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        apiService = RetrofitClient.getClient().create(CarreraApiService.class);
        formLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadCarreras(currentPage);
                    }
                });
        bindViews();
        setupToolbar();
        setupRecycler();
        setupFilters();
        setupActions();
        animateIntro();
        loadCarreras(0);
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
        loadCarreras(currentPage);
    }

    @Override
    public void onEdit(CarreraResponse carrera) {
        Intent intent = new Intent(this, CarreraFormActivity.class);
        intent.putExtra(CarreraFormActivity.EXTRA_CARRERA_ID, carrera.id);
        formLauncher.launch(intent);
    }

    @Override
    public void onToggleStatus(CarreraResponse carrera) {
        if (statusCalls.containsKey(carrera.id)) {
            return;
        }
        adapter.setStatusChanging(carrera.id, true);
        Call<CarreraResponse> call = apiService.cambiarEstado(carrera.id, new EstadoRequest(!carrera.activo));
        statusCalls.put(carrera.id, call);
        call.enqueue(new Callback<CarreraResponse>() {
            @Override
            public void onResponse(Call<CarreraResponse> call, Response<CarreraResponse> response) {
                adapter.setStatusChanging(carrera.id, false);
                statusCalls.remove(carrera.id);
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        adapter.replace(response.body());
                    } else {
                        loadCarreras(currentPage);
                    }
                    UiNotifier.success(CarreraListActivity.this, R.string.carrera_estado_actualizado);
                    return;
                }
                UiNotifier.error(
                        CarreraListActivity.this,
                        CarreraErrorMapper.fromResponse(CarreraListActivity.this, response));
            }

            @Override
            public void onFailure(Call<CarreraResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                adapter.setStatusChanging(carrera.id, false);
                statusCalls.remove(carrera.id);
                UiNotifier.error(
                        CarreraListActivity.this,
                        CarreraErrorMapper.fromFailure(CarreraListActivity.this, throwable));
            }
        });
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarCarreras);
        tilBuscar = findViewById(R.id.tilBuscarCarrera);
        edtBuscar = findViewById(R.id.edtBuscarCarrera);
        toggleEstado = findViewById(R.id.toggleEstadoCarrera);
        recyclerCarreras = findViewById(R.id.recyclerCarreras);
        progressBar = findViewById(R.id.progressCarreras);
        txtEmptyState = findViewById(R.id.txtCarrerasEmpty);
        txtErrorState = findViewById(R.id.txtCarrerasError);
        txtPageInfo = findViewById(R.id.txtCarrerasPageInfo);
        carreraHero = findViewById(R.id.carreraHero);
        carreraFilters = findViewById(R.id.carreraFilters);
        carreraFiltersHeader = findViewById(R.id.carreraFiltersHeader);
        carreraFilterControls = findViewById(R.id.carreraFilterControls);
        btnBuscar = findViewById(R.id.btnBuscarCarrera);
        btnAgregar = findViewById(R.id.btnAgregarCarrera);
        btnAnterior = findViewById(R.id.btnCarrerasAnterior);
        btnSiguiente = findViewById(R.id.btnCarrerasSiguiente);
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
        carreraHero.setAlpha(0f);
        carreraHero.setTranslationY(20f);
        carreraHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        carreraFilters.setAlpha(0f);
        carreraFilters.setTranslationY(16f);
        carreraFilters.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(110)
                .setDuration(300)
                .start();
    }

    private void setupRecycler() {
        adapter = new CarreraAdapter(this);
        recyclerCarreras.setLayoutManager(new LinearLayoutManager(this));
        recyclerCarreras.setAdapter(adapter);
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
            loadCarreras(0);
        });
    }

    private void setupActions() {
        carreraFiltersHeader.setOnClickListener(view -> setFiltersExpanded(!filtersExpanded));
        btnBuscar.setOnClickListener(view -> loadCarreras(0));
        tilBuscar.setEndIconOnClickListener(view -> loadCarreras(0));
        edtBuscar.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadCarreras(0);
                return true;
            }
            return false;
        });
        btnAgregar.setOnClickListener(view ->
                formLauncher.launch(new Intent(this, CarreraFormActivity.class)));
        btnAnterior.setOnClickListener(view -> {
            if (currentPage > 0) {
                loadCarreras(currentPage - 1);
            }
        });
        btnSiguiente.setOnClickListener(view -> {
            if (currentPage + 1 < totalPages) {
                loadCarreras(currentPage + 1);
            }
        });
    }

    private void loadCarreras(int page) {
        if (loading) {
            return;
        }
        if (listCall != null) {
            listCall.cancel();
        }
        setLoading(true);
        txtErrorState.setVisibility(View.GONE);

        String searchText = getText(edtBuscar).trim();
        listCall = apiService.listarCarreras(
                searchText.isEmpty() ? null : searchText,
                activeFilter,
                Math.max(page, 0),
                PAGE_SIZE);
        listCall.enqueue(new Callback<PageResponse<CarreraResponse>>() {
            @Override
            public void onResponse(
                    Call<PageResponse<CarreraResponse>> call,
                    Response<PageResponse<CarreraResponse>> response) {
                setLoading(false);
                listCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    renderPage(response.body());
                    return;
                }
                showError(CarreraErrorMapper.fromResponse(CarreraListActivity.this, response));
            }

            @Override
            public void onFailure(Call<PageResponse<CarreraResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                listCall = null;
                showError(CarreraErrorMapper.fromFailure(CarreraListActivity.this, throwable));
            }
        });
    }

    private void renderPage(PageResponse<CarreraResponse> page) {
        currentPage = page.number;
        totalPages = page.totalPages;
        adapter.submitList(page.content);
        boolean empty = page.content == null || page.content.isEmpty();
        txtEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerCarreras.setVisibility(empty ? View.GONE : View.VISIBLE);
        updatePaginationControls();
    }

    private void showError(String message) {
        adapter.submitList(null);
        recyclerCarreras.setVisibility(View.GONE);
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
            carreraFilterControls.setVisibility(View.VISIBLE);
            carreraFilterControls.setAlpha(0f);
            carreraFilterControls.setTranslationY(-8f);
            carreraFilterControls.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
            return;
        }
        carreraFilterControls.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(140)
                .withEndAction(() -> carreraFilterControls.setVisibility(View.GONE))
                .start();
    }

    private void updatePaginationControls() {
        int visiblePage = totalPages == 0 ? 0 : currentPage + 1;
        txtPageInfo.setText(getString(R.string.carrera_pagina_info, visiblePage, totalPages));
        btnAnterior.setEnabled(!loading && currentPage > 0);
        btnSiguiente.setEnabled(!loading && currentPage + 1 < totalPages);
    }

    private Boolean resolveActiveFilter(int checkedId) {
        if (checkedId == R.id.btnFiltroActivas) {
            return Boolean.TRUE;
        }
        if (checkedId == R.id.btnFiltroInactivas) {
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
        for (Call<CarreraResponse> call : statusCalls.values()) {
            call.cancel();
        }
        statusCalls.clear();
    }
}
