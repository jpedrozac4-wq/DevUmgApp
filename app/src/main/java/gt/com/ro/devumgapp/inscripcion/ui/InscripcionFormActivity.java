package gt.com.ro.devumgapp.inscripcion.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.carrera.dto.CarreraResumenResponse;
import gt.com.ro.devumgapp.carrera.network.CarreraApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.curso.dto.CursoResponse;
import gt.com.ro.devumgapp.curso.network.CursoApiService;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import gt.com.ro.devumgapp.inscripcion.dto.InscripcionRequest;
import gt.com.ro.devumgapp.inscripcion.dto.InscripcionResponse;
import gt.com.ro.devumgapp.inscripcion.dto.InscripcionUpdateRequest;
import gt.com.ro.devumgapp.inscripcion.network.InscripcionApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InscripcionFormActivity extends AppCompatActivity {

    public static final String EXTRA_INSCRIPCION_ID = "inscripcionId";
    private static final long NEW_INSCRIPCION_ID = -1L;

    private MaterialToolbar toolbar;
    private TextView txtHeroTitle;
    private View inscripcionFormHero;
    private View inscripcionFormPanel;
    private TextView txtEstudianteNota;
    private TextInputLayout tilGrado;
    private TextInputLayout tilSeccion;
    private TextInputLayout tilFecha;
    private TextInputLayout tilObservaciones;
    private TextInputEditText edtGrado;
    private TextInputEditText edtSeccion;
    private TextInputEditText edtFecha;
    private TextInputEditText edtObservaciones;
    private MaterialAutoCompleteTextView actEstudiante;
    private MaterialAutoCompleteTextView actCarrera;
    private MaterialAutoCompleteTextView actCurso;
    private Spinner spinnerCiclo;
    private EstudianteResumenResponse selectedEstudiante;
    private CarreraResumenResponse selectedCarrera;
    private CursoResponse selectedCurso;
    private final Map<MaterialAutoCompleteTextView, TextWatcher> autoCompleteWatchers = new HashMap<>();
    private final Map<MaterialAutoCompleteTextView, String[]> autoCompletePickedLabels = new HashMap<>();
    private MaterialButton btnGuardar;
    private LinearProgressIndicator progressBar;

    private InscripcionApiService apiService;
    private EstudianteApiService estudianteApiService;
    private CarreraApiService carreraApiService;
    private CursoApiService cursoApiService;
    private Call<InscripcionResponse> loadCall;
    private Call<InscripcionResponse> saveCall;
    private Call<List<EstudianteResumenResponse>> estudiantesCall;
    private Call<List<CarreraResumenResponse>> carrerasCall;
    private Call<List<CursoResponse>> cursosCall;
    private final List<NamedItem<EstudianteResumenResponse>> estudianteItems = new ArrayList<>();
    private final List<NamedItem<CarreraResumenResponse>> carreraItems = new ArrayList<>();
    private final List<NamedItem<CursoResponse>> cursoItems = new ArrayList<>();
    private final List<NamedItem<Integer>> cicloItems = new ArrayList<>();
    private InscripcionResponse pendingInscripcion;
    private long inscripcionId = NEW_INSCRIPCION_ID;
    private boolean loading;
    private boolean estudiantesLoaded;
    private boolean carrerasLoaded;
    private boolean cursosLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String permission = getIntent().hasExtra(EXTRA_INSCRIPCION_ID) ? "INSCRIPCIONES_EDITAR" : "INSCRIPCIONES_CREAR";
        if (!Permissions.requireAll(this, Permissions.INSCRIPCIONES_LEER, permission)) return;
        setContentView(R.layout.activity_inscripcion_form);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        inscripcionId = getIntent().getLongExtra(EXTRA_INSCRIPCION_ID, NEW_INSCRIPCION_ID);
        apiService = RetrofitClient.getClient().create(InscripcionApiService.class);
        estudianteApiService = RetrofitClient.getClient().create(EstudianteApiService.class);
        carreraApiService = RetrofitClient.getClient().create(CarreraApiService.class);
        cursoApiService = RetrofitClient.getClient().create(CursoApiService.class);
        bindViews();
        setupToolbar();
        setupActions();
        initializeSpinners();
        configureModeSpecificUi();
        animateIntro();
        loadCatalogs();
        if (isEditMode()) {
            loadInscripcion();
        }
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarInscripcionForm);
        int title = isEditMode()
                ? R.string.inscripcion_form_titulo_editar
                : R.string.inscripcion_form_titulo_crear;
        toolbar.setTitle(title);
        toolbar.setNavigationOnClickListener(view -> finish());
        txtHeroTitle = findViewById(R.id.txtInscripcionFormHeroTitle);
        txtHeroTitle.setText(title);
        inscripcionFormHero = findViewById(R.id.inscripcionFormHero);
        inscripcionFormPanel = findViewById(R.id.inscripcionFormPanel);
        txtEstudianteNota = findViewById(R.id.txtInscripcionEstudianteNota);
        tilGrado = findViewById(R.id.tilInscripcionGrado);
        tilSeccion = findViewById(R.id.tilInscripcionSeccion);
        tilFecha = findViewById(R.id.tilInscripcionFecha);
        tilObservaciones = findViewById(R.id.tilInscripcionObservaciones);
        edtGrado = findViewById(R.id.edtInscripcionGrado);
        edtSeccion = findViewById(R.id.edtInscripcionSeccion);
        edtFecha = findViewById(R.id.edtInscripcionFecha);
        edtObservaciones = findViewById(R.id.edtInscripcionObservaciones);
        actEstudiante = findViewById(R.id.actInscripcionEstudiante);
        actCarrera = findViewById(R.id.actInscripcionCarrera);
        actCurso = findViewById(R.id.actInscripcionCurso);
        spinnerCiclo = findViewById(R.id.spinnerInscripcionCiclo);
        btnGuardar = findViewById(R.id.btnGuardarInscripcion);
        progressBar = findViewById(R.id.progressInscripcionForm);
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
        btnGuardar.setOnClickListener(view -> saveInscripcion());
        edtFecha.setOnClickListener(view -> openFechaPicker());
        tilFecha.setOnClickListener(view -> openFechaPicker());
        edtObservaciones.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveInscripcion();
                return true;
            }
            return false;
        });
    }

    private void initializeSpinners() {
        estudianteItems.add(new NamedItem<>(getString(R.string.inscripcion_selecciona_estudiante), null));
        carreraItems.add(new NamedItem<>(getString(R.string.inscripcion_selecciona_carrera), null));
        cursoItems.add(new NamedItem<>(getString(R.string.inscripcion_sin_curso), null));
        int year = Calendar.getInstance().get(Calendar.YEAR);
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
        selectCiclo(Calendar.getInstance().get(Calendar.YEAR));
    }

    private void configureModeSpecificUi() {
        if (!isEditMode()) {
            edtFecha.setText(todayUtcDate());
            return;
        }
        // The backend update contract excludes the student id, so it is locked on edit.
        tilFecha.setVisibility(View.GONE);
        txtEstudianteNota.setVisibility(View.VISIBLE);
        actEstudiante.setEnabled(false);
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
                            getString(R.string.inscripcion_selecciona_estudiante), null));
                    for (EstudianteResumenResponse estudiante : response.body()) {
                        estudianteItems.add(new NamedItem<>(formatEstudiante(estudiante), estudiante));
                    }
                    bindAutoComplete(actEstudiante, estudianteItems,
                            item -> selectedEstudiante = item == null ? null : item.value);
                    applyPendingSelections();
                } else {
                    UiNotifier.error(
                            InscripcionFormActivity.this,
                            InscripcionErrorMapper.fromResponse(InscripcionFormActivity.this, response));
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
                            InscripcionFormActivity.this,
                            InscripcionErrorMapper.fromFailure(InscripcionFormActivity.this, throwable));
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
                            getString(R.string.inscripcion_selecciona_carrera), null));
                    for (CarreraResumenResponse carrera : response.body()) {
                        carreraItems.add(new NamedItem<>(formatCarrera(carrera), carrera));
                    }
                    bindAutoComplete(actCarrera, carreraItems,
                            item -> selectedCarrera = item == null ? null : item.value);
                    applyPendingSelections();
                } else {
                    UiNotifier.error(
                            InscripcionFormActivity.this,
                            InscripcionErrorMapper.fromResponse(InscripcionFormActivity.this, response));
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
                            InscripcionFormActivity.this,
                            InscripcionErrorMapper.fromFailure(InscripcionFormActivity.this, throwable));
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
                    cursoItems.add(new NamedItem<>(getString(R.string.inscripcion_sin_curso), null));
                    for (CursoResponse curso : response.body()) {
                        cursoItems.add(new NamedItem<>(formatCurso(curso), curso));
                    }
                    bindAutoComplete(actCurso, cursoItems,
                            item -> selectedCurso = item == null ? null : item.value);
                    applyPendingSelections();
                } else {
                    UiNotifier.error(
                            InscripcionFormActivity.this,
                            InscripcionErrorMapper.fromResponse(InscripcionFormActivity.this, response));
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
                            InscripcionFormActivity.this,
                            InscripcionErrorMapper.fromFailure(InscripcionFormActivity.this, throwable));
                }
            }
        });
    }

    private void loadInscripcion() {
        setLoading(true);
        loadCall = apiService.obtenerInscripcion(inscripcionId);
        loadCall.enqueue(new Callback<InscripcionResponse>() {
            @Override
            public void onResponse(Call<InscripcionResponse> call, Response<InscripcionResponse> response) {
                loadCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    fillForm(response.body());
                    finishInitialLoadIfReady();
                    return;
                }
                setLoading(false);
                showErrorAndFinish(InscripcionErrorMapper.fromResponse(InscripcionFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<InscripcionResponse> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    loadCall = null;
                    setLoading(false);
                    showErrorAndFinish(InscripcionErrorMapper.fromFailure(InscripcionFormActivity.this, throwable));
                }
            }
        });
    }

    private void fillForm(InscripcionResponse inscripcion) {
        pendingInscripcion = inscripcion;
        edtGrado.setText(inscripcion.grado);
        edtSeccion.setText(inscripcion.seccion);
        edtObservaciones.setText(inscripcion.observaciones);
        selectCiclo(inscripcion.cicloAnio);
        applyPendingSelections();
    }

    private void saveInscripcion() {
        if (loading || !validateForm()) {
            return;
        }
        SgauDialog.confirmSave(this, isEditMode(), "la inscripción seleccionada", this::submitInscripcion);
    }

    private void submitInscripcion() {
        if (loading) return;
        setLoading(true);
        if (isEditMode()) {
            InscripcionUpdateRequest request = new InscripcionUpdateRequest(
                    selectedCarreraId(),
                    selectedCursoId(),
                    getText(edtGrado).trim().toUpperCase(Locale.US),
                    getText(edtSeccion).trim(),
                    selectedCiclo(),
                    getText(edtObservaciones).trim());
            saveCall = apiService.actualizarInscripcion(inscripcionId, request);
        } else {
            InscripcionRequest request = new InscripcionRequest(
                    selectedEstudianteId(),
                    selectedCarreraId(),
                    selectedCursoId(),
                    getText(edtGrado).trim().toUpperCase(Locale.US),
                    getText(edtSeccion).trim(),
                    selectedCiclo(),
                    getText(edtFecha).trim(),
                    getText(edtObservaciones).trim());
            saveCall = apiService.crearInscripcion(request);
        }
        saveCall.enqueue(new Callback<InscripcionResponse>() {
            @Override
            public void onResponse(Call<InscripcionResponse> call, Response<InscripcionResponse> response) {
                setLoading(false);
                saveCall = null;
                if (response.isSuccessful()) {
                    UiNotifier.success(
                            InscripcionFormActivity.this,
                            isEditMode()
                                    ? R.string.inscripcion_actualizada
                                    : R.string.inscripcion_creada);
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                UiNotifier.error(
                        InscripcionFormActivity.this,
                        InscripcionErrorMapper.fromResponse(InscripcionFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<InscripcionResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                saveCall = null;
                UiNotifier.error(
                        InscripcionFormActivity.this,
                        InscripcionErrorMapper.fromFailure(InscripcionFormActivity.this, throwable));
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        if (!isEditMode() && selectedEstudianteId() <= 0) {
            UiNotifier.error(this, getString(R.string.inscripcion_error_estudiante_requerido));
            valid = false;
        }
        if (selectedCarreraId() <= 0) {
            UiNotifier.error(this, getString(R.string.inscripcion_error_carrera_requerida));
            valid = false;
        }

        String grado = getText(edtGrado).trim();
        if (grado.isEmpty()) {
            tilGrado.setError(getString(R.string.inscripcion_error_grado_requerido));
            valid = false;
        } else {
            tilGrado.setError(null);
        }

        if (getText(edtSeccion).trim().isEmpty()) {
            tilSeccion.setError(getString(R.string.inscripcion_error_seccion_requerida));
            valid = false;
        } else {
            tilSeccion.setError(null);
        }

        if (!isEditMode()) {
            String fecha = getText(edtFecha).trim();
            if (fecha.isEmpty()) {
                tilFecha.setError(getString(R.string.inscripcion_error_fecha_requerida));
                valid = false;
            } else if (!fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
                tilFecha.setError(getString(R.string.inscripcion_error_fecha_invalida));
                valid = false;
            } else {
                tilFecha.setError(null);
            }
        }
        return valid;
    }

    private void applyPendingSelections() {
        if (pendingInscripcion == null) {
            return;
        }
        if (estudiantesLoaded) {
            selectEstudiante(pendingInscripcion.estudianteId);
        }
        if (carrerasLoaded) {
            selectCarrera(pendingInscripcion.carreraId);
        }
        if (cursosLoaded) {
            selectCurso(pendingInscripcion.cursoId);
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

    private void selectCarrera(long carreraId) {
        for (NamedItem<CarreraResumenResponse> item : carreraItems) {
            CarreraResumenResponse carrera = item.value;
            if (carrera != null && carrera.id == carreraId) {
                selectedCarrera = carrera;
                setAutoCompleteText(actCarrera, item.toString());
                return;
            }
        }
    }

    private void selectCurso(Long cursoId) {
        for (NamedItem<CursoResponse> item : cursoItems) {
            CursoResponse curso = item.value;
            if (cursoId != null && curso != null && curso.id == cursoId) {
                selectedCurso = curso;
                setAutoCompleteText(actCurso, item.toString());
                return;
            }
        }
    }

    private void selectCiclo(int cicloAnio) {
        for (int index = 0; index < cicloItems.size(); index++) {
            Integer itemYear = cicloItems.get(index).value;
            if (itemYear != null && itemYear == cicloAnio) {
                spinnerCiclo.setSelection(index);
                return;
            }
        }
    }

    private void finishInitialLoadIfReady() {
        if (estudiantesLoaded && carrerasLoaded && cursosLoaded && loadCall == null && saveCall == null) {
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

    private <T> void bindSpinner(Spinner spinner, List<NamedItem<T>> items) {
        ArrayAdapter<NamedItem<T>> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                items);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinner.setAdapter(adapter);
    }

    private void setLoading(boolean isLoading) {
        loading = isLoading;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!isLoading);
        tilGrado.setEnabled(!isLoading);
        tilSeccion.setEnabled(!isLoading);
        tilFecha.setEnabled(!isLoading);
        tilObservaciones.setEnabled(!isLoading);
        actEstudiante.setEnabled(!isLoading && !isEditMode());
        actCarrera.setEnabled(!isLoading);
        actCurso.setEnabled(!isLoading);
        spinnerCiclo.setEnabled(!isLoading);
    }

    private void animateIntro() {
        inscripcionFormHero.setAlpha(0f);
        inscripcionFormHero.setTranslationY(20f);
        inscripcionFormHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        inscripcionFormPanel.setAlpha(0f);
        inscripcionFormPanel.setTranslationY(18f);
        inscripcionFormPanel.animate()
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

    private long selectedCarreraId() {
        return selectedCarrera == null ? -1L : selectedCarrera.id;
    }

    private Long selectedCursoId() {
        return selectedCurso == null ? null : selectedCurso.id;
    }

    private int selectedCiclo() {
        NamedItem<Integer> item = selectedItem(spinnerCiclo);
        return item == null || item.value == null ? 0 : item.value;
    }

    private void openFechaPicker() {
        if (isEditMode()) {
            return;
        }
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.inscripcion_form_fecha_hint)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> edtFecha.setText(formatUtcDate(selection)));
        picker.show(getSupportFragmentManager(), "inscripcion_fecha_picker");
    }

    private String formatUtcDate(long utcMillis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(utcMillis));
    }

    private String todayUtcDate() {
        return formatUtcDate(MaterialDatePicker.todayInUtcMilliseconds());
    }

    @SuppressWarnings("unchecked")
    private <T> NamedItem<T> selectedItem(Spinner spinner) {
        return (NamedItem<T>) spinner.getSelectedItem();
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

    private boolean isEditMode() {
        return inscripcionId != NEW_INSCRIPCION_ID;
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
        if (carrerasCall != null) {
            carrerasCall.cancel();
        }
        if (cursosCall != null) {
            cursosCall.cancel();
        }
    }
}
