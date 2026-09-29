package gt.com.ro.devumgapp.permiso.ui;

import android.os.Bundle;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.permiso.dto.PermisoRequest;
import gt.com.ro.devumgapp.permiso.dto.PermisoResponse;
import gt.com.ro.devumgapp.permiso.network.PermisoApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PermisoFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "permisoId";
    private static final long NEW_PERMISO_ID = -1L;

    private MaterialToolbar toolbar;
    private TextInputLayout tilCodigo;
    private TextInputLayout tilNombre;
    private TextInputLayout tilDescripcion;
    private TextInputEditText edtCodigo;
    private TextInputEditText edtNombre;
    private TextInputEditText edtDescripcion;
    private MaterialButton btnGuardar;
    private MaterialButton btnCancelar;
    private LinearProgressIndicator progressBar;
    private TextView txtHeroTitle;
    private View permisoFormHero;
    private View permisoFormPanel;

    private PermisoApiService apiService;
    private Call<PermisoResponse> loadCall;
    private Call<PermisoResponse> saveCall;
    private long permisoId = NEW_PERMISO_ID;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String permission = getIntent().hasExtra(EXTRA_ID) ? "PERMISOS_EDITAR" : "PERMISOS_CREAR";
        if (!Permissions.requireAll(this, Permissions.PERMISOS_LEER, permission)) return;
        setContentView(R.layout.activity_permiso_form);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        permisoId = getIntent().getLongExtra(EXTRA_ID, NEW_PERMISO_ID);
        apiService = RetrofitClient.getClient().create(PermisoApiService.class);
        bindViews();
        setupToolbar();
        setupActions();
        animateIntro();

        if (isEditMode()) {
            loadPermiso();
        }
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarPermisoForm);
        tilCodigo = findViewById(R.id.tilPermisoCodigo);
        tilNombre = findViewById(R.id.tilPermisoNombre);
        tilDescripcion = findViewById(R.id.tilPermisoDescripcion);
        edtCodigo = findViewById(R.id.edtPermisoCodigo);
        edtNombre = findViewById(R.id.edtPermisoNombre);
        edtDescripcion = findViewById(R.id.edtPermisoDescripcion);
        btnGuardar = findViewById(R.id.btnGuardarPermiso);
        btnCancelar = findViewById(R.id.btnCancelarPermiso);
        progressBar = findViewById(R.id.progressPermisoForm);
        txtHeroTitle = findViewById(R.id.txtPermisoFormHeroTitle);
        permisoFormHero = findViewById(R.id.permisoFormHero);
        permisoFormPanel = findViewById(R.id.cardPermisoForm);
    }

    private void setupToolbar() {
        int title = isEditMode()
                ? R.string.permiso_form_titulo_editar
                : R.string.permiso_form_titulo_crear;
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
        permisoFormHero.setAlpha(0f);
        permisoFormHero.setTranslationY(20f);
        permisoFormHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        permisoFormPanel.setAlpha(0f);
        permisoFormPanel.setTranslationY(18f);
        permisoFormPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(320)
                .start();
    }

    private void setupActions() {
        btnGuardar.setOnClickListener(view -> savePermiso());
        btnCancelar.setOnClickListener(view -> finish());
        edtDescripcion.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                savePermiso();
                return true;
            }
            return false;
        });
    }

    private void loadPermiso() {
        setLoading(true);
        loadCall = apiService.obtener(permisoId);
        loadCall.enqueue(new Callback<PermisoResponse>() {
            @Override
            public void onResponse(Call<PermisoResponse> call, Response<PermisoResponse> response) {
                setLoading(false);
                loadCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    fillForm(response.body());
                    return;
                }
                showErrorAndFinish(PermisoErrorMapper.fromResponse(PermisoFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<PermisoResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                loadCall = null;
                showErrorAndFinish(PermisoErrorMapper.fromFailure(PermisoFormActivity.this, throwable));
            }
        });
    }

    private void savePermiso() {
        if (loading || !validateForm()) {
            return;
        }

        SgauDialog.confirmSave(this, isEditMode(),
                "el permiso \"" + getText(edtNombre).trim() + "\"", this::submitPermiso);
    }

    private void submitPermiso() {
        if (loading) return;

        setLoading(true);
        PermisoRequest request = new PermisoRequest(
                getText(edtCodigo).trim(),
                getText(edtNombre).trim(),
                getText(edtDescripcion).trim());
        saveCall = isEditMode()
                ? apiService.actualizar(permisoId, request)
                : apiService.crear(request);
        saveCall.enqueue(new Callback<PermisoResponse>() {
            @Override
            public void onResponse(Call<PermisoResponse> call, Response<PermisoResponse> response) {
                setLoading(false);
                saveCall = null;
                if (response.isSuccessful()) {
                    UiNotifier.success(
                            PermisoFormActivity.this,
                            isEditMode()
                                    ? R.string.permiso_actualizado
                                    : R.string.permiso_creado);
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                UiNotifier.error(
                        PermisoFormActivity.this,
                        PermisoErrorMapper.fromResponse(PermisoFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<PermisoResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                saveCall = null;
                UiNotifier.error(
                        PermisoFormActivity.this,
                        PermisoErrorMapper.fromFailure(PermisoFormActivity.this, throwable));
            }
        });
    }

    private void fillForm(PermisoResponse permiso) {
        edtCodigo.setText(permiso.codigo);
        edtNombre.setText(permiso.nombre);
        edtDescripcion.setText(permiso.descripcion);
    }

    private boolean validateForm() {
        boolean valid = true;
        String codigo = getText(edtCodigo).trim();
        String nombre = getText(edtNombre).trim();
        String descripcion = getText(edtDescripcion).trim();

        if (codigo.isEmpty()) {
            tilCodigo.setError(getString(R.string.permiso_error_codigo_requerido));
            valid = false;
        } else if (codigo.length() < 3 || codigo.length() > 80) {
            tilCodigo.setError(getString(R.string.permiso_error_codigo_longitud));
            valid = false;
        } else if (!codigo.matches("[A-Za-z0-9_]+")) {
            tilCodigo.setError(getString(R.string.permiso_error_codigo_formato));
            valid = false;
        } else {
            tilCodigo.setError(null);
        }

        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.permiso_error_nombre_requerido));
            valid = false;
        } else if (nombre.length() < 3 || nombre.length() > 120) {
            tilNombre.setError(getString(R.string.permiso_error_nombre_longitud));
            valid = false;
        } else {
            tilNombre.setError(null);
        }

        if (descripcion.length() > 300) {
            tilDescripcion.setError(getString(R.string.permiso_error_descripcion_longitud));
            valid = false;
        } else {
            tilDescripcion.setError(null);
        }

        return valid;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!loading);
        btnCancelar.setEnabled(!loading);
        tilCodigo.setEnabled(!loading);
        tilNombre.setEnabled(!loading);
        tilDescripcion.setEnabled(!loading);
    }

    private void showErrorAndFinish(String message) {
        UiNotifier.error(this, message);
        finish();
    }

    private boolean isEditMode() {
        return permisoId != NEW_PERMISO_ID;
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
