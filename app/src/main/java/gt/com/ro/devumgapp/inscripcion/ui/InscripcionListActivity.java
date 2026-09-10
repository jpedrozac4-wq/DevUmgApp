package gt.com.ro.devumgapp.inscripcion.ui;

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
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.carrera.dto.CarreraResumenResponse;
import gt.com.ro.devumgapp.carrera.network.CarreraApiService;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.curso.dto.CursoResponse;
import gt.com.ro.devumgapp.curso.network.CursoApiService;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import gt.com.ro.devumgapp.inscripcion.dto.InscripcionResponse;
import gt.com.ro.devumgapp.inscripcion.network.InscripcionApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InscripcionListActivity extends AppCompatActivity implements InscripcionAdapter.Listener {

    private static final int PAGE_SIZE = 10;

    private MaterialToolbar toolbar;
    private MaterialAutoCompleteTextView actEstudiante;
    private MaterialAutoCompleteTextView actCarrera;
    private MaterialAutoCompleteTextView actCurso;
    private Spinner spinnerCiclo;
    private EstudianteResumenResponse selectedEstudiante;
    private CarreraResumenResponse selectedCarrera;
    private CursoResponse selectedCurso;
    private TextInputEditText edtGrado;
    private TextInputEditText edtSeccion;
    private MaterialButtonToggleGroup toggleEstado;
    private RecyclerView recyclerInscripciones;
    private LinearProgressIndicator progressBar;
    private LinearProgressIndicator progressCatalogs;
    private TextView txtEmptyState;
    private TextView txtErrorState;
    private TextView txtPageInfo;
    private View inscripcionHero;
    private View inscripcionFilters;
    private View inscripcionFiltersHeader;
    private View inscripcionFilterControls;
    private MaterialButton btnBuscar;
    private MaterialButton btnAgregar;
    private MaterialButton btnAnterior;
    private MaterialButton btnSiguiente;

    private InscripcionApiService apiService;
    private EstudianteApiService estudianteApiService;
    private CarreraApiService carreraApiService;
    private CursoApiService cursoApiService;
    private InscripcionAdapter adapter;
    private Call<PageResponse<InscripcionResponse>> listCall;
    private Call<List<EstudianteResumenResponse>> estudiantesCall;
    private Call<List<CarreraResumenResponse>> carrerasCall;
    private Call<List<CursoResponse>> cursosCall;
    private final List<NamedItem<EstudianteResumenResponse>> estudianteItems = new ArrayList<>();
    private final List<NamedItem<CarreraResumenResponse>> carreraItems = new ArrayList<>();
    private final List<NamedItem<CursoResponse>> cursoItems = new ArrayList<>();
    private final List<NamedItem<Integer>> cicloItems = new ArrayList<>();
    private final Map<MaterialAutoCompleteTextView, TextWatcher> autoCompleteWatchers = new HashMap<>();
    private final Map<MaterialAutoCompleteTextView, String[]> autoCompletePickedLabels = new HashMap<>();
    private final Map<Long, String> estudianteNombres = new HashMap<>();
    private final Map<Long, String> carreraNombres = new HashMap<>();
    private final Map<Long, String> cursoNombres = new HashMap<>();
    private ActivityResultLauncher<Intent> formLauncher;
    private ActivityResultLauncher<Intent> detailLauncher;

    private int currentPage;
    private int totalPages;
    private int pendingPage;
    private boolean loading;
    private String estadoFilter;
    private boolean firstResume = true;
    private boolean filtersExpanded;
    private boolean estudiantesLoaded;
    private boolean carrerasLoaded;
    private boolean cursosLoaded;
    private boolean catalogsReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inscripcion_list);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        apiService = RetrofitClient.getClient().create(InscripcionApiService.class);
        estudianteApiService = RetrofitClient.getClient().create(EstudianteApiService.class);
        carreraApiService = RetrofitClient.getClient().create(CarreraApiService.class);
        cursoApiService = RetrofitClient.getClient().create(CursoApiService.class);
        formLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadInscripciones(currentPage);
                    }
                });
        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadInscripciones(currentPage);
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
        loadInscripciones(0);
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
        loadInscripciones(currentPage);
    }

    @Override
    public void onEdit(InscripcionResponse inscripcion) {
        Intent intent = new Intent(this, InscripcionFormActivity.class);
        intent.putExtra(InscripcionFormActivity.EXTRA_INSCRIPCION_ID, inscripcion.id);
        formLauncher.launch(intent);
    }

    @Override
    public void onOpenDetail(InscripcionResponse inscripcion) {
        Intent intent = new Intent(this, InscripcionDetailActivity.class);
        intent.putExtra(InscripcionDetailActivity.EXTRA_INSCRIPCION_ID, inscripcion.id);
        detailLauncher.launch(intent);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarInscripciones);
        actEstudiante = findViewById(R.id.actInscripcionEstudiante);
        actCarrera = findViewById(R.id.actInscripcionCarrera);
        actCurso = findViewById(R.id.actInscripcionCurso);
        spinnerCiclo = findViewById(R.id.spnInscripcionCiclo);
        edtGrado = findViewById(R.id.edtInscripcionGrado);
        edtSeccion = findViewById(R.id.edtInscripcionSeccion);
        toggleEstado = findViewById(R.id.toggleEstadoInscripcion);
        recyclerInscripciones = findViewById(R.id.recyclerInscripciones);
        progressBar = findViewById(R.id.progressInscripciones);
        progressCatalogs = findViewById(R.id.progressInscripcionCatalogos);
        txtEmptyState = findViewById(R.id.txtInscripcionesEmpty);
        txtErrorState = findViewById(R.id.txtInscripcionesError);
        txtPageInfo = findViewById(R.id.txtInscripcionesPageInfo);
        inscripcionHero = findViewById(R.id.inscripcionHero);
        inscripcionFilters = findViewById(R.id.inscripcionFilters);
        inscripcionFiltersHeader = findViewById(R.id.inscripcionFiltersHeader);
        inscripcionFilterControls = findViewById(R.id.inscripcionFilterControls);
        btnBuscar = findViewById(R.id.btnBuscarInscripcion);
        btnAgregar = findViewById(R.id.btnAgregarInscripcion);
        btnAnterior = findViewById(R.id.btnInscripcionesAnterior);
        btnSiguiente = findViewById(R.id.btnInscripcionesSiguiente);
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
        inscripcionHero.setAlpha(0f);
        inscripcionHero.setTranslationY(20f);
        inscripcionHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        inscripcionFilters.setAlpha(0f);
        inscripcionFilters.setTranslationY(16f);
        inscripcionFilters.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(110)
                .setDuration(300)
                .start();
    }

    private void setupRecycler() {
        adapter = new InscripcionAdapter(this);
        recyclerInscripciones.setLayoutManager(new LinearLayoutManager(this));
        recyclerInscripciones.setAdapter(adapter);
    }

    private void setupFilters() {
        toggleEstado.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            String nextFilter = resolveEstadoFilter(checkedId);
            if (Objects.equals(estadoFilter, nextFilter)) {
                return;
            }
            estadoFilter = nextFilter;
            loadInscripciones(0);
        });
    }

    private void setupActions() {
        inscripcionFiltersHeader.setOnClickListener(view -> setFiltersExpanded(!filtersExpanded));
        btnBuscar.setOnClickListener(view -> loadInscripciones(0));
        edtSeccion.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadInscripciones(0);
                return true;
            }
            return false;
        });
        btnAgregar.setOnClickListener(view ->
                formLauncher.launch(new Intent(this, InscripcionFormActivity.class)));
        btnAnterior.setOnClickListener(view -> {
            if (currentPage > 0) {
                loadInscripciones(currentPage - 1);
            }
        });
        btnSiguiente.setOnClickListener(view -> {
            if (currentPage + 1 < totalPages) {
                loadInscripciones(currentPage + 1);
            }
        });
    }

    private void initializeSpinners() {
        estudianteItems.add(new NamedItem<>(getString(R.string.inscripcion_filtros_todos_estudiantes), null));
        carreraItems.add(new NamedItem<>(getString(R.string.inscripcion_filtros_todas_carreras), null));
        cursoItems.add(new NamedItem<>(getString(R.string.inscripcion_filtros_todos_cursos), null));
        int year = Calendar.getInstance().get(Calendar.YEAR);
        cicloItems.add(new NamedItem<>(getString(R.string.inscripcion_filtros_todos_ciclos), null));
        for (int itemYear = Math.max(2020, year - 3); itemYear <= year + 3; itemYear++) {
            cicloItems.add(new NamedItem<>(String.valueOf(itemYear), itemYear));
        }
        bindAutoComplete(actEstudiante, estudianteItems,
                item -> selectedEstudiante = item == null ? null : item.value);
        bindAutoComplete(actCarrera, carreraItems,
                item -> selectedCarrera = item == null ? null : item.value);
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
                            getString(R.string.inscripcion_filtros_todos_estudiantes), null));
                    estudianteNombres.clear();
                    for (EstudianteResumenResponse estudiante : response.body()) {
                        String nombre = formatEstudiante(estudiante);
                        estudianteItems.add(new NamedItem<>(nombre, estudiante));
                        estudianteNombres.put(estudiante.id, nombre);
                    }
                    adapter.setNombres(estudianteNombres, carreraNombres, cursoNombres);
                    bindAutoComplete(actEstudiante, estudianteItems,
                            item -> selectedEstudiante = item == null ? null : item.value);
                } else {
                    UiNotifier.error(
                            InscripcionListActivity.this,
                            InscripcionErrorMapper.fromResponse(InscripcionListActivity.this, response));
                }
                markCatalogLoaded();
            }

            @Override
            public void onFailure(Call<List<EstudianteResumenResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    estudiantesCall = null;
                    estudiantesLoaded = true;
                    UiNotifier.error(
                            InscripcionListActivity.this,
                            InscripcionErrorMapper.fromFailure(InscripcionListActivity.this, throwable));
                    markCatalogLoaded();
                }
            }
        });

        carrerasCall = carreraApiService.listarCarrerasActivas();
        carrerasCall.enqueue(new Callback<List<CarreraResumenResponse>>() {
            @Override
            public void onResponse(
                    Call<List<CarreraResumenResponse>> call,
                    Response<List<CarreraResumenResponse>> response) {
                carrerasCall = null;
                carrerasLoaded = true;
                if (response.isSuccessful() && response.body() != null) {
                    carreraItems.clear();
                    carreraItems.add(new NamedItem<>(
                            getString(R.string.inscripcion_filtros_todas_carreras), null));
                    carreraNombres.clear();
                    for (CarreraResumenResponse carrera : response.body()) {
                        String nombre = formatCarrera(carrera);
                        carreraItems.add(new NamedItem<>(nombre, carrera));
                        carreraNombres.put(carrera.id, nombre);
                    }
                    adapter.setNombres(estudianteNombres, carreraNombres, cursoNombres);
                    bindAutoComplete(actCarrera, carreraItems,
                            item -> selectedCarrera = item == null ? null : item.value);
                } else {
                    UiNotifier.error(
                            InscripcionListActivity.this,
                            InscripcionErrorMapper.fromResponse(InscripcionListActivity.this, response));
                }
                markCatalogLoaded();
            }

            @Override
            public void onFailure(Call<List<CarreraResumenResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    carrerasCall = null;
                    carrerasLoaded = true;
                    UiNotifier.error(
                            InscripcionListActivity.this,
                            InscripcionErrorMapper.fromFailure(InscripcionListActivity.this, throwable));
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
                            getString(R.string.inscripcion_filtros_todos_cursos), null));
                    cursoNombres.clear();
                    for (CursoResponse curso : response.body()) {
                        String nombre = formatCurso(curso);
                        cursoItems.add(new NamedItem<>(nombre, curso));
                        cursoNombres.put(curso.id, nombre);
                    }
                    adapter.setNombres(estudianteNombres, carreraNombres, cursoNombres);
                    bindAutoComplete(actCurso, cursoItems,
                            item -> selectedCurso = item == null ? null : item.value);
                } else {
                    UiNotifier.error(
                            InscripcionListActivity.this,
                            InscripcionErrorMapper.fromResponse(InscripcionListActivity.this, response));
                }
                markCatalogLoaded();
            }

            @Override
            public void onFailure(Call<List<CursoResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    cursosCall = null;
                    cursosLoaded = true;
                    UiNotifier.error(
                            InscripcionListActivity.this,
                            InscripcionErrorMapper.fromFailure(InscripcionListActivity.this, throwable));
                    markCatalogLoaded();
                }
            }
        });
    }

    private void markCatalogLoaded() {
        if (catalogsReady || !estudiantesLoaded || !carrerasLoaded || !cursosLoaded) {
            return;
        }
        catalogsReady = true;
        setCatalogsLoading(false);
        loadInscripciones(pendingPage);
    }

    private void loadInscripciones(int page) {
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
        Long carreraId = selectedCarreraId();
        Long cursoId = selectedCursoId();
        Integer cicloAnio = selectedCiclo();
        String grado = getText(edtGrado).trim();
        String seccion = getText(edtSeccion).trim();

        listCall = apiService.listarInscripciones(
                estudianteId,
                carreraId,
                cursoId,
                cicloAnio,
                grado.isEmpty() ? null : grado,
                seccion.isEmpty() ? null : seccion,
                estadoFilter,
                null,
                pendingPage,
                PAGE_SIZE);
        listCall.enqueue(new Callback<PageResponse<InscripcionResponse>>() {
            @Override
            public void onResponse(
                    Call<PageResponse<InscripcionResponse>> call,
                    Response<PageResponse<InscripcionResponse>> response) {
                setLoading(false);
                listCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    renderPage(response.body());
                    return;
                }
                showError(InscripcionErrorMapper.fromResponse(InscripcionListActivity.this, response));
            }

            @Override
            public void onFailure(Call<PageResponse<InscripcionResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                listCall = null;
                showError(InscripcionErrorMapper.fromFailure(InscripcionListActivity.this, throwable));
            }
        });
    }

    private void renderPage(PageResponse<InscripcionResponse> page) {
        currentPage = page.number;
        totalPages = page.totalPages;
        adapter.submitList(page.content);
        boolean empty = page.content == null || page.content.isEmpty();
        txtEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerInscripciones.setVisibility(empty ? View.GONE : View.VISIBLE);
        updatePaginationControls();
    }

    private void showError(String message) {
        adapter.submitList(null);
        recyclerInscripciones.setVisibility(View.GONE);
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
        btnAnterior.setEnabled(!isLoading && currentPage > 0);
        btnSiguiente.setEnabled(!isLoading && currentPage + 1 < totalPages);
        toggleEstado.setEnabled(!isLoading);
        actEstudiante.setEnabled(!isLoading && catalogsReady);
        actCarrera.setEnabled(!isLoading && catalogsReady);
        actCurso.setEnabled(!isLoading && catalogsReady);
        spinnerCiclo.setEnabled(!isLoading);
        edtGrado.setEnabled(!isLoading);
        edtSeccion.setEnabled(!isLoading);
    }

    private void setCatalogsLoading(boolean isLoading) {
        progressCatalogs.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        actEstudiante.setEnabled(!isLoading);
        actCarrera.setEnabled(!isLoading);
        actCurso.setEnabled(!isLoading);
    }

    private void setFiltersExpanded(boolean expanded) {
        filtersExpanded = expanded;
        if (expanded) {
            inscripcionFilterControls.setVisibility(View.VISIBLE);
            inscripcionFilterControls.setAlpha(0f);
            inscripcionFilterControls.setTranslationY(-8f);
            inscripcionFilterControls.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
            return;
        }
        inscripcionFilterControls.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(140)
                .withEndAction(() -> inscripcionFilterControls.setVisibility(View.GONE))
                .start();
    }

    private void updatePaginationControls() {
        int visiblePage = totalPages == 0 ? 0 : currentPage + 1;
        txtPageInfo.setText(getString(R.string.inscripcion_pagina_info, visiblePage, totalPages));
        btnAnterior.setEnabled(!loading && currentPage > 0);
        btnSiguiente.setEnabled(!loading && currentPage + 1 < totalPages);
    }

    private String resolveEstadoFilter(int checkedId) {
        if (checkedId == R.id.btnFiltroInscripcionActivas) {
            return "ACTIVA";
        }
        if (checkedId == R.id.btnFiltroInscripcionAnuladas) {
            return "ANULADA";
        }
        return null;
    }

    private Long selectedEstudianteId() {
        return selectedEstudiante == null ? null : selectedEstudiante.id;
    }

    private Long selectedCarreraId() {
        return selectedCarrera == null ? null : selectedCarrera.id;
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

    private String formatCarrera(CarreraResumenResponse carrera) {
        return nullToEmpty(carrera.codigo) + " - " + nullToEmpty(carrera.nombre);
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
        if (carrerasCall != null) {
            carrerasCall.cancel();
        }
        if (cursosCall != null) {
            cursosCall.cancel();
        }
    }
}
