package gt.com.ro.devumgapp.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.IOException;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.auth.dto.ApiError;
import gt.com.ro.devumgapp.auth.dto.LoginRequest;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import gt.com.ro.devumgapp.auth.network.AuthApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsuario;
    private EditText edtPassword;
    private Button btnLogin;
    private ImageButton btnShowPassword;
    private ProgressBar progressBar;
    private CheckBox chkRecordar;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Forzar colores de barra de estado para modo oscuro/azul
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_blue_darkest, getTheme()));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.bg_blue_darkest, getTheme()));

        // Sesión activa: ir directo a Home
        if (SessionManager.getInstance().isLoggedIn()) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        edtUsuario = findViewById(R.id.edtUsuario);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnShowPassword = findViewById(R.id.btnShowPassword);
        progressBar = findViewById(R.id.progressBar);
        chkRecordar = findViewById(R.id.chkRecordar);

        // Usuario recordado: precargar el campo (nunca la contraseña)
        String rememberedUsername = SessionManager.getInstance().getRememberedUsername();
        if (!rememberedUsername.isEmpty()) {
            edtUsuario.setText(rememberedUsername);
            chkRecordar.setChecked(true);
            edtPassword.requestFocus();
        }

        btnShowPassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Ocultar contraseña
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnShowPassword.setImageResource(R.drawable.ic_visibility);
            } else {
                // Mostrar contraseña
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnShowPassword.setImageResource(R.drawable.ic_visibility_off);
            }
            isPasswordVisible = !isPasswordVisible;
            edtPassword.setSelection(edtPassword.getText().length());
        });

        // Login contra el backend REST
        btnLogin.setOnClickListener(v -> {
            String user = edtUsuario.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (user.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);

            AuthApiService service = RetrofitClient.getClient().create(AuthApiService.class);
            service.login(new LoginRequest(user, password)).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        if (chkRecordar.isChecked()) {
                            SessionManager.getInstance().rememberUsername(user);
                        } else {
                            SessionManager.getInstance().clearRememberedUsername();
                        }
                        SessionManager.getInstance().saveSession(response.body());
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, resolveErrorMessage(response), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /** Shows/hides the loading indicator and toggles the login button. */
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    /** Prefers the backend message; falls back to a mapping by HTTP status. */
    private String resolveErrorMessage(Response<LoginResponse> response) {
        ApiError apiError = null;
        try {
            if (response.errorBody() != null) {
                apiError = new Gson().fromJson(response.errorBody().string(), ApiError.class);
            }
        } catch (IOException e) {
            // Unreadable body: fall through to the status-code mapping.
        }
        if (apiError != null && apiError.message != null && !apiError.message.isEmpty()) {
            return apiError.message;
        }
        switch (response.code()) {
            case 401:
                return getString(R.string.error_invalid_credentials);
            case 400:
                return getString(R.string.error_invalid_data);
            default:
                return getString(R.string.error_server);
        }
    }
}
