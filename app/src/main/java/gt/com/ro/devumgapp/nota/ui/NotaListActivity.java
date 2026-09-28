package gt.com.ro.devumgapp.nota.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.curso.dto.CursoResponse;
import gt.com.ro.devumgapp.curso.network.CursoApiService;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import gt.com.ro.devumgapp.nota.dto.EstadoRequest;
import gt.com.ro.devumgapp.nota.dto.NotaResponse;
import gt.com.ro.devumgapp.nota.network.NotaApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotaListActivity extends AppCompatActivity implements NotaAdapter.Listener {

    private static final int PAGE_SIZE = 10;

    private MaterialToolbar toolbar;
    private MaterialAutoCompleteTextView actEstudiante;
    private MaterialAutoCompleteTextView actCurso;
    private Spinner spinnerCiclo;
    private EstudianteResumenResponse selectedEstudiante;
    private CursoResponse selectedCurso;
    private TextInputEditText edtTipo;
    private MaterialButtonToggleGroup toggleEstado;
    private RecyclerView recyclerNotas;
    private LinearProgressIndicator progressBar;
    private LinearProgressIndicator progressCatalogs;
    private TextView txtEmptyState;
    private TextView txtErrorState;
    private TextView txtPageInfo;
    private View notaHero;
    private View notaFilters;
    private View notaFiltersHeader;
    private View notaFilterControls;
    private MaterialButton btnBuscar;
    private MaterialButton btnAgregar;
    private MaterialButton btnPromedio;
    private MaterialButton btnAnterior;
    private MaterialButton btnSiguiente;

    private NotaApiService apiService;
    private EstudianteApiService estudianteApiService;
    private CursoApiService cursoApiService;
    private NotaAdapter adapter;
    private Call<PageResponse<NotaResponse>> listCall;
    private Call<List<EstudianteResumenResponse>> estudiantesCall;
    private Call<List<CursoResponse>> cursosCall;
    private final Map<Long, Call<NotaResponse>> statusCalls = new HashMap<>();
    private final List<NamedItem<EstudianteResumenResponse>> estudianteItems = new ArrayList<>();
    private final List<NamedItem<CursoResponse>> cursoItems = new ArrayList<>();
    private final List<NamedItem<Integer>> cicloItems = new ArrayList<>();
    private final Map<MaterialAutoCompleteTextView, TextWatcher> autoCompleteWatchers = new HashMap<>();
    private final Map<MaterialAutoCompleteTextView, String[]> autoCompletePickedLabels = new HashMap<>();
    private final Map<Long, String> estudianteNombres = new HashMap<>();
    private final Map<Long, String> cursoNombres = new HashMap<>();
    private ActivityResultLauncher<Intent> formLauncher;
    private ActivityResultLauncher<Intent> promedioLauncher;

    private int currentPage;
    private int totalPages;
    private int pendingPage;
    private boolean loading;
    private Boolean activoFilter;
    private boolean firstResume = true;
    private boolean filtersExpanded;
    private boolean estudiantesLoaded;
    private boolean cursosLoaded;
    private boolean catalogsReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "NOTAS_LEER")) return;
        setContentView(R.layout.activity_nota_list);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        apiService = RetrofitClient.getClient().create(NotaApiService.class);
        estudianteApiService = RetrofitClient.getClient().create(EstudianteApiService.class);
        cursoApiService = RetrofitClient.getClient().create(CursoApiService.class);
        formLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadNotas(currentPage);
                    }
                });
        promedioLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadNotas(currentPage);
                    }
                });
        bindViews();
        setupToolbar();
        setupRecycler();
        setupFilters();
        setupActions();
        initializeSpinners();
        animateIntro();
        loadCatalogs();
        loadNotas(0);
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
        loadNotas(currentPage);
    }

    @Override
    public void onEdit(NotaResponse nota) {
        Intent intent = new Intent(this, NotaFormActivity.class);
        intent.putExtra(NotaFormActivity.EXTRA_NOTA_ID, nota.id);
        formLauncher.launch(intent);
    }

    @Override
    public void onOpenPromedio(NotaResponse nota) {
        Intent intent = new Intent(this, PromedioNotasActivity.class);
        intent.putExtra(PromedioNotasActivity.EXTRA_ESTUDIANTE_ID, nota.estudianteId);
        promedioLauncher.launch(intent);
    }

    @Override
    public void onToggleActivo(NotaResponse nota) {
        if (statusCalls.containsKey(nota.id)) {
            return;
        }
        adapter.setStatusChanging(nota.id, true);
        Call<NotaResponse> call = apiService.cambiarEstado(
                nota.id, new EstadoRequest(!nota.activo));
        statusCalls.put(nota.id, call);
        call.enqueue(new Callback<NotaResponse>() {
            @Override
            public void onResponse(Call<NotaResponse> call, Response<NotaResponse> response) {
                statusCalls.remove(nota.id);
                adapter.setStatusChanging(nota.id, false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.replace(response.body());
                    UiNotifier.success(NotaListActivity.this, R.string.nota_estado_actualizada);
                    return;
                }
                UiNotifier.error(
                        NotaListActivity.this,
                        NotaErrorMapper.fromResponse(NotaListActivity.this, response));
            }

            @Override
            public void onFailure(Call<NotaResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                statusCalls.remove(nota.id);
                adapter.setStatusChanging(nota.id, false);
                UiNotifier.error(
                        NotaListActivity.this,
                        NotaErrorMapper.fromFailure(NotaListActivity.this, throwable));
            }
        });
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarNotas);
        actEstudiante = findViewById(R.id.actNotaEstudiante);
        actCurso = findViewById(R.id.actNotaCurso);
        spinnerCiclo = findViewById(R.id.spnNotaCiclo);
        edtTipo = findViewById(R.id.edtNotaTipo);
        toggleEstado = findViewById(R.id.toggleEstadoNota);
        recyclerNotas = findViewById(R.id.recyclerNotas);
        progressBar = findViewById(R.id.progressNotas);
        progressCatalogs = findViewById(R.id.progressNotaCatalogos);
        txtEmptyState = findViewById(R.id.txtNotasEmpty);
        txtErrorState = findViewById(R.id.txtNotasError);
        txtPageInfo = findViewById(R.id.txtNotasPageInfo);
        notaHero = findViewById(R.id.notaHero);
        notaFilters = findViewById(R.id.notaFilters);
        notaFiltersHeader = findViewById(R.id.notaFiltersHeader);
        notaFilterControls = findViewById(R.id.notaFilterControls);
        btnBuscar = findViewById(R.id.btnBuscarNota);
        btnAgregar = findViewById(R.id.btnAgregarNota);
        btnAgregar.setVisibility(Permissions.has("NOTAS_CREAR") ? View.VISIBLE : View.GONE);
        btnPromedio = findViewById(R.id.btnPromedioNotas);
        btnAnterior = findViewById(R.id.btnNotasAnterior);
        btnSiguiente = findViewById(R.id.btnNotasSiguiente);
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
        notaHero.setAlpha(0f);
        notaHero.setTranslationY(20f);
        notaHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        notaFilters.setAlpha(0f);
        notaFilters.setTranslationY(16f);
        notaFilters.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(110)
                .setDuration(300)
                .start();
    }

    private void setupRecycler() {
        adapter = new NotaAdapter(this);
        recyclerNotas.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotas.setAdapter(adapter);
    }

    private void setupFilters() {
        toggleEstado.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            Boolean nextFilter = resolveActivoFilter(checkedId);
            if (Objects.equals(activoFilter, nextFilter)) {
                return;
            }
            activoFilter = nextFilter;
            loadNotas(0);
        });
    }

    private void setupActions() {
        notaFiltersHeader.setOnClickListener(view -> setFiltersExpanded(!filtersExpanded));
        btnBuscar.setOnClickListener(view -> loadNotas(0));
        edtTipo.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadNotas(0);
                return true;
            }
            return false;
        });
        btnAgregar.setOnClickListener(view ->
                formLauncher.launch(new Intent(this, NotaFormActivity.class)));
        btnPromedio.setOnClickListener(view ->
                promedioLauncher.launch(new Intent(this, PromedioNotasActivity.class)));
        btnAnterior.setOnClickListener(view -> {
            if (currentPage > 0) {
                loadNotas(currentPage - 1);
            }
        });
        btnSiguiente.setOnClickListener(view -> {
            if (currentPage + 1 < totalPages) {
                loadNotas(currentPage + 1);
            }
        });
    }

    private void initializeSpinners() {
        estudianteItems.add(new NamedItem<>(getString(R.string.nota_filtros_todos_estudiantes), null));
        cursoItems.add(new NamedItem<>(getString(R.string.nota_filtros_todos_cursos), null));
        int year = Calendar.getInstance().get(Calendar.YEAR);
        cicloItems.add(new NamedItem<>(getString(R.string.nota_filtros_todos_ciclos), null));
        for (int itemYear = Math.max(2020, year - 3); itemYear <= year + 3; itemYear++) {
            cicloItems.add(new NamedItem<>(String.valueOf(itemYear), itemYear));
        }
        bindAutoComplete(actEstudiante, estudianteItems,
                item -> selectedEstudiante = item == null ? null : item.value);
        bindAutoComplete(actCurso, cursoItems,
                item -> selectedCurso = item == null ? null : item.value);
        bindSpinner(spinnerCiclo, cicloItems);
    }

    private void loadCatalogs() {
        setCatalogsLoading(true);
        estudiantesCall = estudianteApiService.listarEstudiantesActivos();
        estudiantesCall.enqueue(new Callback<List<EstudianteResumenResponse>>() {
            @Override
            public void onResponse(
                    Call<List<EstudianteResumenResponse>> call,
                    Response<List<EstudianteResumenResponse>> response) {
                estudiantesCall = null;
                estudiantesLoaded = true;
                if (response.isSuccessful() && response.body() != null) {
                    estudianteItems.clear();
                    estudianteItems.add(new NamedItem<>(
                            getString(R.string.nota_filtros_todos_estudiantes), null));
                    estudianteNombres.clear();
                    for (EstudianteResumenResponse estudiante : response.body()) {
                        String nombre = formatEstudiante(estudiante);
                        estudianteItems.add(new NamedItem<>(nombre, estudiante));
                        estudianteNombres.put(estudiante.id, nombre);
                    }
                    adapter.setNombres(estudianteNombres, cursoNombres);
                    bindAutoComplete(actEstudiante, estudianteItems,
                            item -> selectedEstudiante = item == null ? null : item.value);
                } else {
                    UiNotifier.error(
                            NotaListActivity.this,
                            NotaErrorMapper.fromResponse(NotaListActivity.this, response));
                }
                markCatalogLoaded();
            }

            @Override
            public void onFailure(Call<List<EstudianteResumenResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    estudiantesCall = null;
                    estudiantesLoaded = true;
                    UiNotifier.error(
                            NotaListActivity.this,
                            NotaErrorMapper.fromFailure(NotaListActivity.this, throwable));
                    markCatalogLoaded();
                }
            }
        });

        cursosCall = cursoApiService.listarCursosActivos();
        cursosCall.enqueue(new Callback<List<CursoResponse>>() {
            @Override
            public void onResponse(
                    Call<List<CursoResponse>> call,
                    Response<List<CursoResponse>> response) {
                cursosCall = null;
                cursosLoaded = true;
                if (response.isSuccessful() && response.body() != null) {
                    cursoItems.clear();
                    cursoItems.add(new NamedItem<>(
                            getString(R.string.nota_filtros_todos_cursos), null));
                    cursoNombres.clear();
                    for (CursoResponse curso : response.body()) {
                        String nombre = formatCurso(curso);
                        cursoItems.add(new NamedItem<>(nombre, curso));
                        cursoNombres.put(curso.id, nombre);
                    }
                    adapter.setNombres(estudianteNombres, cursoNombres);
                    bindAutoComplete(actCurso, cursoItems,
                            item -> selectedCurso = item == null ? null : item.value);
                } else {
                    UiNotifier.error(
                            NotaListActivity.this,
                            NotaErrorMapper.fromResponse(NotaListActivity.this, response));
                }
                markCatalogLoaded();
            }

            @Override
            public void onFailure(Call<List<CursoResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    cursosCall = null;
                    cursosLoaded = true;
                    UiNotifier.error(
                            NotaListActivity.this,
                            NotaErrorMapper.fromFailure(NotaListActivity.this, throwable));
                    markCatalogLoaded();
                }
            }
        });
    }

    private void markCatalogLoaded() {
        if (catalogsReady || !estudiantesLoaded || !cursosLoaded) {
            return;
        }
        catalogsReady = true;
        setCatalogsLoading(false);
        loadNotas(pendingPage);
    }

    private void loadNotas(int page) {
        if (loading) {
            return;
        }
        pendingPage = Math.max(page, 0);
        if (!catalogsReady) {
            return;
        }
        if (listCall != null) {
            listCall.cancel();
        }
        setLoading(true);
        txtErrorState.setVisibility(View.GONE);

        Long estudianteId = selectedEstudianteId();
        Long cursoId = selectedCursoId();
        Integer cicloAnio = selectedCiclo();
        String tipo = getText(edtTipo).trim();

        listCall = apiService.listarNotas(
                estudianteId,
                cursoId,
                cicloAnio,
                tipo.isEmpty() ? null : tipo,
                activoFilter,
                pendingPage,
                PAGE_SIZE);
        listCall.enqueue(new Callback<PageResponse<NotaResponse>>() {
            @Override
            public void onResponse(
                    Call<PageResponse<NotaResponse>> call,
                    Response<PageResponse<NotaResponse>> response) {
                setLoading(false);
                listCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    renderPage(response.body());
                    return;
                }
                showError(NotaErrorMapper.fromResponse(NotaListActivity.this, response));
            }

            @Override
            public void onFailure(Call<PageResponse<NotaResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                listCall = null;
                showError(NotaErrorMapper.fromFailure(NotaListActivity.this, throwable));
            }
        });
    }

    private void renderPage(PageResponse<NotaResponse> page) {
        currentPage = page.number;
        totalPages = page.totalPages;
        adapter.submitList(page.content);
        boolean empty = page.content == null || page.content.isEmpty();
        txtEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerNotas.setVisibility(empty ? View.GONE : View.VISIBLE);
        updatePaginationControls();
    }

    private void showError(String message) {
        adapter.submitList(null);
        recyclerNotas.setVisibility(View.GONE);
        txtEmptyState.setVisibility(View.GONE);
        txtErrorState.setText(message);
        txtErrorState.setVisibility(View.VISIBLE);
        currentPage = 0;
        totalPages = 0;
        updatePaginationControls();
        UiNotifier.error(this, message);
    }

    private void setLoading(boolean isLoading) {
        loading = isLoading;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnBuscar.setEnabled(!isLoading);
        btnAgregar.setEnabled(!isLoading);
        btnPromedio.setEnabled(!isLoading);
        btnAnterior.setEnabled(!isLoading && currentPage > 0);
        btnSiguiente.setEnabled(!isLoading && currentPage + 1 < totalPages);
        toggleEstado.setEnabled(!isLoading);
        actEstudiante.setEnabled(!isLoading && catalogsReady);
        actCurso.setEnabled(!isLoading && catalogsReady);
        spinnerCiclo.setEnabled(!isLoading);
        edtTipo.setEnabled(!isLoading);
    }

    private void setCatalogsLoading(boolean isLoading) {
        progressCatalogs.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        actEstudiante.setEnabled(!isLoading);
        actCurso.setEnabled(!isLoading);
    }

    private void setFiltersExpanded(boolean expanded) {
        filtersExpanded = expanded;
        if (expanded) {
            notaFilterControls.setVisibility(View.VISIBLE);
            notaFilterControls.setAlpha(0f);
            notaFilterControls.setTranslationY(-8f);
            notaFilterControls.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
            return;
        }
        notaFilterControls.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(140)
                .withEndAction(() -> notaFilterControls.setVisibility(View.GONE))
                .start();
    }

    private void updatePaginationControls() {
        int visiblePage = totalPages == 0 ? 0 : currentPage + 1;
        txtPageInfo.setText(getString(R.string.nota_pagina_info, visiblePage, totalPages));
        btnAnterior.setEnabled(!loading && currentPage > 0);
        btnSiguiente.setEnabled(!loading && currentPage + 1 < totalPages);
    }

    private Boolean resolveActivoFilter(int checkedId) {
        if (checkedId == R.id.btnFiltroNotaActivas) {
            return true;
        }
        if (checkedId == R.id.btnFiltroNotaInactivas) {
            return false;
        }
        return null;
    }

    private Long selectedEstudianteId() {
        return selectedEstudiante == null ? null : selectedEstudiante.id;
    }

    private Long selectedCursoId() {
        return selectedCurso == null ? null : selectedCurso.id;
    }

    private Integer selectedCiclo() {
        NamedItem<Integer> item = selectedItem(spinnerCiclo);
        return item == null ? null : item.value;
    }

    @SuppressWarnings("unchecked")
    private <T> NamedItem<T> selectedItem(Spinner spinner) {
        return (NamedItem<T>) spinner.getSelectedItem();
    }

    private <T> void bindAutoComplete(
            MaterialAutoCompleteTextView act,
            List<NamedItem<T>> items,
            Consumer<NamedItem<T>> onPick) {
        BusquedaAdapter<T> adapter = new BusquedaAdapter<>(this, items);
        String[] pickedLabel = autoCompletePickedLabels.computeIfAbsent(act, key -> new String[]{null});
        TextWatcher previous = autoCompleteWatchers.remove(act);
        if (previous != null) {
            act.removeTextChangedListener(previous);
        }
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pickedLabel[0] != null && !pickedLabel[0].equals(s == null ? "" : s.toString())) {
                    // The user edited the text, so the stored selection is stale.
                    pickedLabel[0] = null;
                    if (onPick != null) {
                        onPick.accept(null);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        autoCompleteWatchers.put(act, watcher);
        act.setAdapter(adapter);
        act.setThreshold(1);
        act.setOnItemClickListener((parent, view, position, id) -> {
            NamedItem<T> item = adapter.getItem(position);
            if (item == null) {
                return;
            }
            pickedLabel[0] = item.toString();
            act.setText(item.toString(), false);
            if (onPick != null) {
                onPick.accept(item);
            }
        });
        act.setOnClickListener(view -> act.showDropDown());
        act.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                act.showDropDown();
            }
        });
        act.addTextChangedListener(watcher);
    }

    private <T> void bindSpinner(Spinner spinner, List<NamedItem<T>> items) {
        ArrayAdapter<NamedItem<T>> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                items);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinner.setAdapter(adapter);
    }

    private String formatEstudiante(EstudianteResumenResponse estudiante) {
        return nullToEmpty(estudiante.codigoEstudiantil) + " - "
                + nullToEmpty(estudiante.nombres) + " " + nullToEmpty(estudiante.apellidos);
    }

    private String formatCurso(CursoResponse curso) {
        return nullToEmpty(curso.codigo) + " - " + nullToEmpty(curso.nombre);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private void cancelCalls() {
        if (listCall != null) {
            listCall.cancel();
        }
        if (estudiantesCall != null) {
            estudiantesCall.cancel();
        }
        if (cursosCall != null) {
            cursosCall.cancel();
        }
        for (Call<NotaResponse> call : statusCalls.values()) {
            call.cancel();
        }
        statusCalls.clear();
    }
}
