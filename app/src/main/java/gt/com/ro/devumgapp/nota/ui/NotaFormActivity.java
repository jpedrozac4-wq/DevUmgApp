package gt.com.ro.devumgapp.nota.ui;

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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.curso.dto.CursoResponse;
import gt.com.ro.devumgapp.curso.network.CursoApiService;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import gt.com.ro.devumgapp.nota.dto.NotaRequest;
import gt.com.ro.devumgapp.nota.dto.NotaResponse;
import gt.com.ro.devumgapp.nota.dto.NotaUpdateRequest;
import gt.com.ro.devumgapp.nota.network.NotaApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotaFormActivity extends AppCompatActivity {

    public static final String EXTRA_NOTA_ID = "notaId";
    private static final long NEW_NOTA_ID = -1L;

    private MaterialToolbar toolbar;
    private TextView txtHeroTitle;
    private View notaFormHero;
    private View notaFormPanel;
    private TextView txtEstudianteNota;
    private TextInputLayout tilTipo;
    private TextInputLayout tilCalificacion;
    private TextInputLayout tilObservaciones;
    private TextInputEditText edtTipo;
    private TextInputEditText edtCalificacion;
    private TextInputEditText edtObservaciones;
    private MaterialAutoCompleteTextView actEstudiante;
    private MaterialAutoCompleteTextView actCurso;
    private EstudianteResumenResponse selectedEstudiante;
    private CursoResponse selectedCurso;
    private final Map<MaterialAutoCompleteTextView, TextWatcher> autoCompleteWatchers = new HashMap<>();
    private final Map<MaterialAutoCompleteTextView, String[]> autoCompletePickedLabels = new HashMap<>();
    private MaterialButton btnGuardar;
    private LinearProgressIndicator progressBar;

    private NotaApiService apiService;
    private EstudianteApiService estudianteApiService;
    private CursoApiService cursoApiService;
    private Call<NotaResponse> loadCall;
    private Call<NotaResponse> saveCall;
    private Call<List<EstudianteResumenResponse>> estudiantesCall;
    private Call<List<CursoResponse>> cursosCall;
    private final List<NamedItem<EstudianteResumenResponse>> estudianteItems = new ArrayList<>();
    private final List<NamedItem<CursoResponse>> cursoItems = new ArrayList<>();
    private NotaResponse pendingNota;
    private long notaId = NEW_NOTA_ID;
    private boolean loading;
    private boolean estudiantesLoaded;
    private boolean cursosLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nota_form);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        notaId = getIntent().getLongExtra(EXTRA_NOTA_ID, NEW_NOTA_ID);
        apiService = RetrofitClient.getClient().create(NotaApiService.class);
        estudianteApiService = RetrofitClient.getClient().create(EstudianteApiService.class);
        cursoApiService = RetrofitClient.getClient().create(CursoApiService.class);
        bindViews();
        setupToolbar();
        setupActions();
        initializeSpinners();
        configureModeSpecificUi();
        animateIntro();
        loadCatalogs();
        if (isEditMode()) {
            loadNota();
        }
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarNotaForm);
        int title = isEditMode()
                ? R.string.nota_form_titulo_editar
                : R.string.nota_form_titulo_crear;
        toolbar.setTitle(title);
        toolbar.setNavigationOnClickListener(view -> finish());
        txtHeroTitle = findViewById(R.id.txtNotaFormHeroTitle);
        txtHeroTitle.setText(title);
        notaFormHero = findViewById(R.id.notaFormHero);
        notaFormPanel = findViewById(R.id.notaFormPanel);
        txtEstudianteNota = findViewById(R.id.txtNotaEstudianteNota);
        tilTipo = findViewById(R.id.tilNotaTipo);
        tilCalificacion = findViewById(R.id.tilNotaCalificacion);
        tilObservaciones = findViewById(R.id.tilNotaObservaciones);
        edtTipo = findViewById(R.id.edtNotaTipo);
        edtCalificacion = findViewById(R.id.edtNotaCalificacion);
        edtObservaciones = findViewById(R.id.edtNotaObservaciones);
        actEstudiante = findViewById(R.id.actNotaEstudiante);
        actCurso = findViewById(R.id.actNotaCurso);
        btnGuardar = findViewById(R.id.btnGuardarNota);
        progressBar = findViewById(R.id.progressNotaForm);
    }

    private void setupToolbar() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void setupActions() {
        btnGuardar.setOnClickListener(view -> saveNota());
        edtObservaciones.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveNota();
                return true;
            }
            return false;
        });
    }

    private void initializeSpinners() {
        estudianteItems.add(new NamedItem<>(getString(R.string.nota_selecciona_estudiante), null));
        cursoItems.add(new NamedItem<>(getString(R.string.nota_selecciona_curso), null));
        bindAutoComplete(actEstudiante, estudianteItems,
                item -> selectedEstudiante = item == null ? null : item.value);
        bindAutoComplete(actCurso, cursoItems,
                item -> selectedCurso = item == null ? null : item.value);
    }

    private void configureModeSpecificUi() {
        if (!isEditMode()) {
            return;
        }
        // The backend update contract excludes estudiante, curso and ciclo, so
        // the related pickers are locked on edit.
        txtEstudianteNota.setVisibility(View.VISIBLE);
        actEstudiante.setEnabled(false);
        actCurso.setEnabled(false);
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
                            getString(R.string.nota_selecciona_estudiante), null));
                    for (EstudianteResumenResponse estudiante : response.body()) {
                        estudianteItems.add(new NamedItem<>(formatEstudiante(estudiante), estudiante));
                    }
                    bindAutoComplete(actEstudiante, estudianteItems,
                            item -> selectedEstudiante = item == null ? null : item.value);
                    applyPendingSelections();
                } else {
                    UiNotifier.error(
                            NotaFormActivity.this,
                            NotaErrorMapper.fromResponse(NotaFormActivity.this, response));
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
                            NotaFormActivity.this,
                            NotaErrorMapper.fromFailure(NotaFormActivity.this, throwable));
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
                    cursoItems.add(new NamedItem<>(getString(R.string.nota_selecciona_curso), null));
                    for (CursoResponse curso : response.body()) {
                        cursoItems.add(new NamedItem<>(formatCurso(curso), curso));
                    }
                    bindAutoComplete(actCurso, cursoItems,
                            item -> selectedCurso = item == null ? null : item.value);
                    applyPendingSelections();
                } else {
                    UiNotifier.error(
                            NotaFormActivity.this,
                            NotaErrorMapper.fromResponse(NotaFormActivity.this, response));
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
                            NotaFormActivity.this,
                            NotaErrorMapper.fromFailure(NotaFormActivity.this, throwable));
                }
            }
        });
    }

    private void loadNota() {
        setLoading(true);
        loadCall = apiService.obtenerNota(notaId);
        loadCall.enqueue(new Callback<NotaResponse>() {
            @Override
            public void onResponse(Call<NotaResponse> call, Response<NotaResponse> response) {
                loadCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    fillForm(response.body());
                    finishInitialLoadIfReady();
                    return;
                }
                setLoading(false);
                showErrorAndFinish(NotaErrorMapper.fromResponse(NotaFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<NotaResponse> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    loadCall = null;
                    setLoading(false);
                    showErrorAndFinish(NotaErrorMapper.fromFailure(NotaFormActivity.this, throwable));
                }
            }
        });
    }

    private void fillForm(NotaResponse nota) {
        pendingNota = nota;
        edtTipo.setText(nota.tipoEvaluacion);
        edtCalificacion.setText(String.valueOf(nota.calificacion));
        edtObservaciones.setText(nota.observaciones);
        applyPendingSelections();
    }

    private void saveNota() {
        if (loading || !validateForm()) {
            return;
        }
        setLoading(true);
        String tipoEvaluacion = getText(edtTipo).trim().toUpperCase(Locale.US);
        double calificacion = Double.parseDouble(getText(edtCalificacion).trim());
        String observaciones = getText(edtObservaciones).trim();
        if (isEditMode()) {
            NotaUpdateRequest request = new NotaUpdateRequest(
                    tipoEvaluacion,
                    calificacion,
                    observaciones);
            saveCall = apiService.actualizarNota(notaId, request);
        } else {
            NotaRequest request = new NotaRequest(
                    selectedEstudianteId(),
                    selectedCursoId(),
                    Calendar.getInstance().get(Calendar.YEAR),
                    tipoEvaluacion,
                    calificacion,
                    observaciones);
            saveCall = apiService.crearNota(request);
        }
        saveCall.enqueue(new Callback<NotaResponse>() {
            @Override
            public void onResponse(Call<NotaResponse> call, Response<NotaResponse> response) {
                setLoading(false);
                saveCall = null;
                if (response.isSuccessful()) {
                    UiNotifier.success(
                            NotaFormActivity.this,
                            isEditMode()
                                    ? R.string.nota_actualizada
                                    : R.string.nota_creada);
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                UiNotifier.error(
                        NotaFormActivity.this,
                        NotaErrorMapper.fromResponse(NotaFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<NotaResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                saveCall = null;
                UiNotifier.error(
                        NotaFormActivity.this,
                        NotaErrorMapper.fromFailure(NotaFormActivity.this, throwable));
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        if (!isEditMode() && selectedEstudianteId() <= 0) {
            UiNotifier.error(this, getString(R.string.nota_error_estudiante_requerido));
            valid = false;
        }
        if (!isEditMode() && selectedCursoId() <= 0) {
            UiNotifier.error(this, getString(R.string.nota_error_curso_requerido));
            valid = false;
        }

        if (getText(edtTipo).trim().isEmpty()) {
            tilTipo.setError(getString(R.string.nota_error_tipo_requerido));
            valid = false;
        } else {
            tilTipo.setError(null);
        }

        String calificacion = getText(edtCalificacion).trim();
        if (calificacion.isEmpty()) {
            tilCalificacion.setError(getString(R.string.nota_error_calificacion_requerida));
            valid = false;
        } else {
            try {
                double valor = Double.parseDouble(calificacion);
                if (valor < 0 || valor > 100) {
                    tilCalificacion.setError(getString(R.string.nota_error_calificacion_rango));
                    valid = false;
                } else {
                    tilCalificacion.setError(null);
                }
            } catch (NumberFormatException exception) {
                tilCalificacion.setError(getString(R.string.nota_error_calificacion_invalida));
                valid = false;
            }
        }
        return valid;
    }

    private void applyPendingSelections() {
        if (pendingNota == null) {
            return;
        }
        if (estudiantesLoaded) {
            selectEstudiante(pendingNota.estudianteId);
        }
        if (cursosLoaded) {
            selectCurso(pendingNota.cursoId);
        }
    }

    private void selectEstudiante(long estudianteId) {
        for (NamedItem<EstudianteResumenResponse> item : estudianteItems) {
            EstudianteResumenResponse estudiante = item.value;
            if (estudiante != null && estudiante.id == estudianteId) {
                selectedEstudiante = estudiante;
                setAutoCompleteText(actEstudiante, item.toString());
                return;
            }
        }
    }

    private void selectCurso(long cursoId) {
        for (NamedItem<CursoResponse> item : cursoItems) {
            CursoResponse curso = item.value;
            if (curso != null && curso.id == cursoId) {
                selectedCurso = curso;
                setAutoCompleteText(actCurso, item.toString());
                return;
            }
        }
    }

    private void finishInitialLoadIfReady() {
        if (estudiantesLoaded && cursosLoaded && loadCall == null && saveCall == null) {
            setLoading(false);
        }
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

    private void setLoading(boolean isLoading) {
        loading = isLoading;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!isLoading);
        tilTipo.setEnabled(!isLoading);
        tilCalificacion.setEnabled(!isLoading);
        tilObservaciones.setEnabled(!isLoading);
        actEstudiante.setEnabled(!isLoading && !isEditMode());
        actCurso.setEnabled(!isLoading && !isEditMode());
    }

    private void animateIntro() {
        notaFormHero.setAlpha(0f);
        notaFormHero.setTranslationY(20f);
        notaFormHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        notaFormPanel.setAlpha(0f);
        notaFormPanel.setTranslationY(18f);
        notaFormPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(320)
                .start();
    }

    private void showErrorAndFinish(String message) {
        UiNotifier.error(this, message);
        finish();
    }

    private long selectedEstudianteId() {
        return selectedEstudiante == null ? -1L : selectedEstudiante.id;
    }

    private long selectedCursoId() {
        return selectedCurso == null ? -1L : selectedCurso.id;
    }

    private boolean isEditMode() {
        return notaId != NEW_NOTA_ID;
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
        if (loadCall != null) {
            loadCall.cancel();
        }
        if (saveCall != null) {
            saveCall.cancel();
        }
        if (estudiantesCall != null) {
            estudiantesCall.cancel();
        }
        if (cursosCall != null) {
            cursosCall.cancel();
        }
    }
}
