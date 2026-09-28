package gt.com.ro.devumgapp.auth.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.Set;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import gt.com.ro.devumgapp.auth.network.AuthApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Read-only self-service profile until the backend exposes authenticated update endpoints. */
public class ProfileActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private Call<LoginResponse> profileCall;
    private View progress;
    private TextView errorMessage;
    private TextView avatar;
    private TextView name;
    private TextView username;
    private TextView email;
    private TextView roles;
    private MaterialButton retry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = SessionManager.getInstance();
        if (!sessionManager.isLoggedIn()) {
            SessionManager.handleUnauthorized(this);
            return;
        }
        setContentView(R.layout.activity_profile);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_background));
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        MaterialToolbar toolbar = findViewById(R.id.profileToolbar);
        toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        progress = findViewById(R.id.profileProgress);
        errorMessage = findViewById(R.id.txtProfileError);
        avatar = findViewById(R.id.txtProfileAvatar);
        name = findViewById(R.id.txtProfileName);
        username = findViewById(R.id.txtProfileUsername);
        email = findViewById(R.id.txtProfileEmail);
        roles = findViewById(R.id.txtProfileRoles);
        retry = findViewById(R.id.btnProfileRetry);
        retry.setOnClickListener(view -> loadProfile());
        findViewById(R.id.btnEditProfile).setOnClickListener(view -> showUnavailableMessage());
        findViewById(R.id.btnChangePassword).setOnClickListener(view -> showUnavailableMessage());
        renderStoredProfile();
        loadProfile();
    }

    private void loadProfile() {
        if (profileCall != null) profileCall.cancel();
        setLoading(true);
        errorMessage.setVisibility(View.GONE);
        retry.setVisibility(View.GONE);
        profileCall = RetrofitClient.getClient().create(AuthApiService.class).me();
        profileCall.enqueue(new Callback<LoginResponse>() {
            @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                profileCall = null;
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.refreshProfile(response.body());
                    renderStoredProfile();
                } else if (response.code() == 401 || response.code() == 403) {
                    SessionManager.handleUnauthorized(ProfileActivity.this);
                } else {
                    showLoadError(getString(R.string.profile_load_error));
                }
            }

            @Override public void onFailure(Call<LoginResponse> call, Throwable error) {
                profileCall = null;
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setLoading(false);
                showLoadError(getString(R.string.profile_network_error));
            }
        });
    }

    private void renderStoredProfile() {
        String fullName = sessionManager.getNombreCompleto().trim();
        String user = sessionManager.getUsername().trim();
        String displayName = fullName.isEmpty() ? user : fullName;
        name.setText(displayName.isEmpty() ? getString(R.string.dashboard_user_fallback) : displayName);
        username.setText(user.isEmpty() ? getString(R.string.profile_not_available) : user);
        String storedEmail = sessionManager.getEmail().trim();
        email.setText(storedEmail.isEmpty() ? getString(R.string.profile_not_available) : storedEmail);
        Set<String> storedRoles = sessionManager.getRoles();
        roles.setText(storedRoles.isEmpty() ? getString(R.string.dashboard_roles_fallback) : String.join(", ", storedRoles));
        avatar.setText(displayName.isEmpty() ? "S" : displayName.substring(0, 1).toUpperCase());
    }

    private void showLoadError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
        retry.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        retry.setEnabled(!loading);
    }

    private void showUnavailableMessage() {
        Toast.makeText(this, R.string.profile_edit_unavailable, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (profileCall != null) profileCall.cancel();
        super.onDestroy();
    }
}
