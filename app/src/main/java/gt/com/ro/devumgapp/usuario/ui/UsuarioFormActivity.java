package gt.com.ro.devumgapp.usuario.ui;

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
import gt.com.ro.devumgapp.usuario.dto.UsuarioRequest;
import gt.com.ro.devumgapp.usuario.dto.UsuarioResponse;
import gt.com.ro.devumgapp.usuario.network.UsuarioApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuarioFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "usuarioId";
    private static final long NEW_USUARIO_ID = -1L;

    private MaterialToolbar toolbar;
    private TextInputLayout tilUsername;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilNombre;
    private TextInputLayout tilApellido;
    private TextInputEditText edtUsername;
    private TextInputEditText edtEmail;
    private TextInputEditText edtPassword;
    private TextInputEditText edtNombre;
    private TextInputEditText edtApellido;
    private MaterialButton btnGuardar;
    private MaterialButton btnCancelar;
    private LinearProgressIndicator progressBar;
    private TextView txtHeroTitle;
    private View usuarioFormHero;
    private View usuarioFormPanel;

    private UsuarioApiService apiService;
    private Call<UsuarioResponse> loadUsuarioCall;
    private Call<UsuarioResponse> saveCall;

    private long usuarioId = NEW_USUARIO_ID;
    private boolean editMode;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String permission = getIntent().hasExtra(EXTRA_ID) ? "USUARIOS_EDITAR" : "USUARIOS_CREAR";
        if (!Permissions.requireAll(this, Permissions.USUARIOS_LEER, permission)) return;
        setContentView(R.layout.activity_usuario_form);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        usuarioId = getIntent().getLongExtra(EXTRA_ID, NEW_USUARIO_ID);
        editMode = usuarioId != NEW_USUARIO_ID;
        apiService = RetrofitClient.getClient().create(UsuarioApiService.class);
        bindViews();
        setupToolbar();
        setupActions();
        animateIntro();
        startLoading();
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarUsuarioForm);
        tilUsername = findViewById(R.id.tilUsuarioUsername);
        tilEmail = findViewById(R.id.tilUsuarioEmail);
        tilPassword = findViewById(R.id.tilUsuarioPassword);
        tilNombre = findViewById(R.id.tilUsuarioNombre);
        tilApellido = findViewById(R.id.tilUsuarioApellido);
        edtUsername = findViewById(R.id.edtUsuarioUsername);
        edtEmail = findViewById(R.id.edtUsuarioEmail);
        edtPassword = findViewById(R.id.edtUsuarioPassword);
        edtNombre = findViewById(R.id.edtUsuarioNombre);
        edtApellido = findViewById(R.id.edtUsuarioApellido);
        btnGuardar = findViewById(R.id.btnGuardarUsuario);
        btnCancelar = findViewById(R.id.btnCancelarUsuario);
        progressBar = findViewById(R.id.progressUsuarioForm);
        txtHeroTitle = findViewById(R.id.txtUsuarioFormHeroTitle);
        usuarioFormHero = findViewById(R.id.usuarioFormHero);
        usuarioFormPanel = findViewById(R.id.cardUsuarioForm);
    }

    private void applyModeTitle() {
        int title = editMode
                ? R.string.usuario_form_titulo_editar
                : R.string.usuario_form_titulo_crear;
        toolbar.setTitle(title);
        txtHeroTitle.setText(title);
        // On edit an empty password field means "keep the stored password".
        tilPassword.setHint(editMode
                ? R.string.usuario_password_editar_hint
                : R.string.usuario_password_hint);
    }

    private void setupToolbar() {
        applyModeTitle();
        toolbar.setNavigationOnClickListener(view -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void animateIntro() {
        usuarioFormHero.setAlpha(0f);
        usuarioFormHero.setTranslationY(20f);
        usuarioFormHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        usuarioFormPanel.setAlpha(0f);
        usuarioFormPanel.setTranslationY(18f);
        usuarioFormPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(320)
                .start();
    }

    private void setupActions() {
        btnGuardar.setOnClickListener(view -> saveUsuario());
        btnCancelar.setOnClickListener(view -> finish());
        edtApellido.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveUsuario();
                return true;
            }
            return false;
        });
    }

    private void startLoading() {
        if (editMode) {
            loadUsuarioDetails();
            return;
        }
        setLoading(false);
    }

    private void loadUsuarioDetails() {
        setLoading(true);
        loadUsuarioCall = apiService.obtener(usuarioId);
        loadUsuarioCall.enqueue(new Callback<UsuarioResponse>() {
            @Override
            public void onResponse(Call<UsuarioResponse> call, Response<UsuarioResponse> response) {
                setLoading(false);
                loadUsuarioCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    fillForm(response.body());
                    return;
                }
                showErrorAndFinish(
                        UsuarioErrorMapper.fromResponse(UsuarioFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<UsuarioResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                loadUsuarioCall = null;
                showErrorAndFinish(
                        UsuarioErrorMapper.fromFailure(UsuarioFormActivity.this, throwable));
            }
        });
    }

    private void fillForm(UsuarioResponse usuario) {
        edtUsername.setText(usuario.username);
        edtEmail.setText(usuario.email);
        edtNombre.setText(usuario.nombre);
        edtApellido.setText(usuario.apellido);
        // The backend never returns the password, so the field always starts empty on edit.
        edtPassword.setText("");
    }

    private void saveUsuario() {
        if (loading || !validateForm()) {
            return;
        }

        boolean createRequest = usuarioId == NEW_USUARIO_ID;
        String password = getText(edtPassword);
        // On edit an empty password field means "do not change it": send null so Gson omits
        // the field and the backend keeps the stored password. Never send an empty string.
        String passwordToSend = createRequest || !password.trim().isEmpty()
                ? password
                : null;

        setLoading(true);
        UsuarioRequest request = new UsuarioRequest(
                getText(edtUsername).trim(),
                passwordToSend,
                getText(edtEmail).trim(),
                getText(edtNombre).trim(),
                getText(edtApellido).trim());
        saveCall = createRequest
                ? apiService.crear(request)
                : apiService.actualizar(usuarioId, request);
        saveCall.enqueue(new Callback<UsuarioResponse>() {
            @Override
            public void onResponse(Call<UsuarioResponse> call, Response<UsuarioResponse> response) {
                setLoading(false);
                saveCall = null;
                if (response.isSuccessful()) {
                    UiNotifier.success(
                            UsuarioFormActivity.this,
                            createRequest
                                    ? R.string.usuario_creado
                                    : R.string.usuario_actualizado);
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                UiNotifier.error(
                        UsuarioFormActivity.this,
                        UsuarioErrorMapper.fromResponse(UsuarioFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<UsuarioResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                saveCall = null;
                UiNotifier.error(
                        UsuarioFormActivity.this,
                        UsuarioErrorMapper.fromFailure(UsuarioFormActivity.this, throwable));
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;
        String username = getText(edtUsername).trim();
        String email = getText(edtEmail).trim();
        String password = getText(edtPassword);
        String nombre = getText(edtNombre).trim();
        String apellido = getText(edtApellido).trim();

        if (username.isEmpty()) {
            tilUsername.setError(getString(R.string.usuario_error_username_requerido));
            valid = false;
        } else if (username.length() < 3 || username.length() > 50) {
            tilUsername.setError(getString(R.string.usuario_error_username_longitud));
            valid = false;
        } else {
            tilUsername.setError(null);
        }

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.usuario_error_email_requerido));
            valid = false;
        } else if (!looksLikeEmail(email)) {
            tilEmail.setError(getString(R.string.usuario_error_email_formato));
            valid = false;
        } else {
            tilEmail.setError(null);
        }

        boolean passwordProvided = !password.trim().isEmpty();
        if (!editMode && !passwordProvided) {
            tilPassword.setError(getString(R.string.usuario_error_password_requerido));
            valid = false;
        } else if (passwordProvided && password.length() < 8) {
            tilPassword.setError(getString(R.string.usuario_error_password_longitud));
            valid = false;
        } else {
            tilPassword.setError(null);
        }

        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.usuario_error_nombre_requerido));
            valid = false;
        } else if (nombre.length() < 3 || nombre.length() > 100) {
            tilNombre.setError(getString(R.string.usuario_error_nombre_longitud));
            valid = false;
        } else {
            tilNombre.setError(null);
        }

        if (apellido.isEmpty()) {
            tilApellido.setError(getString(R.string.usuario_error_apellido_requerido));
            valid = false;
        } else if (apellido.length() < 3 || apellido.length() > 100) {
            tilApellido.setError(getString(R.string.usuario_error_apellido_longitud));
            valid = false;
        } else {
            tilApellido.setError(null);
        }

        return valid;
    }

    private boolean looksLikeEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return false;
        }
        return email.indexOf('.', atIndex + 1) > atIndex + 1;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!loading);
        btnCancelar.setEnabled(!loading);
        tilUsername.setEnabled(!loading);
        tilEmail.setEnabled(!loading);
        tilPassword.setEnabled(!loading);
        tilNombre.setEnabled(!loading);
        tilApellido.setEnabled(!loading);
    }

    private void showErrorAndFinish(String message) {
        UiNotifier.error(this, message);
        finish();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private void cancelCalls() {
        if (loadUsuarioCall != null) {
            loadUsuarioCall.cancel();
        }
        if (saveCall != null) {
            saveCall.cancel();
        }
    }
}
