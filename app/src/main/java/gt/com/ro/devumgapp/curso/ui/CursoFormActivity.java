package gt.com.ro.devumgapp.curso.ui;

import android.os.Bundle;
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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.carrera.dto.CarreraResumenResponse;
import gt.com.ro.devumgapp.carrera.network.CarreraApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.curso.dto.CursoRequest;
import gt.com.ro.devumgapp.curso.dto.CursoResponse;
import gt.com.ro.devumgapp.curso.dto.DocenteRequest;
import gt.com.ro.devumgapp.curso.dto.DocenteResumenResponse;
import gt.com.ro.devumgapp.curso.network.CursoApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CursoFormActivity extends AppCompatActivity {

    public static final String EXTRA_CURSO_ID = "cursoId";
    private static final long NEW_CURSO_ID = -1L;

    private TextView txtHeroTitle;
    private View cursoFormHero;
    private View cursoFormPanel;
    private TextInputLayout tilCodigo;
    private TextInputLayout tilNombre;
    private TextInputLayout tilDescripcion;
    private TextInputLayout tilCreditos;
    private TextInputLayout tilHoras;
    private TextInputLayout tilCiclo;
    private TextInputEditText edtCodigo;
    private TextInputEditText edtNombre;
    private TextInputEditText edtDescripcion;
    private TextInputEditText edtCreditos;
    private TextInputEditText edtHoras;
    private TextInputEditText edtCiclo;
    private Spinner spinnerCarrera;
    private Spinner spinnerDocente;
    private MaterialButton btnGuardar;
    private LinearProgressIndicator progressBar;

    private CursoApiService cursoApiService;
    private CarreraApiService carreraApiService;
    private Call<CursoResponse> loadCursoCall;
    private Call<CursoResponse> saveCursoCall;
    private Call<CursoResponse> docenteMutationCall;
    private Call<List<CarreraResumenResponse>> carrerasCall;
    private Call<List<DocenteResumenResponse>> docentesCall;
    private final List<NamedItem<CarreraResumenResponse>> carreraItems = new ArrayList<>();
    private final List<NamedItem<DocenteResumenResponse>> docenteItems = new ArrayList<>();
    private CursoResponse pendingCurso;
    private long cursoId = NEW_CURSO_ID;
    private boolean loading;
    private boolean carrerasLoaded;
    private boolean docentesLoaded;
    private boolean carrerasAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String permission = getIntent().hasExtra(EXTRA_CURSO_ID) ? "CURSOS_EDITAR" : "CURSOS_CREAR";
        if (!Permissions.requireAll(this, Permissions.CURSOS_LEER, permission)) return;
        setContentView(R.layout.activity_curso_form);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        cursoId = getIntent().getLongExtra(EXTRA_CURSO_ID, NEW_CURSO_ID);
        cursoApiService = RetrofitClient.getClient().create(CursoApiService.class);
        carreraApiService = RetrofitClient.getClient().create(CarreraApiService.class);
        bindViews();
        setupToolbar();
        setupActions();
        initializeSpinners();
        animateIntro();
        loadCatalogs();
        if (isEditMode()) {
            loadCurso();
        }
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarCursoForm);
        int title = isEditMode() ? R.string.curso_form_titulo_editar : R.string.curso_form_titulo_crear;
        toolbar.setTitle(title);
        toolbar.setNavigationOnClickListener(view -> finish());
        txtHeroTitle = findViewById(R.id.txtCursoFormHeroTitle);
        txtHeroTitle.setText(title);
        cursoFormHero = findViewById(R.id.cursoFormHero);
        cursoFormPanel = findViewById(R.id.cursoFormPanel);
        tilCodigo = findViewById(R.id.tilCursoCodigo);
        tilNombre = findViewById(R.id.tilCursoNombre);
        tilDescripcion = findViewById(R.id.tilCursoDescripcion);
        tilCreditos = findViewById(R.id.tilCursoCreditos);
        tilHoras = findViewById(R.id.tilCursoHoras);
        tilCiclo = findViewById(R.id.tilCursoCiclo);
        edtCodigo = findViewById(R.id.edtCursoCodigo);
        edtNombre = findViewById(R.id.edtCursoNombre);
        edtDescripcion = findViewById(R.id.edtCursoDescripcion);
        edtCreditos = findViewById(R.id.edtCursoCreditos);
        edtHoras = findViewById(R.id.edtCursoHoras);
        edtCiclo = findViewById(R.id.edtCursoCiclo);
        spinnerCarrera = findViewById(R.id.spinnerCursoCarrera);
        spinnerDocente = findViewById(R.id.spinnerCursoDocente);
        btnGuardar = findViewById(R.id.btnGuardarCurso);
        progressBar = findViewById(R.id.progressCursoForm);
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
        btnGuardar.setOnClickListener(view -> saveCurso());
        edtCiclo.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCurso();
                return true;
            }
            return false;
        });
    }

    private void initializeSpinners() {
        carreraItems.add(new NamedItem<>(getString(R.string.curso_selecciona_carrera), null));
        docenteItems.add(new NamedItem<>(getString(R.string.curso_sin_docente), null));
        bindSpinner(spinnerCarrera, carreraItems);
        bindSpinner(spinnerDocente, docenteItems);
    }

    private void loadCatalogs() {
        setLoading(true);
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
                    carreraItems.add(new NamedItem<>(getString(R.string.curso_selecciona_carrera), null));
                    for (CarreraResumenResponse carrera : response.body()) {
                        carreraItems.add(new NamedItem<>(formatCarrera(carrera), carrera));
                    }
                    carrerasAvailable = carreraItems.size() > 1;
                    bindSpinner(spinnerCarrera, carreraItems);
                    applyPendingSelections();
                    if (!carrerasAvailable) {
                        showToast(getString(R.string.curso_error_carreras_no_disponibles));
                    }
                } else {
                    carrerasAvailable = false;
                    showToast(CursoErrorMapper.fromResponse(CursoFormActivity.this, response));
                }
                finishInitialLoadIfReady();
            }

            @Override
            public void onFailure(Call<List<CarreraResumenResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    carrerasCall = null;
                    carrerasLoaded = true;
                    carrerasAvailable = false;
                    finishInitialLoadIfReady();
                    showToast(CursoErrorMapper.fromFailure(CursoFormActivity.this, throwable));
                }
            }
        });

        docentesCall = cursoApiService.listarDocentesActivos();
        docentesCall.enqueue(new Callback<List<DocenteResumenResponse>>() {
            @Override
            public void onResponse(
                    Call<List<DocenteResumenResponse>> call,
                    Response<List<DocenteResumenResponse>> response) {
                docentesCall = null;
                docentesLoaded = true;
                if (response.isSuccessful() && response.body() != null) {
                    docenteItems.clear();
                    docenteItems.add(new NamedItem<>(getString(R.string.curso_sin_docente), null));
                    for (DocenteResumenResponse docente : response.body()) {
                        docenteItems.add(new NamedItem<>(formatDocente(docente), docente));
                    }
                    bindSpinner(spinnerDocente, docenteItems);
                    applyPendingSelections();
                } else {
                    showToast(CursoErrorMapper.fromResponse(CursoFormActivity.this, response));
                }
                finishInitialLoadIfReady();
            }

            @Override
            public void onFailure(Call<List<DocenteResumenResponse>> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    docentesCall = null;
                    docentesLoaded = true;
                    finishInitialLoadIfReady();
                    showToast(CursoErrorMapper.fromFailure(CursoFormActivity.this, throwable));
                }
            }
        });
    }

    private void loadCurso() {
        setLoading(true);
        loadCursoCall = cursoApiService.obtenerCurso(cursoId);
        loadCursoCall.enqueue(new Callback<CursoResponse>() {
            @Override
            public void onResponse(Call<CursoResponse> call, Response<CursoResponse> response) {
                loadCursoCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    fillForm(response.body());
                    finishInitialLoadIfReady();
                    return;
                }
                setLoading(false);
                showErrorAndFinish(CursoErrorMapper.fromResponse(CursoFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<CursoResponse> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    loadCursoCall = null;
                    setLoading(false);
                    showErrorAndFinish(CursoErrorMapper.fromFailure(CursoFormActivity.this, throwable));
                }
            }
        });
    }

    private void fillForm(CursoResponse curso) {
        pendingCurso = curso;
        edtCodigo.setText(curso.codigo);
        edtNombre.setText(curso.nombre);
        edtDescripcion.setText(curso.descripcion);
        edtCreditos.setText(String.valueOf(curso.creditos));
        edtHoras.setText(String.valueOf(curso.horasSemanales));
        edtCiclo.setText(String.valueOf(curso.cicloAnio));
        applyPendingSelections();
    }

    private void saveCurso() {
        if (loading || !validateForm()) {
            return;
        }
        SgauDialog.confirmSave(this, isEditMode(),
                "el curso \"" + getText(edtNombre).trim() + "\"", this::submitCurso);
    }

    private void submitCurso() {
        if (loading) return;
        setLoading(true);
        CursoRequest request = new CursoRequest(
                getText(edtCodigo).trim(),
                getText(edtNombre).trim(),
                getText(edtDescripcion).trim(),
                Integer.parseInt(getText(edtCreditos).trim()),
                Integer.parseInt(getText(edtHoras).trim()),
                selectedCarreraId(),
                Integer.parseInt(getText(edtCiclo).trim()));
        saveCursoCall = isEditMode()
                ? cursoApiService.actualizarCurso(cursoId, request)
                : cursoApiService.crearCurso(request);
        saveCursoCall.enqueue(new Callback<CursoResponse>() {
            @Override
            public void onResponse(Call<CursoResponse> call, Response<CursoResponse> response) {
                saveCursoCall = null;
                if (response.isSuccessful()) {
                    CursoResponse saved = response.body();
                    if (saved != null && shouldSyncDocente()) {
                        syncDocente(saved.id);
                        return;
                    }
                    setLoading(false);
                    finishSuccessfully();
                    return;
                }
                setLoading(false);
                showToast(CursoErrorMapper.fromResponse(CursoFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<CursoResponse> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    saveCursoCall = null;
                    setLoading(false);
                    showToast(CursoErrorMapper.fromFailure(CursoFormActivity.this, throwable));
                }
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;
        if (getText(edtCodigo).trim().isEmpty()) {
            tilCodigo.setError(getString(R.string.curso_error_codigo_requerido));
            valid = false;
        } else {
            tilCodigo.setError(null);
        }
        if (getText(edtNombre).trim().isEmpty()) {
            tilNombre.setError(getString(R.string.curso_error_nombre_requerido));
            valid = false;
        } else {
            tilNombre.setError(null);
        }
        if (getText(edtDescripcion).trim().length() > 500) {
            tilDescripcion.setError(getString(R.string.curso_error_descripcion_longitud));
            valid = false;
        } else {
            tilDescripcion.setError(null);
        }
        valid = validateNumber(tilCreditos, edtCreditos, 1, 20, R.string.curso_error_creditos_rango) && valid;
        valid = validateNumber(tilHoras, edtHoras, 1, 40, R.string.curso_error_horas_rango) && valid;
        valid = validateNumber(tilCiclo, edtCiclo, 2020, 2100, R.string.curso_error_ciclo_rango) && valid;
        if (!carrerasAvailable) {
            showToast(getString(R.string.curso_error_carreras_no_disponibles));
            valid = false;
        }
        if (selectedCarreraId() <= 0) {
            showToast(getString(R.string.curso_error_carrera_requerida));
            valid = false;
        }
        return valid;
    }

    private boolean validateNumber(TextInputLayout layout, TextInputEditText field, int min, int max, int errorRes) {
        Integer value = parseIntOrNull(getText(field).trim());
        if (value == null || value < min || value > max) {
            layout.setError(getString(errorRes));
            return false;
        }
        layout.setError(null);
        return true;
    }

    private boolean shouldSyncDocente() {
        Long selected = selectedDocenteId();
        Long current = pendingCurso == null ? null : pendingCurso.docenteId;
        if (selected == null) {
            return current != null;
        }
        return !selected.equals(current);
    }

    private void syncDocente(long savedCursoId) {
        Long docenteId = selectedDocenteId();
        docenteMutationCall = docenteId == null
                ? cursoApiService.quitarDocente(savedCursoId)
                : cursoApiService.asignarDocente(savedCursoId, new DocenteRequest(docenteId));
        docenteMutationCall.enqueue(new Callback<CursoResponse>() {
            @Override
            public void onResponse(Call<CursoResponse> call, Response<CursoResponse> response) {
                docenteMutationCall = null;
                setLoading(false);
                if (response.isSuccessful()) {
                    finishSuccessfully();
                    return;
                }
                showToast(CursoErrorMapper.fromResponse(CursoFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<CursoResponse> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    docenteMutationCall = null;
                    setLoading(false);
                    showToast(CursoErrorMapper.fromFailure(CursoFormActivity.this, throwable));
                }
            }
        });
    }

    private void applyPendingSelections() {
        if (pendingCurso == null) {
            return;
        }
        if (carrerasLoaded) {
            selectCarrera(pendingCurso.carreraId);
        }
        if (docentesLoaded && pendingCurso.docenteId != null) {
            selectDocente(pendingCurso.docenteId);
        }
    }

    private void selectCarrera(long carreraId) {
        for (int index = 0; index < carreraItems.size(); index++) {
            CarreraResumenResponse carrera = carreraItems.get(index).value;
            if (carrera != null && carrera.id == carreraId) {
                spinnerCarrera.setSelection(index);
                return;
            }
        }
    }

    private void selectDocente(long docenteId) {
        for (int index = 0; index < docenteItems.size(); index++) {
            DocenteResumenResponse docente = docenteItems.get(index).value;
            if (docente != null && docente.id == docenteId) {
                spinnerDocente.setSelection(index);
                return;
            }
        }
    }

    private void finishInitialLoadIfReady() {
        if (carrerasLoaded && docentesLoaded && loadCursoCall == null && saveCursoCall == null) {
            setLoading(false);
        }
    }

    private <T> void bindSpinner(Spinner spinner, List<NamedItem<T>> items) {
        ArrayAdapter<NamedItem<T>> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_selected,
                items);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinner.setAdapter(adapter);
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!loading && (!carrerasLoaded || carrerasAvailable));
        tilCodigo.setEnabled(!loading);
        tilNombre.setEnabled(!loading);
        tilDescripcion.setEnabled(!loading);
        tilCreditos.setEnabled(!loading);
        tilHoras.setEnabled(!loading);
        tilCiclo.setEnabled(!loading);
        spinnerCarrera.setEnabled(!loading);
        spinnerDocente.setEnabled(!loading);
    }

    private void animateIntro() {
        cursoFormHero.setAlpha(0f);
        cursoFormHero.setTranslationY(20f);
        cursoFormHero.animate().alpha(1f).translationY(0f).setDuration(360).start();
        cursoFormPanel.setAlpha(0f);
        cursoFormPanel.setTranslationY(18f);
        cursoFormPanel.animate().alpha(1f).translationY(0f).setStartDelay(120).setDuration(320).start();
    }

    private void finishSuccessfully() {
        showSuccess(getString(isEditMode() ? R.string.curso_actualizado : R.string.curso_creado));
        setResult(RESULT_OK);
        finish();
    }

    private void showErrorAndFinish(String message) {
        showToast(message);
        finish();
    }

    private long selectedCarreraId() {
        NamedItem<CarreraResumenResponse> item = selectedItem(spinnerCarrera);
        return item == null || item.value == null ? -1L : item.value.id;
    }

    private Long selectedDocenteId() {
        NamedItem<DocenteResumenResponse> item = selectedItem(spinnerDocente);
        return item == null || item.value == null ? null : item.value.id;
    }

    @SuppressWarnings("unchecked")
    private <T> NamedItem<T> selectedItem(Spinner spinner) {
        return (NamedItem<T>) spinner.getSelectedItem();
    }

    private String formatCarrera(CarreraResumenResponse carrera) {
        return nullToEmpty(carrera.codigo) + " - " + nullToEmpty(carrera.nombre);
    }

    private String formatDocente(DocenteResumenResponse docente) {
        return nullToEmpty(docente.codigoDocente) + " - "
                + nullToEmpty(docente.nombre) + " " + nullToEmpty(docente.apellido);
    }

    private Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isEditMode() {
        return cursoId != NEW_CURSO_ID;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void showToast(String message) {
        showError(message);
    }

    private void showSuccess(String message) {
        UiNotifier.success(this, message);
    }

    private void showError(String message) {
        UiNotifier.error(this, message);
    }

    private void cancelCalls() {
        if (loadCursoCall != null) {
            loadCursoCall.cancel();
        }
        if (saveCursoCall != null) {
            saveCursoCall.cancel();
        }
        if (docenteMutationCall != null) {
            docenteMutationCall.cancel();
        }
        if (carrerasCall != null) {
            carrerasCall.cancel();
        }
        if (docentesCall != null) {
            docentesCall.cancel();
        }
    }
}
