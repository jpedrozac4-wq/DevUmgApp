package gt.com.ro.devumgapp.carrera.ui;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.carrera.dto.CarreraRequest;
import gt.com.ro.devumgapp.carrera.dto.CarreraResponse;
import gt.com.ro.devumgapp.carrera.network.CarreraApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CarreraFormActivity extends AppCompatActivity {

    public static final String EXTRA_CARRERA_ID = "carreraId";
    private static final long NEW_CARRERA_ID = -1L;

    private MaterialToolbar toolbar;
    private TextInputLayout tilCodigo;
    private TextInputLayout tilNombre;
    private TextInputLayout tilDescripcion;
    private TextInputLayout tilDuracion;
    private TextInputEditText edtCodigo;
    private TextInputEditText edtNombre;
    private TextInputEditText edtDescripcion;
    private TextInputEditText edtDuracion;
    private MaterialButton btnGuardar;
    private LinearProgressIndicator progressBar;
    private TextView txtHeroTitle;
    private View carreraFormHero;
    private View carreraFormPanel;

    private CarreraApiService apiService;
    private Call<CarreraResponse> loadCall;
    private Call<CarreraResponse> saveCall;
    private long carreraId = NEW_CARRERA_ID;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrera_form);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        carreraId = getIntent().getLongExtra(EXTRA_CARRERA_ID, NEW_CARRERA_ID);
        apiService = RetrofitClient.getClient().create(CarreraApiService.class);
        bindViews();
        setupToolbar();
        setupActions();
        animateIntro();

        if (isEditMode()) {
            loadCarrera();
        }
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarCarreraForm);
        tilCodigo = findViewById(R.id.tilCarreraCodigo);
        tilNombre = findViewById(R.id.tilCarreraNombre);
        tilDescripcion = findViewById(R.id.tilCarreraDescripcion);
        tilDuracion = findViewById(R.id.tilCarreraDuracion);
        edtCodigo = findViewById(R.id.edtCarreraCodigo);
        edtNombre = findViewById(R.id.edtCarreraNombre);
        edtDescripcion = findViewById(R.id.edtCarreraDescripcion);
        edtDuracion = findViewById(R.id.edtCarreraDuracion);
        btnGuardar = findViewById(R.id.btnGuardarCarrera);
        progressBar = findViewById(R.id.progressCarreraForm);
        txtHeroTitle = findViewById(R.id.txtCarreraFormHeroTitle);
        carreraFormHero = findViewById(R.id.carreraFormHero);
        carreraFormPanel = findViewById(R.id.carreraFormPanel);
    }

    private void setupToolbar() {
        int title = isEditMode()
                ? R.string.carrera_form_titulo_editar
                : R.string.carrera_form_titulo_crear;
        toolbar.setTitle(title);
        txtHeroTitle.setText(title);
        toolbar.setNavigationOnClickListener(view -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void animateIntro() {
        carreraFormHero.setAlpha(0f);
        carreraFormHero.setTranslationY(20f);
        carreraFormHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        carreraFormPanel.setAlpha(0f);
        carreraFormPanel.setTranslationY(18f);
        carreraFormPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(320)
                .start();
    }

    private void setupActions() {
        btnGuardar.setOnClickListener(view -> saveCarrera());
        edtDuracion.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCarrera();
                return true;
            }
            return false;
        });
    }

    private void loadCarrera() {
        setLoading(true);
        loadCall = apiService.obtenerCarrera(carreraId);
        loadCall.enqueue(new Callback<CarreraResponse>() {
            @Override
            public void onResponse(Call<CarreraResponse> call, Response<CarreraResponse> response) {
                setLoading(false);
                loadCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    fillForm(response.body());
                    return;
                }
                showErrorAndFinish(CarreraErrorMapper.fromResponse(CarreraFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<CarreraResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                loadCall = null;
                showErrorAndFinish(CarreraErrorMapper.fromFailure(CarreraFormActivity.this, throwable));
            }
        });
    }

    private void saveCarrera() {
        if (loading || !validateForm()) {
            return;
        }

        setLoading(true);
        CarreraRequest request = new CarreraRequest(
                getText(edtCodigo).trim(),
                getText(edtNombre).trim(),
                getText(edtDescripcion).trim(),
                Integer.parseInt(getText(edtDuracion).trim()));
        saveCall = isEditMode()
                ? apiService.actualizarCarrera(carreraId, request)
                : apiService.crearCarrera(request);
        saveCall.enqueue(new Callback<CarreraResponse>() {
            @Override
            public void onResponse(Call<CarreraResponse> call, Response<CarreraResponse> response) {
                setLoading(false);
                saveCall = null;
                if (response.isSuccessful()) {
                    Toast.makeText(CarreraFormActivity.this,
                            isEditMode()
                                    ? R.string.carrera_actualizada
                                    : R.string.carrera_creada,
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                Toast.makeText(CarreraFormActivity.this,
                        CarreraErrorMapper.fromResponse(CarreraFormActivity.this, response),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<CarreraResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                saveCall = null;
                Toast.makeText(CarreraFormActivity.this,
                        CarreraErrorMapper.fromFailure(CarreraFormActivity.this, throwable),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fillForm(CarreraResponse carrera) {
        edtCodigo.setText(carrera.codigo);
        edtNombre.setText(carrera.nombre);
        edtDescripcion.setText(carrera.descripcion);
        edtDuracion.setText(String.valueOf(carrera.duracionAnios));
    }

    private boolean validateForm() {
        boolean valid = true;
        String codigo = getText(edtCodigo).trim();
        String nombre = getText(edtNombre).trim();
        String descripcion = getText(edtDescripcion).trim();
        String duracionText = getText(edtDuracion).trim();

        if (codigo.isEmpty()) {
            tilCodigo.setError(getString(R.string.carrera_error_codigo_requerido));
            valid = false;
        } else if (codigo.length() < 2 || codigo.length() > 20) {
            tilCodigo.setError(getString(R.string.carrera_error_codigo_longitud));
            valid = false;
        } else if (!codigo.matches("[A-Za-z0-9-]+")) {
            tilCodigo.setError(getString(R.string.carrera_error_codigo_formato));
            valid = false;
        } else {
            tilCodigo.setError(null);
        }

        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.carrera_error_nombre_requerido));
            valid = false;
        } else if (nombre.length() < 3 || nombre.length() > 120) {
            tilNombre.setError(getString(R.string.carrera_error_nombre_longitud));
            valid = false;
        } else {
            tilNombre.setError(null);
        }

        if (descripcion.length() > 500) {
            tilDescripcion.setError(getString(R.string.carrera_error_descripcion_longitud));
            valid = false;
        } else {
            tilDescripcion.setError(null);
        }

        Integer duracion = parseIntOrNull(duracionText);
        if (duracion == null || duracion < 1 || duracion > 10) {
            tilDuracion.setError(getString(R.string.carrera_error_duracion_rango));
            valid = false;
        } else {
            tilDuracion.setError(null);
        }

        return valid;
    }

    private Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!loading);
        tilCodigo.setEnabled(!loading);
        tilNombre.setEnabled(!loading);
        tilDescripcion.setEnabled(!loading);
        tilDuracion.setEnabled(!loading);
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private boolean isEditMode() {
        return carreraId != NEW_CARRERA_ID;
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
    }
}
