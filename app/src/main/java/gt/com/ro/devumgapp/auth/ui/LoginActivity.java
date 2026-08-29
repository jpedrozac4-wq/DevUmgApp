package gt.com.ro.devumgapp.auth.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.auth.dto.LoginRequest;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import gt.com.ro.devumgapp.auth.network.AuthApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsuario;
    private TextInputLayout tilPassword;
    private TextInputEditText edtUsuario;
    private TextInputEditText edtPassword;
    private MaterialButton btnLogin;
    private LinearProgressIndicator progressBar;
    private MaterialCheckBox chkRecordar;
    private TextView txtLoginError;
    private AuthApiService authApiService;
    private Call<LoginResponse> loginCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        getWindow().setStatusBarColor(getResources().getColor(R.color.login_background, getTheme()));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.login_background, getTheme()));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        if (SessionManager.getInstance().isLoggedIn()) {
            navigateToHome();
            return;
        }

        authApiService = RetrofitClient.getClient().create(AuthApiService.class);
        bindViews();
        restoreRememberedUsername();
        setupFormActions();
    }

    @Override
    protected void onDestroy() {
        if (loginCall != null) {
            loginCall.cancel();
        }
        super.onDestroy();
    }

    private void bindViews() {
        tilUsuario = findViewById(R.id.tilUsuario);
        tilPassword = findViewById(R.id.tilPassword);
        edtUsuario = findViewById(R.id.edtUsuario);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        chkRecordar = findViewById(R.id.chkRecordar);
        txtLoginError = findViewById(R.id.txtLoginError);
    }

    private void restoreRememberedUsername() {
        String rememberedUsername = SessionManager.getInstance().getRememberedUsername();
        if (!rememberedUsername.isEmpty()) {
            edtUsuario.setText(rememberedUsername);
            chkRecordar.setChecked(true);
            edtPassword.requestFocus();
        }
    }

    private void setupFormActions() {
        edtUsuario.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                tilUsuario.setError(null);
                clearGeneralError();
            }
        });
        edtPassword.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                tilPassword.setError(null);
                clearGeneralError();
            }
        });
        edtPassword.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
        btnLogin.setOnClickListener(view -> attemptLogin());
    }

    private void attemptLogin() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            return;
        }

        String username = getText(edtUsuario).trim();
        String password = getText(edtPassword);

        if (!validateCredentials(username, password)) {
            return;
        }

        if (!hasInternetConnection()) {
            showGeneralError(getString(R.string.error_no_internet));
            return;
        }

        setLoading(true);
        LoginRequest request = new LoginRequest(username, password);
        loginCall = authApiService.login(request);
        loginCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    persistSession(username, response.body());
                    navigateToHome();
                    return;
                }
                showGeneralError(resolveErrorMessage(response));
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                showGeneralError(resolveFailureMessage(throwable));
            }
        });
    }

    private boolean validateCredentials(String username, String password) {
        boolean isValid = true;
        clearGeneralError();

        if (username.isEmpty()) {
            tilUsuario.setError(getString(R.string.error_username_required));
            isValid = false;
        } else {
            tilUsuario.setError(null);
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        return isValid;
    }

    private void persistSession(String username, LoginResponse response) {
        if (chkRecordar.isChecked()) {
            SessionManager.getInstance().rememberUsername(username);
        } else {
            SessionManager.getInstance().clearRememberedUsername();
        }
        SessionManager.getInstance().saveSession(response);
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? R.string.login_loading : R.string.login);
        tilUsuario.setEnabled(!loading);
        tilPassword.setEnabled(!loading);
        edtUsuario.setEnabled(!loading);
        edtPassword.setEnabled(!loading);
        chkRecordar.setEnabled(!loading);
    }

    private String resolveErrorMessage(Response<LoginResponse> response) {
        switch (response.code()) {
            case 400:
                return getString(R.string.error_invalid_data);
            case 401:
            case 403:
                return getString(R.string.error_invalid_credentials);
            case 404:
                return getString(R.string.error_endpoint_not_found);
            default:
                return getString(R.string.error_server);
        }
    }

    private String resolveFailureMessage(Throwable throwable) {
        if (!hasInternetConnection() || throwable instanceof UnknownHostException) {
            return getString(R.string.error_no_internet);
        }
        if (throwable instanceof SocketTimeoutException) {
            return getString(R.string.error_timeout);
        }
        return getString(R.string.error_network);
    }

    private boolean hasInternetConnection() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || connectivityManager.getActiveNetwork() == null) {
            return false;
        }
        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void showGeneralError(String message) {
        txtLoginError.setText(message);
        txtLoginError.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void clearGeneralError() {
        txtLoginError.setText(null);
        txtLoginError.setVisibility(View.GONE);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }
}
