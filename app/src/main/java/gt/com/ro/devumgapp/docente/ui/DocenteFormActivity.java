package gt.com.ro.devumgapp.docente.ui;

import android.os.Bundle;
import android.util.Patterns;
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
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.docente.dto.DocenteRequest;
import gt.com.ro.devumgapp.docente.dto.DocenteResponse;
import gt.com.ro.devumgapp.docente.network.DocenteApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocenteFormActivity extends AppCompatActivity {

    public static final String EXTRA_DOCENTE_ID = "docenteId";
    private static final long NEW_DOCENTE_ID = -1L;
    private static final String CODIGO_PATTERN = "^[A-Za-z0-9-]+$";
    private static final String TELEFONO_PATTERN = "^[0-9+()\\-\\s]*$";

    private TextView txtHeroTitleView;
    private View docenteFormHero;
    private View docenteFormPanel;
    private TextInputLayout tilCodigo;
    private TextInputLayout tilNombre;
    private TextInputLayout tilApellido;
    private TextInputLayout tilEmail;
    private TextInputLayout tilTelefono;
    private TextInputLayout tilEspecialidad;
    private TextInputEditText edtCodigo;
    private TextInputEditText edtNombre;
    private TextInputEditText edtApellido;
    private TextInputEditText edtEmail;
    private TextInputEditText edtTelefono;
    private TextInputEditText edtEspecialidad;
    private MaterialButton btnGuardar;
    private LinearProgressIndicator progressBar;

    private DocenteApiService docenteApiService;
    private Call<DocenteResponse> loadDocenteCall;
    private Call<DocenteResponse> saveDocenteCall;
    private long docenteId = NEW_DOCENTE_ID;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        docenteId = getIntent().getLongExtra(EXTRA_DOCENTE_ID, NEW_DOCENTE_ID);
        if (docenteId <= 0) {
            Toast.makeText(this, "Los docentes se crean desde Usuarios para vincular su cuenta y perfil.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String permission = "DOCENTES_EDITAR";
        if (!Permissions.requireAll(this, Permissions.DOCENTES_LEER, permission)) return;
        setContentView(R.layout.activity_docente_form);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        docenteApiService = RetrofitClient.getClient().create(DocenteApiService.class);
        bindViews();
        setupToolbar();
        setupActions();
        animateIntro();
        if (isEditMode()) {
            tilNombre.setHelperText("La identidad pertenece a Usuario. Cámbiala desde Mi perfil.");
            tilApellido.setHelperText("La identidad pertenece a Usuario. Cámbiala desde Mi perfil.");
            tilEmail.setHelperText("La identidad pertenece a Usuario. Cámbiala desde Mi perfil.");
            loadDocente();
        }
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarDocenteForm);
        int title = isEditMode() ? R.string.docente_form_titulo_editar : R.string.docente_form_titulo_crear;
        toolbar.setTitle(title);
        toolbar.setNavigationOnClickListener(view -> finish());
        txtHeroTitleView = findViewById(R.id.txtDocenteFormHeroTitle);
        txtHeroTitleView.setText(title);
        docenteFormHero = findViewById(R.id.docenteFormHero);
        docenteFormPanel = findViewById(R.id.docenteFormPanel);
        tilCodigo = findViewById(R.id.tilDocenteCodigo);
        tilNombre = findViewById(R.id.tilDocenteNombre);
        tilApellido = findViewById(R.id.tilDocenteApellido);
        tilEmail = findViewById(R.id.tilDocenteEmail);
        tilTelefono = findViewById(R.id.tilDocenteTelefono);
        tilEspecialidad = findViewById(R.id.tilDocenteEspecialidad);
        edtCodigo = findViewById(R.id.edtDocenteCodigo);
        edtNombre = findViewById(R.id.edtDocenteNombre);
        edtApellido = findViewById(R.id.edtDocenteApellido);
        edtEmail = findViewById(R.id.edtDocenteEmail);
        edtTelefono = findViewById(R.id.edtDocenteTelefono);
        edtEspecialidad = findViewById(R.id.edtDocenteEspecialidad);
        btnGuardar = findViewById(R.id.btnGuardarDocente);
        progressBar = findViewById(R.id.progressDocenteForm);
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
        btnGuardar.setOnClickListener(view -> saveDocente());
        edtEspecialidad.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveDocente();
                return true;
            }
            return false;
        });
    }

    private void loadDocente() {
        setLoading(true);
        loadDocenteCall = docenteApiService.obtenerDocente(docenteId);
        loadDocenteCall.enqueue(new Callback<DocenteResponse>() {
            @Override
            public void onResponse(Call<DocenteResponse> call, Response<DocenteResponse> response) {
                loadDocenteCall = null;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    DocenteResponse docente = response.body();
                    fillForm(docente);
                    return;
                }
                showErrorAndFinish(DocenteErrorMapper.fromResponse(DocenteFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<DocenteResponse> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    loadDocenteCall = null;
                    setLoading(false);
                    showErrorAndFinish(DocenteErrorMapper.fromFailure(DocenteFormActivity.this, throwable));
                }
            }
        });
    }

    private void fillForm(DocenteResponse docente) {
        edtCodigo.setText(docente.codigoDocente);
        edtNombre.setText(docente.nombre);
        edtApellido.setText(docente.apellido);
        edtEmail.setText(docente.email);
        edtTelefono.setText(docente.telefono);
        edtEspecialidad.setText(docente.especialidad);
        String source = docente.identidadFuente;
        String identityHint = "USUARIO".equalsIgnoreCase(source)
                ? "Identidad desde Usuario · cambia nombre, apellido y correo en Mi perfil"
                : "Perfil histórico sin cuenta · identidad conservada por el perfil";
        tilNombre.setHelperText(identityHint);
        tilApellido.setHelperText(identityHint);
        tilEmail.setHelperText(identityHint);
    }

    private void saveDocente() {
        if (loading || !validateForm()) {
            return;
        }
        SgauDialog.confirmSave(this, isEditMode(),
                "el docente \"" + getText(edtNombre).trim() + " " + getText(edtApellido).trim() + "\"",
                this::submitDocente);
    }

    private void submitDocente() {
        if (loading) return;
        setLoading(true);
        DocenteRequest request;
        if (isEditMode()) {
            request = new DocenteRequest();
            request.codigoDocente = getText(edtCodigo).trim();
            request.telefono = getText(edtTelefono).trim();
            request.especialidad = getText(edtEspecialidad).trim();
        } else {
            request = new DocenteRequest(
                    getText(edtCodigo).trim(), getText(edtNombre).trim(), getText(edtApellido).trim(),
                    getText(edtEmail).trim(), getText(edtTelefono).trim(), getText(edtEspecialidad).trim());
        }
        saveDocenteCall = isEditMode()
                ? docenteApiService.actualizarDocente(docenteId, request)
                : docenteApiService.crearDocente(request);
        saveDocenteCall.enqueue(new Callback<DocenteResponse>() {
            @Override
            public void onResponse(Call<DocenteResponse> call, Response<DocenteResponse> response) {
                saveDocenteCall = null;
                setLoading(false);
                if (response.isSuccessful()) {
                    finishSuccessfully();
                    return;
                }
                showToast(DocenteErrorMapper.fromResponse(DocenteFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<DocenteResponse> call, Throwable throwable) {
                if (!call.isCanceled()) {
                    saveDocenteCall = null;
                    setLoading(false);
                    showToast(DocenteErrorMapper.fromFailure(DocenteFormActivity.this, throwable));
                }
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;
        String codigo = getText(edtCodigo).trim();
        if (codigo.isEmpty()) {
            tilCodigo.setError(getString(R.string.docente_error_codigo_requerido));
            valid = false;
        } else if (codigo.length() > 20 || !codigo.matches(CODIGO_PATTERN)) {
            tilCodigo.setError(getString(R.string.docente_error_codigo_formato));
            valid = false;
        } else {
            tilCodigo.setError(null);
        }

        if (!isEditMode()) {
            if (getText(edtNombre).trim().isEmpty()) {
                tilNombre.setError(getString(R.string.docente_error_nombre_requerido));
                valid = false;
            } else tilNombre.setError(null);

            if (getText(edtApellido).trim().isEmpty()) {
                tilApellido.setError(getString(R.string.docente_error_apellido_requerido));
                valid = false;
            } else tilApellido.setError(null);

            String email = getText(edtEmail).trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError(getString(R.string.docente_error_email_formato));
                valid = false;
            } else tilEmail.setError(null);
        }

        String telefono = getText(edtTelefono).trim();
        if (!telefono.isEmpty() && (telefono.length() > 20 || !telefono.matches(TELEFONO_PATTERN))) {
            tilTelefono.setError(getString(R.string.docente_error_telefono_formato));
            valid = false;
        } else {
            tilTelefono.setError(null);
        }

        if (getText(edtEspecialidad).trim().length() > 100) {
            tilEspecialidad.setError(getString(R.string.docente_error_especialidad_longitud));
            valid = false;
        } else {
            tilEspecialidad.setError(null);
        }

        return valid;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!loading);
        tilCodigo.setEnabled(!loading);
        tilNombre.setEnabled(!loading && !isEditMode());
        tilApellido.setEnabled(!loading && !isEditMode());
        tilEmail.setEnabled(!loading && !isEditMode());
        tilTelefono.setEnabled(!loading);
        tilEspecialidad.setEnabled(!loading);
    }

    private void animateIntro() {
        docenteFormHero.setAlpha(0f);
        docenteFormHero.setTranslationY(20f);
        docenteFormHero.animate().alpha(1f).translationY(0f).setDuration(360).start();
        docenteFormPanel.setAlpha(0f);
        docenteFormPanel.setTranslationY(18f);
        docenteFormPanel.animate().alpha(1f).translationY(0f).setStartDelay(120).setDuration(320).start();
    }

    private void finishSuccessfully() {
        showSuccess(getString(isEditMode() ? R.string.docente_actualizado : R.string.docente_creado));
        setResult(RESULT_OK);
        finish();
    }

    private void showErrorAndFinish(String message) {
        showToast(message);
        finish();
    }

    private boolean isEditMode() {
        return docenteId != NEW_DOCENTE_ID;
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
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
        if (loadDocenteCall != null) {
            loadDocenteCall.cancel();
        }
        if (saveDocenteCall != null) {
            saveDocenteCall.cancel();
        }
    }
}
