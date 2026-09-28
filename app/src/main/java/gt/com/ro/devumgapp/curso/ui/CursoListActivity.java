package gt.com.ro.devumgapp.curso.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.carrera.dto.CarreraResumenResponse;
import gt.com.ro.devumgapp.carrera.dto.EstadoRequest;
import gt.com.ro.devumgapp.carrera.network.CarreraApiService;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.curso.dto.CursoResponse;
import gt.com.ro.devumgapp.curso.dto.DocenteRequest;
import gt.com.ro.devumgapp.curso.dto.DocenteResumenResponse;
import gt.com.ro.devumgapp.curso.network.CursoApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CursoListActivity extends AppCompatActivity implements CursoAdapter.Listener {

    private static final int PAGE_SIZE = 10;

    private TextInputLayout tilBuscar;
    private TextInputEditText edtBuscar;
    private Spinner spinnerCarrera;
    private Spinner spinnerDocente;
    private Spinner spinnerCiclo;
    private MaterialButtonToggleGroup toggleEstado;
    private RecyclerView recyclerCursos;
    private LinearProgressIndicator progressBar;
    private TextView txtEmptyState;
    private TextView txtErrorState;
    private TextView txtPageInfo;
    private View cursoHero;
    private View cursoFilters;
    private View cursoFiltersHeader;
    private View cursoFilterControls;
    private MaterialButton btnBuscar;
    private MaterialButton btnAgregar;
    private MaterialButton btnAnterior;
    private MaterialButton btnSiguiente;

    private CursoApiService cursoApiService;
    private CarreraApiService carreraApiService;
    private CursoAdapter adapter;
    private ActivityResultLauncher<Intent> formLauncher;
    private Call<PageResponse<CursoResponse>> listCall;
    private Call<List<CarreraResumenResponse>> carrerasCall;
    private Call<List<DocenteResumenResponse>> docentesCall;
    private final Map<Long, Call<CursoResponse>> itemCalls = new HashMap<>();
    private final List<NamedItem<CarreraResumenResponse>> carreraItems = new ArrayList<>();
    private final List<NamedItem<DocenteResumenResponse>> docenteItems = new ArrayList<>();
    private final List<NamedItem<Integer>> cicloItems = new ArrayList<>();
    private int currentPage;
    private int totalPages;
    private boolean loading;
    private Boolean activeFilter;
    private boolean firstResume = true;
    private boolean filtersExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "CURSOS_LEER")) return;
        setContentView(R.layout.activity_curso_list);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        cursoApiService = RetrofitClient.getClient().create(CursoApiService.class);
        carreraApiService = RetrofitClient.getClient().create(CarreraApiService.class);
        formLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadCursos(currentPage);
                    }
                });
        bindViews();
        setupToolbar();
        setupRecycler();
        setupFilters();
        setupActions();
        animateIntro();
        loadFilterData();
        loadCursos(0);
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
        loadFilterData();
        loadCursos(currentPage);
    }

    @Override
    public void onEdit(CursoResponse curso) {
        Intent intent = new Intent(this, CursoFormActivity.class);
        intent.putExtra(CursoFormActivity.EXTRA_CURSO_ID, curso.id);
        formLauncher.launch(intent);
    }

    @Override
    public void onToggleStatus(CursoResponse curso) {
        enqueueItemCall(curso.id, cursoApiService.cambiarEstado(curso.id, new EstadoRequest(!curso.activo)));
    }

    @Override
    public void onAssignTeacher(CursoResponse curso) {
        if (docenteItems.size() <= 1) {
            UiNotifier.info(this, R.string.curso_docentes_no_disponibles);
            return;
        }
        List<NamedItem<DocenteResumenResponse>> assignable = docenteItems.subList(1, docenteItems.size());
        String[] labels = new String[assignable.size()];
        for (int index = 0; index < assignable.size(); index++) {
            labels[index] = assignable.get(index).label;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.curso_asignar_docente)
                .setItems(labels, (dialog, which) -> enqueueItemCall(
                        curso.id,
                        cursoApiService.asignarDocente(
                                curso.id,
                                new DocenteRequest(assignable.get(which).value.id))))
                .show();
    }

    @Override
    public void onRemoveTeacher(CursoResponse curso) {
        enqueueItemCall(curso.id, cursoApiService.quitarDocente(curso.id));
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarCursos);
        toolbar.setTitle("");
        tilBuscar = findViewById(R.id.tilBuscarCurso);
        edtBuscar = findViewById(R.id.edtBuscarCurso);
        spinnerCarrera = findViewById(R.id.spinnerFiltroCarrera);
        spinnerDocente = findViewById(R.id.spinnerFiltroDocente);
        spinnerCiclo = findViewById(R.id.spinnerFiltroCiclo);
        toggleEstado = findViewById(R.id.toggleEstadoCurso);
        recyclerCursos = findViewById(R.id.recyclerCursos);
        progressBar = findViewById(R.id.progressCursos);
        txtEmptyState = findViewById(R.id.txtCursosEmpty);
        txtErrorState = findViewById(R.id.txtCursosError);
        txtPageInfo = findViewById(R.id.txtCursosPageInfo);
        cursoHero = findViewById(R.id.cursoHero);
        cursoFilters = findViewById(R.id.cursoFilters);
        cursoFiltersHeader = findViewById(R.id.cursoFiltersHeader);
        cursoFilterControls = findViewById(R.id.cursoFilterControls);
        btnBuscar = findViewById(R.id.btnBuscarCurso);
        btnAgregar = findViewById(R.id.btnAgregarCurso);
        btnAgregar.setVisibility(Permissions.has("CURSOS_CREAR") ? View.VISIBLE : View.GONE);
        btnAnterior = findViewById(R.id.btnCursosAnterior);
        btnSiguiente = findViewById(R.id.btnCursosSiguiente);
        toolbar.setNavigationOnClickListener(view -> finish());
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
        adapter = new CursoAdapter(this);
        recyclerCursos.setLayoutManager(new LinearLayoutManager(this));
        recyclerCursos.setAdapter(adapter);
    }

    private void setupFilters() {
        carreraItems.add(new NamedItem<>(getString(R.string.curso_filtro_todas_carreras), null));
        docenteItems.add(new NamedItem<>(getString(R.string.curso_filtro_todos_docentes), null));
        int year = Calendar.getInstance().get(Calendar.YEAR);
        cicloItems.add(new NamedItem<>(getString(R.string.curso_filtro_todos_ciclos), null));
        for (int itemYear = Math.max(2020, year - 3); itemYear <= year + 3; itemYear++) {
            cicloItems.add(new NamedItem<>(String.valueOf(itemYear), itemYear));
        }
        bindSpinner(spinnerCarrera, carreraItems);
        bindSpinner(spinnerDocente, docenteItems);
        bindSpinner(spinnerCiclo, cicloItems);
        toggleEstado.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            Boolean nextFilter = resolveActiveFilter(checkedId);
            if (activeFilter == nextFilter || (activeFilter != null && activeFilter.equals(nextFilter))) {
                return;
            }
            activeFilter = nextFilter;
            loadCursos(0);
        });
    }

    private void setupActions() {
        cursoFiltersHeader.setOnClickListener(view -> setFiltersExpanded(!filtersExpanded));
        btnBuscar.setOnClickListener(view -> loadCursos(0));
        tilBuscar.setEndIconOnClickListener(view -> loadCursos(0));
        edtBuscar.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadCursos(0);
                return true;
            }
            return false;
        });
        btnAgregar.setOnClickListener(view -> formLauncher.launch(new Intent(this, CursoFormActivity.class)));
        btnAnterior.setOnClickListener(view -> {
            if (currentPage > 0) {
                loadCursos(currentPage - 1);
            }
        });
        btnSiguiente.setOnClickListener(view -> {
            if (currentPage + 1 < totalPages) {
                loadCursos(currentPage + 1);
            }
        });
    }

    private void loadFilterData() {
        if (carrerasCall != null) {
            carrerasCall.cancel();
        }
        carrerasCall = carreraApiService.listarCarrerasActivas();
        carrerasCall.enqueue(new Callback<List<CarreraResumenResponse>>() {
            @Override
            public void onResponse(
                    Call<List<CarreraResumenResponse>> call,
                    Response<List<CarreraResumenResponse>> response) {
                carrerasCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    carreraItems.clear();
                    carreraItems.add(new NamedItem<>(getString(R.string.curso_filtro_todas_carreras), null));
                    for (CarreraResumenResponse carrera : response.body()) {
                        carreraItems.add(new NamedItem<>(formatCarrera(carrera), carrera));
                    }
                    bindSpinner(spinnerCarrera, carreraItems);
                    return;
                }
                showToast(CursoErrorMapper.fromResponse(CursoListActivity.this, response));
            }

            @Override
            public void onFailure(Call<List<CarreraResumenResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    carrerasCall = null;
                    showToast(CursoErrorMapper.fromFailure(CursoListActivity.this, throwable));
                }
            }
        });

        if (docentesCall != null) {
            docentesCall.cancel();
        }
        docentesCall = cursoApiService.listarDocentesActivos();
        docentesCall.enqueue(new Callback<List<DocenteResumenResponse>>() {
            @Override
            public void onResponse(
                    Call<List<DocenteResumenResponse>> call,
                    Response<List<DocenteResumenResponse>> response) {
                docentesCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    docenteItems.clear();
                    docenteItems.add(new NamedItem<>(getString(R.string.curso_filtro_todos_docentes), null));
                    for (DocenteResumenResponse docente : response.body()) {
                        docenteItems.add(new NamedItem<>(formatDocente(docente), docente));
                    }
                    bindSpinner(spinnerDocente, docenteItems);
                    return;
                }
                showToast(CursoErrorMapper.fromResponse(CursoListActivity.this, response));
            }

            @Override
            public void onFailure(Call<List<DocenteResumenResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    docentesCall = null;
                    showToast(CursoErrorMapper.fromFailure(CursoListActivity.this, throwable));
                }
            }
        });
    }

    private void loadCursos(int page) {
        if (loading) {
            return;
        }
        if (listCall != null) {
            listCall.cancel();
        }
        setLoading(true);
        txtErrorState.setVisibility(View.GONE);
        listCall = cursoApiService.listarCursos(
                nullableText(getText(edtBuscar)),
                selectedCarreraId(),
                selectedDocenteId(),
                selectedCiclo(),
                activeFilter,
                Math.max(page, 0),
                PAGE_SIZE);
        listCall.enqueue(new Callback<PageResponse<CursoResponse>>() {
            @Override
            public void onResponse(
                    Call<PageResponse<CursoResponse>> call,
                    Response<PageResponse<CursoResponse>> response) {
                setLoading(false);
                listCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    renderPage(response.body());
                    return;
                }
                showSnackError(CursoErrorMapper.fromResponse(CursoListActivity.this, response));
            }

            @Override
            public void onFailure(Call<PageResponse<CursoResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                listCall = null;
                showSnackError(CursoErrorMapper.fromFailure(CursoListActivity.this, throwable));
            }
        });
    }

    private void enqueueItemCall(long cursoId, Call<CursoResponse> call) {
        if (itemCalls.containsKey(cursoId)) {
            return;
        }
        itemCalls.put(cursoId, call);
        adapter.setBusy(cursoId, true);
        call.enqueue(new Callback<CursoResponse>() {
            @Override
            public void onResponse(Call<CursoResponse> call, Response<CursoResponse> response) {
                itemCalls.remove(cursoId);
                adapter.setBusy(cursoId, false);
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        adapter.replace(response.body());
                    } else {
                        loadCursos(currentPage);
                    }
                    showSuccess(getString(R.string.curso_accion_completada));
                    return;
                }
                showListError(CursoErrorMapper.fromResponse(CursoListActivity.this, response));
            }

            @Override
            public void onFailure(Call<CursoResponse> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    itemCalls.remove(cursoId);
                    adapter.setBusy(cursoId, false);
                showListError(CursoErrorMapper.fromFailure(CursoListActivity.this, throwable));
                }
            }
        });
    }

    private void renderPage(PageResponse<CursoResponse> page) {
        currentPage = page.number;
        totalPages = page.totalPages;
        adapter.submitList(page.content);
        boolean empty = page.content == null || page.content.isEmpty();
        txtEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerCursos.setVisibility(empty ? View.GONE : View.VISIBLE);
        updatePaginationControls();
    }

    private void showListError(String message) {
        adapter.submitList(null);
        recyclerCursos.setVisibility(View.GONE);
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
        spinnerCarrera.setEnabled(!loading);
        spinnerDocente.setEnabled(!loading);
        spinnerCiclo.setEnabled(!loading);
        edtBuscar.setEnabled(!loading);
    }

    private void setFiltersExpanded(boolean expanded) {
        filtersExpanded = expanded;
        if (expanded) {
            cursoFilterControls.setVisibility(View.VISIBLE);
            cursoFilterControls.setAlpha(0f);
            cursoFilterControls.setTranslationY(-8f);
            cursoFilterControls.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
            return;
        }
        cursoFilterControls.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(140)
                .withEndAction(() -> cursoFilterControls.setVisibility(View.GONE))
                .start();
    }

    private void updatePaginationControls() {
        int visiblePage = totalPages == 0 ? 0 : currentPage + 1;
        txtPageInfo.setText(getString(R.string.curso_pagina_info, visiblePage, totalPages));
        btnAnterior.setEnabled(!loading && currentPage > 0);
        btnSiguiente.setEnabled(!loading && currentPage + 1 < totalPages);
    }

    private void animateIntro() {
        cursoHero.setAlpha(0f);
        cursoHero.setTranslationY(20f);
        cursoHero.animate().alpha(1f).translationY(0f).setDuration(360).start();
        cursoFilters.setAlpha(0f);
        cursoFilters.setTranslationY(16f);
        cursoFilters.animate().alpha(1f).translationY(0f).setStartDelay(110).setDuration(320).start();
    }

    private <T> void bindSpinner(Spinner spinner, List<NamedItem<T>> items) {
        ArrayAdapter<NamedItem<T>> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private String formatCarrera(CarreraResumenResponse carrera) {
        return nullToEmpty(carrera.codigo) + " - " + nullToEmpty(carrera.nombre);
    }

    private String formatDocente(DocenteResumenResponse docente) {
        return nullToEmpty(docente.codigoDocente) + " - "
                + nullToEmpty(docente.nombre) + " " + nullToEmpty(docente.apellido);
    }

    private Long selectedCarreraId() {
        NamedItem<CarreraResumenResponse> item = selectedItem(spinnerCarrera);
        return item == null || item.value == null ? null : item.value.id;
    }

    private Long selectedDocenteId() {
        NamedItem<DocenteResumenResponse> item = selectedItem(spinnerDocente);
        return item == null || item.value == null ? null : item.value.id;
    }

    private Integer selectedCiclo() {
        NamedItem<Integer> item = selectedItem(spinnerCiclo);
        return item == null ? null : item.value;
    }

    @SuppressWarnings("unchecked")
    private <T> NamedItem<T> selectedItem(Spinner spinner) {
        return (NamedItem<T>) spinner.getSelectedItem();
    }

    private Boolean resolveActiveFilter(int checkedId) {
        if (checkedId == R.id.btnFiltroCursosActivos) {
            return Boolean.TRUE;
        }
        if (checkedId == R.id.btnFiltroCursosInactivos) {
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void showToast(String message) {
        showSnackError(message);
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
        if (carrerasCall != null) {
            carrerasCall.cancel();
        }
        if (docentesCall != null) {
            docentesCall.cancel();
        }
        for (Call<CursoResponse> call : itemCalls.values()) {
            call.cancel();
        }
        itemCalls.clear();
    }
}
