package gt.com.ro.devumgapp.auth.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.progressindicator.CircularProgressIndicator;
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
import gt.com.ro.devumgapp.core.updates.UpdateManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_PREFILL_USERNAME = "prefill_username";

    private TextInputLayout tilUsuario;
    private TextInputLayout tilPassword;
    private TextInputEditText edtUsuario;
    private TextInputEditText edtPassword;
    private MaterialButton btnLogin;
    private LinearProgressIndicator progressBar;
    private CircularProgressIndicator progressSession;
    private MaterialCheckBox chkRecordar;
    private TextView txtLoginError;
    private ScrollView loginScroll;
    private View imgTopDecoration;
    private View imgLogo;
    private AuthApiService authApiService;
    private Call<LoginResponse> loginCall;
    private Call<LoginResponse> profileCall;

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

        authApiService = RetrofitClient.getClient().create(AuthApiService.class);
        bindViews();
        boolean hasSavedSession = SessionManager.getInstance().isLoggedIn();
        setSessionChecking(hasSavedSession);
        setupKeyboardInsets();
        restoreRememberedUsername();
        setupFormActions();
        if (hasSavedSession) {
            refreshSavedProfile();
        } else {
            checkForUpdates(null);
        }
    }

    private void checkForUpdates(Runnable onFinished) {
        new UpdateManager(this).checkForUpdates(new UpdateManager.UpdateCheckCallback() {
            @Override
            public void onNoUpdate() {
                if (onFinished != null) {
                    onFinished.run();
                }
            }

            @Override
            public void onError(Exception e) {
                if (onFinished != null) {
                    onFinished.run();
                }
            }

            @Override
            public void onPostponed() {
                if (onFinished != null) {
                    onFinished.run();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (loginCall != null) {
            loginCall.cancel();
        }
        if (profileCall != null) profileCall.cancel();
        super.onDestroy();
    }

    private void refreshSavedProfile() {
        setLoading(true);
        profileCall = authApiService.me();
        profileCall.enqueue(new Callback<LoginResponse>() {
            @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (isFinishing()) return;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    SessionManager.getInstance().refreshProfile(response.body());
                    checkForUpdates(LoginActivity.this::navigateToHome);
                } else if (response.code() == 401) {
                    SessionManager.getInstance().logout();
                    setSessionChecking(false);
                    showGeneralError(getString(R.string.error_invalid_credentials));
                    checkForUpdates(null);
                } else {
                    setSessionChecking(false);
                    showGeneralError("No se pudo actualizar la sesión. Intenta iniciar sesión de nuevo.");
                }
            }
            @Override public void onFailure(Call<LoginResponse> call, Throwable error) {
                if (call.isCanceled() || isFinishing()) return;
                setLoading(false);
                setSessionChecking(false);
                showGeneralError(resolveFailureMessage(error));
            }
        });
    }

    private void bindViews() {
        tilUsuario = findViewById(R.id.tilUsuario);
        tilPassword = findViewById(R.id.tilPassword);
        edtUsuario = findViewById(R.id.edtUsuario);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        progressSession = findViewById(R.id.progressSession);
        chkRecordar = findViewById(R.id.chkRecordar);
        txtLoginError = findViewById(R.id.txtLoginError);
        loginScroll = findViewById(R.id.loginScroll);
        imgTopDecoration = findViewById(R.id.imgTopDecoration);
        imgLogo = findViewById(R.id.imgLogo);
    }

    private void setupKeyboardInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(loginScroll, (view, insets) -> {
            int keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navigationBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            int actionGap = insets.isVisible(WindowInsetsCompat.Type.ime())
                    ? dpToPx(20)
                    : 0;
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                    Math.max(keyboardBottom, navigationBottom) + actionGap);
            if (insets.isVisible(WindowInsetsCompat.Type.ime()) && edtPassword.hasFocus()) {
                revealPasswordActions();
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(loginScroll);
    }

    private void revealPasswordActions() {
        loginScroll.postDelayed(() -> {
            // Scroll through the login button, not only through the password
            // field, so the primary action remains above the keyboard.
            Rect actionBounds = new Rect();
            btnLogin.getDrawingRect(actionBounds);
            loginScroll.offsetDescendantRectToMyCoords(btnLogin, actionBounds);
            int visibleBottom = loginScroll.getHeight() - loginScroll.getPaddingBottom();
            int target = Math.max(0, actionBounds.bottom - visibleBottom + dpToPx(20));
            loginScroll.smoothScrollTo(0, target);
        }, 220);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void restoreRememberedUsername() {
        String rememberedUsername = getIntent().getStringExtra(EXTRA_PREFILL_USERNAME);
        if (rememberedUsername == null || rememberedUsername.isEmpty()) {
            rememberedUsername = SessionManager.getInstance().getRememberedUsername();
        }
        if (!rememberedUsername.isEmpty()) {
            edtUsuario.setText(rememberedUsername);
            chkRecordar.setChecked(!SessionManager.getInstance().getRememberedUsername().isEmpty());
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
                revealPasswordActions();
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
        gt.com.ro.devumgapp.notificacion.PushRegistrationManager.register(this);
    }

    private void navigateToHome() {
        String pendingNotificationId = getIntent().getStringExtra("pendingNotificationId");
        if (pendingNotificationId != null) {
            Intent inbox = new Intent(this, gt.com.ro.devumgapp.notificacion.ui.NotificationInboxActivity.class);
            inbox.putExtra(gt.com.ro.devumgapp.notificacion.ui.NotificationInboxActivity.EXTRA_NOTIFICATION_ID, pendingNotificationId);
            startActivity(inbox); finish(); return;
        }
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("profileVerified", true);
        startActivity(intent);
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

    private void setSessionChecking(boolean checking) {
        int formVisibility = checking ? View.INVISIBLE : View.VISIBLE;
        loginScroll.setVisibility(formVisibility);
        imgTopDecoration.setVisibility(formVisibility);
        imgLogo.setVisibility(formVisibility);
        progressSession.setVisibility(checking ? View.VISIBLE : View.GONE);
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
