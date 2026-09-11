package gt.com.ro.devumgapp.nota.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import gt.com.ro.devumgapp.nota.dto.NotaResponse;
import gt.com.ro.devumgapp.nota.dto.PromedioResponse;
import gt.com.ro.devumgapp.nota.network.NotaApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PromedioNotasActivity extends AppCompatActivity {

    public static final String EXTRA_ESTUDIANTE_ID = "estudianteId";

    private MaterialToolbar toolbar;
    private LinearProgressIndicator progressBar;
    private View promedioNotasHero;
    private View promedioNotasPanel;
    private MaterialAutoCompleteTextView actEstudiante;
    private MaterialAutoCompleteTextView actCarrera;
    private MaterialAutoCompleteTextView actCurso;
    private View promedioFiltersHeader;
    private View promedioFilterControls;
    private TextView txtPromedioFiltrosChevron;
    private View promedioResultado;
    private TextView txtPromedioValor;
    private TextView txtPromedioCantidad;
    private TextView txtPromedioTotal;
    private RecyclerView recyclerNotaPromedio;
    private TextView txtEmptyState;
    private TextView txtErrorState;
    private EstudianteResumenResponse selectedEstudiante;
    private CarreraResumenResponse selectedCarrera;
    private CursoResponse selectedCurso;
    private final Map<MaterialAutoCompleteTextView, TextWatcher> autoCompleteWatchers = new HashMap<>();
    private final Map<MaterialAutoCompleteTextView, String[]> autoCompletePickedLabels = new HashMap<>();

    private NotaApiService apiService;
    private EstudianteApiService estudianteApiService;
    private CarreraApiService carreraApiService;
    private CursoApiService cursoApiService;
    private Call<List<EstudianteResumenResponse>> estudiantesCall;
    private Call<List<CarreraResumenResponse>> carrerasCall;
    private Call<List<CursoResponse>> cursosCall;
    private Call<PromedioResponse> promedioCall;
    private Call<PageResponse<NotaResponse>> activasCall;
    private final List<NamedItem<EstudianteResumenResponse>> estudianteItems = new ArrayList<>();
    private final List<NamedItem<CarreraResumenResponse>> carreraItems = new ArrayList<>();
    private final List<NamedItem<CursoResponse>> cursoItems = new ArrayList<>();
    private final Map<Long, CursoResponse> cursoPorId = new HashMap<>();
    private final Map<Long, String> cursoNombrePorId = new HashMap<>();
    private List<NotaResponse> pendingActivas;
    private PromedioResponse pendingPromedio;
    private String pendingError;
    private long pendingEstudianteId = -1L;
    private boolean loading;
    private boolean estudiantesLoaded;
    private boolean carrerasLoaded;
    private boolean cursosLoaded;
    private boolean activasLoaded;
    private boolean promedioLoaded;
    private boolean promedioRequestActive;
    private boolean filtersExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nota_promedio);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        pendingEstudianteId = getIntent().getLongExtra(EXTRA_ESTUDIANTE_ID, -1L);
        apiService = RetrofitClient.getClient().create(NotaApiService.class);
        estudianteApiService = RetrofitClient.getClient().create(EstudianteApiService.class);
        carreraApiService = RetrofitClient.getClient().create(CarreraApiService.class);
        cursoApiService = RetrofitClient.getClient().create(CursoApiService.class);
        bindViews();
        setupToolbar();
        initializeSpinners();
        animateIntro();
        loadCatalogs();
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarPromedioNotas);
        toolbar.setTitle("");
        toolbar.setNavigationOnClickListener(view -> finish());
        progressBar = findViewById(R.id.progressPromedioNotas);
        promedioNotasHero = findViewById(R.id.promedioNotasHero);
        promedioNotasPanel = findViewById(R.id.promedioNotasPanel);
        actEstudiante = findViewById(R.id.actPromedioEstudiante);
        actCarrera = findViewById(R.id.actPromedioCarrera);
        actCurso = findViewById(R.id.actPromedioCurso);
        promedioFiltersHeader = findViewById(R.id.promedioFiltersHeader);
        promedioFilterControls = findViewById(R.id.promedioFilterControls);
        txtPromedioFiltrosChevron = findViewById(R.id.txtPromedioFiltrosChevron);
        promedioFiltersHeader.setOnClickListener(view -> setFiltersExpanded(!filtersExpanded));
        promedioResultado = findViewById(R.id.promedioResultado);
        txtPromedioValor = findViewById(R.id.txtNotaPromedioValor);
        txtPromedioCantidad = findViewById(R.id.txtNotaPromedioCantidad);
        txtPromedioTotal = findViewById(R.id.txtNotaPromedioTotal);
        recyclerNotaPromedio = findViewById(R.id.recyclerNotaPromedio);
        recyclerNotaPromedio.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotaPromedio.setAdapter(new NotaPromedioAdapter());
        txtEmptyState = findViewById(R.id.txtPromedioNotasEmpty);
        txtErrorState = findViewById(R.id.txtPromedioNotasError);
    }

    private void setupToolbar() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void initializeSpinners() {
        estudianteItems.add(new NamedItem<>(getString(R.string.nota_promedio_selecciona_estudiante), null));
        bindAutoComplete(actEstudiante, estudianteItems, item -> {
            selectedEstudiante = item == null ? null : item.value;
            if (selectedEstudiante == null) {
                showEmptyState();
            } else {
                loadPromedio(selectedEstudiante.id);
            }
        });

        carreraItems.add(new NamedItem<>(getString(R.string.nota_promedio_todas_carreras), null));
        bindAutoComplete(actCarrera, carreraItems, item -> {
            selectedCarrera = item == null ? null : item.value;
            applyFilters();
        });

        cursoItems.add(new NamedItem<>(getString(R.string.nota_promedio_todos_cursos), null));
        bindAutoComplete(actCurso, cursoItems, item -> {
            selectedCurso = item == null ? null : item.value;
            applyFilters();
        });
    }

    private void loadCatalogs() {
        setLoading(true);
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
                            getString(R.string.nota_promedio_selecciona_estudiante), null));
                    for (EstudianteResumenResponse estudiante : response.body()) {
                        estudianteItems.add(new NamedItem<>(formatEstudiante(estudiante), estudiante));
                    }
                    bindAutoComplete(actEstudiante, estudianteItems, item -> {
                        selectedEstudiante = item == null ? null : item.value;
                        if (selectedEstudiante == null) {
                            showEmptyState();
                        } else {
                            loadPromedio(selectedEstudiante.id);
                        }
                    });
                    if (pendingEstudianteId > 0 && selectEstudiante(pendingEstudianteId)) {
                        loadPromedio(pendingEstudianteId);
                        return;
                    }
                } else {
                    UiNotifier.error(
                            PromedioNotasActivity.this,
                            NotaErrorMapper.fromResponse(PromedioNotasActivity.this, response));
                }
                finishInitialLoadIfReady();
            }

            @Override
            public void onFailure(Call<List<EstudianteResumenResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    estudiantesCall = null;
                    estudiantesLoaded = true;
                    finishInitialLoadIfReady();
                    UiNotifier.error(
                            PromedioNotasActivity.this,
                            NotaErrorMapper.fromFailure(PromedioNotasActivity.this, throwable));
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
                            getString(R.string.nota_promedio_todas_carreras), null));
                    for (CarreraResumenResponse carrera : response.body()) {
                        carreraItems.add(new NamedItem<>(formatCarrera(carrera), carrera));
                    }
                    bindAutoComplete(actCarrera, carreraItems, item -> {
                        selectedCarrera = item == null ? null : item.value;
                        applyFilters();
                    });
                } else {
                    UiNotifier.error(
                            PromedioNotasActivity.this,
                            NotaErrorMapper.fromResponse(PromedioNotasActivity.this, response));
                }
                finishInitialLoadIfReady();
            }

            @Override
            public void onFailure(Call<List<CarreraResumenResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    carrerasCall = null;
                    carrerasLoaded = true;
                    finishInitialLoadIfReady();
                    UiNotifier.error(
                            PromedioNotasActivity.this,
                            NotaErrorMapper.fromFailure(PromedioNotasActivity.this, throwable));
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
                    cursoPorId.clear();
                    cursoNombrePorId.clear();
                    cursoItems.add(new NamedItem<>(
                            getString(R.string.nota_promedio_todos_cursos), null));
                    for (CursoResponse curso : response.body()) {
                        String nombre = formatCurso(curso);
                        cursoItems.add(new NamedItem<>(nombre, curso));
                        cursoPorId.put(curso.id, curso);
                        cursoNombrePorId.put(curso.id, nombre);
                    }
                    bindAutoComplete(actCurso, cursoItems, item -> {
                        selectedCurso = item == null ? null : item.value;
                        applyFilters();
                    });
                } else {
                    UiNotifier.error(
                            PromedioNotasActivity.this,
                            NotaErrorMapper.fromResponse(PromedioNotasActivity.this, response));
                }
                finishInitialLoadIfReady();
            }

            @Override
            public void onFailure(Call<List<CursoResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    cursosCall = null;
                    cursosLoaded = true;
                    finishInitialLoadIfReady();
                    UiNotifier.error(
                            PromedioNotasActivity.this,
                            NotaErrorMapper.fromFailure(PromedioNotasActivity.this, throwable));
                }
            }
        });
    }

    private void finishInitialLoadIfReady() {
        if (estudiantesLoaded && carrerasLoaded && cursosLoaded && !promedioRequestActive) {
            setLoading(false);
        }
    }

    private void loadPromedio(long estudianteId) {
        cancelPromedioCalls();
        promedioLoaded = false;
        activasLoaded = false;
        pendingPromedio = null;
        pendingActivas = null;
        pendingError = null;
        promedioRequestActive = true;
        setLoading(true);
        txtErrorState.setVisibility(View.GONE);

        promedioCall = apiService.obtenerPromedio(estudianteId);
        promedioCall.enqueue(new Callback<PromedioResponse>() {
            @Override
            public void onResponse(Call<PromedioResponse> call, Response<PromedioResponse> response) {
                promedioCall = null;
                promedioLoaded = true;
                if (response.isSuccessful() && response.body() != null) {
                    pendingPromedio = response.body();
                } else if (pendingError == null) {
                    pendingError = NotaErrorMapper.fromResponse(PromedioNotasActivity.this, response);
                }
                maybeRender();
            }

            @Override
            public void onFailure(Call<PromedioResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                promedioCall = null;
                promedioLoaded = true;
                if (pendingError == null) {
                    pendingError = NotaErrorMapper.fromFailure(PromedioNotasActivity.this, throwable);
                }
                maybeRender();
            }
        });

        activasCall = apiService.listarNotasActivasPorEstudiante(estudianteId, 0, 200);
        activasCall.enqueue(new Callback<PageResponse<NotaResponse>>() {
            @Override
            public void onResponse(
                    Call<PageResponse<NotaResponse>> call,
                    Response<PageResponse<NotaResponse>> response) {
                activasCall = null;
                activasLoaded = true;
                if (response.isSuccessful() && response.body() != null) {
                    pendingActivas = response.body().content;
                } else if (pendingError == null) {
                    pendingError = NotaErrorMapper.fromResponse(PromedioNotasActivity.this, response);
                }
                maybeRender();
            }

            @Override
            public void onFailure(Call<PageResponse<NotaResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                activasCall = null;
                activasLoaded = true;
                if (pendingError == null) {
                    pendingError = NotaErrorMapper.fromFailure(PromedioNotasActivity.this, throwable);
                }
                maybeRender();
            }
        });
    }

    private void maybeRender() {
        if (!promedioLoaded || !activasLoaded || !carrerasLoaded || !cursosLoaded) {
            return;
        }
        promedioRequestActive = false;
        setLoading(false);
        if (pendingError != null) {
            showError(pendingError);
            return;
        }
        render(pendingPromedio, pendingActivas);
    }

    private void render(PromedioResponse promedio, List<NotaResponse> notas) {
        if (promedio == null || notas == null) {
            showEmptyState();
            return;
        }
        txtPromedioTotal.setText(getString(
                R.string.nota_promedio_total,
                String.format(Locale.US, "%.2f", promedio.promedioGeneral),
                promedio.cantidadNotas));
        promedioResultado.setVisibility(View.VISIBLE);
        txtErrorState.setVisibility(View.GONE);
        applyFilters();
    }

    private void applyFilters() {
        if (pendingActivas == null) {
            getAdapter().submitList(null);
            recyclerNotaPromedio.setVisibility(View.GONE);
            txtEmptyState.setVisibility(View.GONE);
            return;
        }
        List<NotaPromedioAdapter.NotaPromedioRow> rows = new ArrayList<>();
        for (NotaResponse nota : pendingActivas) {
            CursoResponse curso = cursoPorId.get(nota.cursoId);
            if (selectedCurso != null && nota.cursoId != selectedCurso.id) {
                continue;
            }
            if (selectedCarrera != null && (curso == null || curso.carreraId != selectedCarrera.id)) {
                continue;
            }
            rows.add(new NotaPromedioAdapter.NotaPromedioRow(
                    resolverNombreCurso(nota.cursoId),
                    nullToEmpty(nota.tipoEvaluacion),
                    nota.cicloAnio,
                    nota.calificacion));
        }
        getAdapter().submitList(rows);
        if (rows.isEmpty()) {
            txtPromedioValor.setText(getString(
                    R.string.nota_promedio_valor, getString(R.string.nota_promedio_sin_valor)));
            txtPromedioCantidad.setText(getString(R.string.nota_promedio_cantidad, 0));
            recyclerNotaPromedio.setVisibility(View.GONE);
            txtEmptyState.setText(pendingActivas.isEmpty()
                    ? R.string.nota_promedio_sin_notas
                    : R.string.nota_promedio_sin_resultados);
            txtEmptyState.setVisibility(View.VISIBLE);
        } else {
            double suma = 0.0;
            for (NotaPromedioAdapter.NotaPromedioRow row : rows) {
                suma += row.calificacion;
            }
            txtPromedioValor.setText(getString(
                    R.string.nota_promedio_valor,
                    String.format(Locale.US, "%.2f", suma / rows.size())));
            txtPromedioCantidad.setText(getString(R.string.nota_promedio_cantidad, rows.size()));
            txtEmptyState.setVisibility(View.GONE);
            recyclerNotaPromedio.setVisibility(View.VISIBLE);
        }
    }

    private NotaPromedioAdapter getAdapter() {
        return (NotaPromedioAdapter) recyclerNotaPromedio.getAdapter();
    }

    private String resolverNombreCurso(long cursoId) {
        String nombre = cursoNombrePorId.get(cursoId);
        return nombre != null ? nombre : getString(R.string.nota_item_id, cursoId);
    }

    private void showEmptyState() {
        promedioResultado.setVisibility(View.GONE);
        txtErrorState.setVisibility(View.GONE);
        txtEmptyState.setText(R.string.nota_promedio_empty);
        txtEmptyState.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        promedioResultado.setVisibility(View.GONE);
        txtEmptyState.setVisibility(View.GONE);
        txtErrorState.setText(message);
        txtErrorState.setVisibility(View.VISIBLE);
        UiNotifier.error(this, message);
    }

    private void setLoading(boolean isLoading) {
        loading = isLoading;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        actEstudiante.setEnabled(!isLoading);
        actCarrera.setEnabled(!isLoading);
        actCurso.setEnabled(!isLoading);
    }

    private void setFiltersExpanded(boolean expanded) {
        filtersExpanded = expanded;
        txtPromedioFiltrosChevron.setText(expanded
                ? R.string.nota_promedio_filtros_cerrar
                : R.string.nota_promedio_filtros_abrir);
        if (expanded) {
            promedioFilterControls.setVisibility(View.VISIBLE);
            promedioFilterControls.setAlpha(0f);
            promedioFilterControls.setTranslationY(-8f);
            promedioFilterControls.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
            return;
        }
        promedioFilterControls.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(140)
                .withEndAction(() -> promedioFilterControls.setVisibility(View.GONE))
                .start();
    }

    private void animateIntro() {
        promedioNotasHero.setAlpha(0f);
        promedioNotasHero.setTranslationY(20f);
        promedioNotasHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        promedioNotasPanel.setAlpha(0f);
        promedioNotasPanel.setTranslationY(18f);
        promedioNotasPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(320)
                .start();
    }

    private boolean selectEstudiante(long estudianteId) {
        for (NamedItem<EstudianteResumenResponse> item : estudianteItems) {
            EstudianteResumenResponse estudiante = item.value;
            if (estudiante != null && estudiante.id == estudianteId) {
                selectedEstudiante = estudiante;
                setAutoCompleteText(actEstudiante, item.toString());
                return true;
            }
        }
        return false;
    }

    private void setAutoCompleteText(MaterialAutoCompleteTextView act, String label) {
        String[] pickedLabel = autoCompletePickedLabels.computeIfAbsent(act, key -> new String[]{null});
        pickedLabel[0] = label;
        act.setText(label, false);
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

    private void cancelPromedioCalls() {
        if (promedioCall != null) {
            promedioCall.cancel();
        }
        if (activasCall != null) {
            activasCall.cancel();
        }
    }

    private void cancelCalls() {
        if (estudiantesCall != null) {
            estudiantesCall.cancel();
        }
        if (carrerasCall != null) {
            carrerasCall.cancel();
        }
        if (cursosCall != null) {
            cursosCall.cancel();
        }
        cancelPromedioCalls();
    }
}
