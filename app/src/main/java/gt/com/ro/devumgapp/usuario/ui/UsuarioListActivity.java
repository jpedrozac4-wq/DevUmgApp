package gt.com.ro.devumgapp.usuario.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.core.ui.ModuleNavigation;
import gt.com.ro.devumgapp.usuario.dto.UsuarioResponse;
import gt.com.ro.devumgapp.usuario.network.UsuarioApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuarioListActivity extends AppCompatActivity implements UsuarioAdapter.Listener {

    private MaterialToolbar toolbar;
    private TextInputLayout tilBuscar;
    private TextInputEditText edtBuscar;
    private RecyclerView recyclerUsuarios;
    private LinearProgressIndicator progressBar;
    private TextView txtEmptyState;
    private TextView txtErrorState;
    private View usuarioHero;
    private View usuarioFilters;
    private View usuarioFiltersHeader;
    private View usuarioFilterControls;
    private boolean filtersExpanded;
    private MaterialButton btnBuscar;
    private MaterialButton btnAgregar;

    private UsuarioApiService apiService;
    private UsuarioAdapter adapter;
    private Call<List<UsuarioResponse>> listCall;
    private Call<Void> deleteCall;
    private ActivityResultLauncher<Intent> formLauncher;
    private ActivityResultLauncher<Intent> rolesLauncher;

    private final List<UsuarioResponse> allUsuarios = new ArrayList<>();
    private boolean loading;
    private boolean firstResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "USUARIOS_LEER")) return;
        setContentView(R.layout.activity_usuario_list);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        apiService = RetrofitClient.getClient().create(UsuarioApiService.class);
        formLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadUsuarios();
                    }
                });
        rolesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadUsuarios();
                    }
                });
        bindViews();
        setupToolbar();
        setupRecycler();
        setupActions();
        animateIntro();
        loadUsuarios();
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
            return;
        }
        loadUsuarios();
    }

    @Override
    public void onEdit(UsuarioResponse usuario) {
        Intent intent = new Intent(this, UsuarioFormActivity.class);
        intent.putExtra(UsuarioFormActivity.EXTRA_ID, usuario.id);
        formLauncher.launch(intent);
    }

    @Override
    public void onDelete(UsuarioResponse usuario) {
        SgauDialog.confirm(this, R.drawable.ic_power,
                getString(R.string.usuario_eliminar_titulo),
                getString(R.string.usuario_eliminar_mensaje, UsuarioAdapter.displayName(usuario)),
                getString(R.string.usuario_eliminar_confirmar), () -> deleteUsuario(usuario.id));
    }

    @Override
    public void onManageRoles(UsuarioResponse usuario) {
        Intent intent = new Intent(this, UsuarioRolesActivity.class);
        intent.putExtra(UsuarioRolesActivity.EXTRA_ID, usuario.id);
        rolesLauncher.launch(intent);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarUsuarioList);
        tilBuscar = findViewById(R.id.tilBuscarUsuario);
        edtBuscar = findViewById(R.id.edtBuscarUsuario);
        recyclerUsuarios = findViewById(R.id.recyclerUsuarios);
        progressBar = findViewById(R.id.progressUsuarios);
        txtEmptyState = findViewById(R.id.txtUsuariosEmpty);
        txtErrorState = findViewById(R.id.txtUsuariosError);
        usuarioHero = findViewById(R.id.usuarioListHero);
        usuarioFilters = findViewById(R.id.usuarioFilters);
        usuarioFiltersHeader = findViewById(R.id.usuarioFiltersHeader);
        usuarioFilterControls = findViewById(R.id.usuarioFilterControls);
        btnBuscar = findViewById(R.id.btnBuscarUsuario);
        btnAgregar = findViewById(R.id.btnAgregarUsuario);
        btnAgregar.setVisibility(Permissions.has("USUARIOS_CREAR") ? View.VISIBLE : View.GONE);
    }

    private void setupToolbar() {
        toolbar.setTitle("");
        ModuleNavigation.attach(this, toolbar);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void animateIntro() {
        usuarioHero.setAlpha(0f);
        usuarioHero.setTranslationY(20f);
        usuarioHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        usuarioFilters.setAlpha(0f);
        usuarioFilters.setTranslationY(16f);
        usuarioFilters.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(110)
                .setDuration(300)
                .start();
    }

    private void setupRecycler() {
        adapter = new UsuarioAdapter(null, this);
        recyclerUsuarios.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsuarios.setAdapter(adapter);
    }

    private void setupActions() {
        usuarioFiltersHeader.setOnClickListener(view -> setFiltersExpanded(!filtersExpanded));
        btnBuscar.setOnClickListener(view -> applyFilter());
        tilBuscar.setEndIconOnClickListener(view -> applyFilter());
        edtBuscar.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applyFilter();
                return true;
            }
            return false;
        });
        btnAgregar.setOnClickListener(view ->
                formLauncher.launch(new Intent(this, UsuarioFormActivity.class)));
    }

    private void setFiltersExpanded(boolean expanded) {
        filtersExpanded = expanded;
        if (expanded) {
            usuarioFilterControls.setVisibility(View.VISIBLE);
            usuarioFilterControls.setAlpha(0f);
            usuarioFilterControls.setTranslationY(-8f);
            usuarioFilterControls.animate().alpha(1f).translationY(0f).setDuration(180).start();
            return;
        }
        usuarioFilterControls.animate().alpha(0f).translationY(-8f).setDuration(140)
                .withEndAction(() -> usuarioFilterControls.setVisibility(View.GONE)).start();
    }

    private void loadUsuarios() {
        if (loading) {
            return;
        }
        if (listCall != null) {
            listCall.cancel();
        }
        setLoading(true);
        txtErrorState.setVisibility(View.GONE);

        listCall = apiService.listar();
        listCall.enqueue(new Callback<List<UsuarioResponse>>() {
            @Override
            public void onResponse(
                    Call<List<UsuarioResponse>> call,
                    Response<List<UsuarioResponse>> response) {
                setLoading(false);
                listCall = null;
                if (response.isSuccessful()) {
                    renderUsuarios(response.body() == null
                            ? new ArrayList<>()
                            : response.body());
                    return;
                }
                showError(UsuarioErrorMapper.fromResponse(UsuarioListActivity.this, response));
            }

            @Override
            public void onFailure(Call<List<UsuarioResponse>> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                listCall = null;
                showError(UsuarioErrorMapper.fromFailure(UsuarioListActivity.this, throwable));
            }
        });
    }

    private void renderUsuarios(List<UsuarioResponse> usuarios) {
        allUsuarios.clear();
        allUsuarios.addAll(usuarios);
        applyFilter();
    }

    /**
     * {@code GET /api/usuarios} has no server-side search, so the filter runs over the list
     * already loaded, matching the same fields the search hint advertises.
     */
    private void applyFilter() {
        String query = getText(edtBuscar).trim().toLowerCase(Locale.ROOT);
        List<UsuarioResponse> filtered = new ArrayList<>();
        for (UsuarioResponse usuario : allUsuarios) {
            if (usuario == null) {
                continue;
            }
            if (query.isEmpty() || matches(usuario, query)) {
                filtered.add(usuario);
            }
        }
        adapter.submitList(filtered);
        boolean empty = filtered.isEmpty();
        txtEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerUsuarios.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean matches(UsuarioResponse usuario, String query) {
        return contains(usuario.username, query)
                || contains(usuario.email, query)
                || contains(usuario.nombre, query)
                || contains(usuario.apellido, query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void deleteUsuario(long usuarioId) {
        if (loading) {
            return;
        }
        setLoading(true);
        deleteCall = apiService.eliminar(usuarioId);
        deleteCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);
                deleteCall = null;
                // DELETE returns 204 with no body: never read it.
                if (response.isSuccessful()) {
                    UiNotifier.success(UsuarioListActivity.this, R.string.usuario_eliminado);
                    loadUsuarios();
                    return;
                }
                UiNotifier.error(
                        UsuarioListActivity.this,
                        UsuarioErrorMapper.fromResponse(UsuarioListActivity.this, response));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                setLoading(false);
                deleteCall = null;
                UiNotifier.error(
                        UsuarioListActivity.this,
                        UsuarioErrorMapper.fromFailure(UsuarioListActivity.this, throwable));
            }
        });
    }

    private void showError(String message) {
        allUsuarios.clear();
        adapter.submitList(null);
        recyclerUsuarios.setVisibility(View.GONE);
        txtEmptyState.setVisibility(View.GONE);
        txtErrorState.setText(message);
        txtErrorState.setVisibility(View.VISIBLE);
        UiNotifier.error(this, message);
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnBuscar.setEnabled(!loading);
        btnAgregar.setEnabled(!loading);
        edtBuscar.setEnabled(!loading);
        tilBuscar.setEnabled(!loading);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private void cancelCalls() {
        if (listCall != null) {
            listCall.cancel();
        }
        if (deleteCall != null) {
            deleteCall.cancel();
        }
    }
}
