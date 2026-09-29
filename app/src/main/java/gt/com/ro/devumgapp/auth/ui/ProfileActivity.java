package gt.com.ro.devumgapp.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Set;
import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.auth.dto.ApiError;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import gt.com.ro.devumgapp.auth.dto.PasswordChangeRequest;
import gt.com.ro.devumgapp.auth.dto.ProfileUpdateRequest;
import gt.com.ro.devumgapp.auth.dto.ProfileUpdateResponse;
import gt.com.ro.devumgapp.auth.network.AuthApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Authenticated self-service profile. Server authorization remains authoritative. */
public class ProfileActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private AuthApiService authApiService;
    private Call<?> activeCall;
    private View progress;
    private TextView errorMessage, avatar, name, username, userId, email, roles;
    private MaterialButton retry, editProfile, changePassword;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = SessionManager.getInstance();
        if (!sessionManager.isLoggedIn()) { SessionManager.handleUnauthorized(this); return; }
        setContentView(R.layout.activity_profile);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_background));
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        authApiService = RetrofitClient.getClient().create(AuthApiService.class);
        MaterialToolbar toolbar = findViewById(R.id.profileToolbar);
        toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        progress = findViewById(R.id.profileProgress); errorMessage = findViewById(R.id.txtProfileError);
        avatar = findViewById(R.id.txtProfileAvatar); name = findViewById(R.id.txtProfileName);
        username = findViewById(R.id.txtProfileUsername); userId = findViewById(R.id.txtProfileUserId);
        email = findViewById(R.id.txtProfileEmail); roles = findViewById(R.id.txtProfileRoles);
        retry = findViewById(R.id.btnProfileRetry);
        editProfile = findViewById(R.id.btnEditProfile); changePassword = findViewById(R.id.btnChangePassword);
        retry.setOnClickListener(view -> loadProfile());
        editProfile.setOnClickListener(view -> showEditDialog());
        changePassword.setOnClickListener(view -> showPasswordDialog());
        renderStoredProfile(); loadProfile();
    }

    private void loadProfile() {
        cancelActiveCall(); setLoading(true); clearError();
        Call<LoginResponse> call = authApiService.me(); activeCall = call;
        call.enqueue(new Callback<LoginResponse>() {
            @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (!finishCall(call)) return;
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.refreshProfile(response.body()); renderStoredProfile();
                } else if (response.code() == 401 || response.code() == 403) {
                    SessionManager.handleUnauthorized(ProfileActivity.this);
                } else showLoadError(errorFrom(response, false));
            }
            @Override public void onFailure(Call<LoginResponse> call, Throwable error) {
                if (!finishCall(call) || call.isCanceled()) return;
                showLoadError(getString(R.string.profile_network_error));
            }
        });
    }

    private void showEditDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText user = view.findViewById(R.id.edtProfileUsername), mail = view.findViewById(R.id.edtProfileEmail);
        TextInputEditText first = view.findViewById(R.id.edtProfileNombre), last = view.findViewById(R.id.edtProfileApellido);
        user.setText(sessionManager.getUsername()); mail.setText(sessionManager.getEmail());
        first.setText(sessionManager.getNombre()); last.setText(sessionManager.getApellido());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(R.string.profile_edit_title).setView(view)
                .setNegativeButton(R.string.profile_cancel, null).setPositiveButton(R.string.profile_save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            String u = text(user).trim(), e = sessionManager.getEmail().trim();
            String n = text(first).trim(), a = text(last).trim();
            if (!ProfileValidator.isProfileValid(u, e, n, a)) {
                Toast.makeText(this, R.string.profile_invalid_fields, Toast.LENGTH_LONG).show(); return;
            }
            dialog.dismiss(); updateProfile(new ProfileUpdateRequest(u, e, n, a));
        }));
        dialog.show();
    }

    private void updateProfile(ProfileUpdateRequest request) {
        setLoading(true); clearError();
        Call<ProfileUpdateResponse> call = authApiService.updateProfile(request); activeCall = call;
        call.enqueue(new Callback<ProfileUpdateResponse>() {
            @Override public void onResponse(Call<ProfileUpdateResponse> call, Response<ProfileUpdateResponse> response) {
                if (!finishCall(call)) return;
                if (response.isSuccessful() && response.body() != null) {
                    ProfileUpdateResponse updated = response.body();
                    sessionManager.refreshProfile(updated); renderStoredProfile();
                    if (updated.requiereNuevoLogin) redirectForNewLogin(updated.username == null ? request.username : updated.username);
                    else Toast.makeText(ProfileActivity.this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                } else if (response.code() == 401) SessionManager.handleUnauthorized(ProfileActivity.this);
                else showOperationError(errorFrom(response, false));
            }
            @Override public void onFailure(Call<ProfileUpdateResponse> call, Throwable error) {
                if (!finishCall(call) || call.isCanceled()) return;
                showOperationError(getString(R.string.profile_network_error));
            }
        });
    }

    private void showPasswordDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        TextInputLayout currentLayout = view.findViewById(R.id.tilCurrentPassword), newLayout = view.findViewById(R.id.tilNewPassword);
        TextInputEditText current = view.findViewById(R.id.edtCurrentPassword), next = view.findViewById(R.id.edtNewPassword);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(R.string.profile_change_password_title).setView(view)
                .setNegativeButton(R.string.profile_cancel, null).setPositiveButton(R.string.profile_save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            String oldPassword = text(current), newPassword = text(next);
            currentLayout.setError(null); newLayout.setError(null);
            ProfileValidator.PasswordError validation = ProfileValidator.validatePassword(oldPassword, newPassword);
            if (validation == ProfileValidator.PasswordError.CURRENT_REQUIRED) currentLayout.setError(getString(R.string.profile_current_password_required));
            else if (validation == ProfileValidator.PasswordError.INVALID_LENGTH) newLayout.setError(getString(R.string.profile_password_length));
            else if (validation == ProfileValidator.PasswordError.SAME_PASSWORD) newLayout.setError(getString(R.string.profile_password_same));
            else { dialog.dismiss(); changePassword(oldPassword, newPassword); }
        }));
        dialog.show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        setLoading(true); clearError();
        Call<Void> call = authApiService.changePassword(new PasswordChangeRequest(currentPassword, newPassword)); activeCall = call;
        call.enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (!finishCall(call)) return;
                if (response.isSuccessful()) Toast.makeText(ProfileActivity.this, R.string.profile_password_updated, Toast.LENGTH_LONG).show();
                else showOperationError(errorFrom(response, true));
            }
            @Override public void onFailure(Call<Void> call, Throwable error) {
                if (!finishCall(call) || call.isCanceled()) return;
                showOperationError(getString(R.string.profile_network_error));
            }
        });
    }

    private String errorFrom(Response<?> response, boolean passwordRequest) {
        String backend = backendMessage(response);
        if (backend != null && !backend.trim().isEmpty()) return backend;
        switch (ProfileErrorPolicy.classify(response.code(), passwordRequest)) {
            case INVALID_DATA: return getString(R.string.profile_error_bad_request);
            case EXPIRED_SESSION: return getString(R.string.profile_error_unauthorized);
            case CURRENT_PASSWORD: return getString(R.string.profile_error_current_password);
            case DUPLICATE: return getString(R.string.profile_error_conflict);
            case RATE_LIMIT: return getString(R.string.profile_error_rate_limit);
            default: return getString(R.string.profile_error_server);
        }
    }

    private String backendMessage(Response<?> response) {
        if (response.errorBody() == null) return null;
        try {
            ApiError apiError = new Gson().fromJson(response.errorBody().string(), ApiError.class);
            return apiError == null ? null : apiError.message;
        } catch (IOException | RuntimeException ignored) { return null; }
    }

    private void redirectForNewLogin(String newUsername) {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_PREFILL_USERNAME, newUsername);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent); finish();
    }

    private void renderStoredProfile() {
        String fullName = sessionManager.getNombreCompleto().trim(), user = sessionManager.getUsername().trim();
        String displayName = fullName.isEmpty() ? user : fullName;
        name.setText(displayName.isEmpty() ? getString(R.string.dashboard_user_fallback) : displayName);
        username.setText(orUnavailable(user));
        userId.setText(sessionManager.getUsuarioId() > 0 ? String.valueOf(sessionManager.getUsuarioId()) : getString(R.string.profile_not_available));
        email.setText(orUnavailable(sessionManager.getEmail().trim()));
        roles.setText(joinOrFallback(sessionManager.getRoles(), R.string.dashboard_roles_fallback));
        avatar.setText(displayName.isEmpty() ? "S" : displayName.substring(0, 1).toUpperCase());
    }

    private String joinOrFallback(Set<String> values, int fallback) { return values.isEmpty() ? getString(fallback) : String.join(", ", values); }
    private String orUnavailable(String value) { return value.isEmpty() ? getString(R.string.profile_not_available) : value; }
    private String text(TextInputEditText editText) { return editText.getText() == null ? "" : editText.getText().toString(); }
    private void clearError() { errorMessage.setVisibility(View.GONE); retry.setVisibility(View.GONE); }
    private void showLoadError(String message) { errorMessage.setText(message); errorMessage.setVisibility(View.VISIBLE); retry.setVisibility(View.VISIBLE); }
    private void showOperationError(String message) { errorMessage.setText(message); errorMessage.setVisibility(View.VISIBLE); Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void setLoading(boolean loading) { progress.setVisibility(loading ? View.VISIBLE : View.GONE); retry.setEnabled(!loading); editProfile.setEnabled(!loading); changePassword.setEnabled(!loading); }
    private boolean finishCall(Call<?> call) { if (activeCall != call || isFinishing() || isDestroyed()) return false; activeCall = null; setLoading(false); return true; }
    private void cancelActiveCall() { if (activeCall != null) activeCall.cancel(); activeCall = null; }
    @Override protected void onDestroy() { cancelActiveCall(); super.onDestroy(); }
}
