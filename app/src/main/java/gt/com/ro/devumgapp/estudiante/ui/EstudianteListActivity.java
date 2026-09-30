package gt.com.ro.devumgapp.estudiante.ui;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.ModuleNavigation;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResponse;
import gt.com.ro.devumgapp.estudiante.dto.EstadoRequest;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstudianteListActivity extends AppCompatActivity implements EstudianteAdapter.Listener {
    private static final int PAGE_SIZE = 10;

    private TextInputEditText edtBuscar;
    private MaterialButtonToggleGroup toggleEstado;
    private LinearLayout filtersHeader, filterControls;
    private ImageView filtersChevron;
    private RecyclerView recycler;
    private LinearProgressIndicator progress;
    private TextView empty, error, pageInfo;
    private MaterialButton btnBuscar, btnAnterior, btnSiguiente, btnAbrirBusqueda;
    private EstudianteAdapter adapter;
    private EstudianteApiService service;
    private Call<PageResponse<EstudianteResponse>> listCall;
    private int pagina = 0;
    private int totalPages = 0;
    private Boolean activo = null;
    private boolean filtersExpanded;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "ESTUDIANTES_LEER")) return;
        setContentView(R.layout.activity_estudiante_list);
        service = RetrofitClient.getClient().create(EstudianteApiService.class);
        bindViews();
        setupToolbar();
        setupRecycler();
        setupFilters();
        setupActions();
        loadEstudiantes(0);
    }

    @Override protected void onDestroy() {
        if (listCall != null) listCall.cancel();
        super.onDestroy();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarEstudiantes);
        edtBuscar = findViewById(R.id.edtBuscarEstudiante);
        toggleEstado = findViewById(R.id.toggleEstadoEstudiante);
        filtersHeader = findViewById(R.id.estudianteFiltersHeader);
        filterControls = findViewById(R.id.estudianteFilterControls);
        filtersChevron = findViewById(R.id.imgEstudiantesFiltersChevron);
        recycler = findViewById(R.id.recyclerEstudiantes);
        progress = findViewById(R.id.progressEstudiantes);
        empty = findViewById(R.id.txtEstudiantesEmpty);
        error = findViewById(R.id.txtEstudiantesError);
        pageInfo = findViewById(R.id.txtEstudiantesPageInfo);
        btnBuscar = findViewById(R.id.btnBuscarEstudiante);
        btnAnterior = findViewById(R.id.btnEstudiantesAnterior);
        btnSiguiente = findViewById(R.id.btnEstudiantesSiguiente);
        btnAbrirBusqueda = findViewById(R.id.btnAgregarEstudiante);
        btnAbrirBusqueda.setVisibility(View.VISIBLE);
    }

    private void setupToolbar() {
        ModuleNavigation.attach(this, findViewById(R.id.toolbarEstudiantes));
    }

    private void setupRecycler() {
        adapter = new EstudianteAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
    }

    private void setupFilters() {
        toggleEstado.check(R.id.btnFiltroEstudiantesTodas);
        toggleEstado.addOnButtonCheckedListener((group, checkedId, checked) -> {
            if (!checked) return;
            if (checkedId == R.id.btnFiltroEstudiantesActivos) activo = true;
            else if (checkedId == R.id.btnFiltroEstudiantesInactivos) activo = false;
            else activo = null;
            pagina = 0;
            loadEstudiantes(pagina);
        });
    }

    private void setupActions() {
        filtersHeader.setOnClickListener(v -> setFiltersExpanded(!filtersExpanded));
        btnBuscar.setOnClickListener(v -> { pagina = 0; loadEstudiantes(pagina); });
        btnAnterior.setOnClickListener(v -> { if (pagina > 0) loadEstudiantes(--pagina); });
        btnSiguiente.setOnClickListener(v -> { if (pagina + 1 < totalPages) loadEstudiantes(++pagina); });
        btnAbrirBusqueda.setOnClickListener(v -> {
            setFiltersExpanded(true);
            edtBuscar.requestFocus();
            edtBuscar.post(() -> ((android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(edtBuscar,
                    android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT));
        });
    }

    private void setFiltersExpanded(boolean expanded) {
        filtersExpanded = expanded;
        filtersHeader.setContentDescription(expanded ? "Ocultar filtros" : "Mostrar filtros");
        filtersChevron.animate().rotation(expanded ? 90f : 0f).setDuration(180).start();
        if (expanded) {
            filterControls.setVisibility(View.VISIBLE);
            filterControls.setAlpha(0f);
            filterControls.setTranslationY(-8f);
            filterControls.animate().alpha(1f).translationY(0f).setDuration(180).start();
            return;
        }
        filterControls.animate().alpha(0f).translationY(-8f).setDuration(140)
                .withEndAction(() -> filterControls.setVisibility(View.GONE)).start();
    }

    private void loadEstudiantes(int page) {
        if (listCall != null) listCall.cancel();
        setLoading(true);
        String texto = edtBuscar.getText() == null ? "" : edtBuscar.getText().toString().trim();
        listCall = service.listarEstudiantes(texto, activo, page, PAGE_SIZE);
        listCall.enqueue(new Callback<PageResponse<EstudianteResponse>>() {
            @Override public void onResponse(Call<PageResponse<EstudianteResponse>> call,
                                             Response<PageResponse<EstudianteResponse>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<EstudianteResponse> body = response.body();
                    List<EstudianteResponse> content = body.content == null
                            ? new ArrayList<>() : body.content;
                    adapter.submitList(content);
                    totalPages = body.totalPages;
                    pagina = body.number;
                    error.setVisibility(View.GONE);
                    empty.setText(R.string.estudiante_empty);
                    empty.setVisibility(content.isEmpty() ? View.VISIBLE : View.GONE);
                    pageInfo.setText(getString(R.string.estudiante_pagina_info,
                            pagina + 1, Math.max(totalPages, 1)));
                    btnAnterior.setEnabled(!body.first);
                    btnSiguiente.setEnabled(!body.last);
                } else {
                    adapter.submitList(new ArrayList<>());
                    empty.setVisibility(View.GONE);
                    error.setText(EstudianteErrorMapper.fromResponse(EstudianteListActivity.this, response));
                    error.setVisibility(View.VISIBLE);
                    pageInfo.setText("");
                }
            }
            @Override public void onFailure(Call<PageResponse<EstudianteResponse>> call, Throwable t) {
                if (call.isCanceled()) return;
                setLoading(false);
                adapter.submitList(new ArrayList<>());
                empty.setVisibility(View.GONE);
                error.setText(EstudianteErrorMapper.fromFailure(EstudianteListActivity.this, t));
                error.setVisibility(View.VISIBLE);
                pageInfo.setText("");
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnBuscar.setEnabled(!loading);
        btnAbrirBusqueda.setEnabled(!loading);
        btnAnterior.setEnabled(!loading && pagina > 0);
        btnSiguiente.setEnabled(!loading && pagina + 1 < totalPages);
    }

    @Override public void onEdit(EstudianteResponse estudiante) {
        Intent intent = new Intent(this, EstudianteFormActivity.class);
        intent.putExtra("estudianteId", estudiante.id);
        startActivity(intent);
    }

    @Override public void onDetail(EstudianteResponse estudiante) {
        Intent intent = new Intent(this, EstudianteDetailActivity.class);
        intent.putExtra("estudianteId", estudiante.id);
        startActivity(intent);
    }

    @Override public void onToggleStatus(EstudianteResponse estudiante) {
        SgauDialog.confirmState(this, !estudiante.activo,
                "el estudiante \"" + estudiante.nombres + " " + estudiante.apellidos + "\"",
                () -> changeStudentStatus(estudiante));
    }

    private void changeStudentStatus(EstudianteResponse estudiante) {
        adapter.setStatusChanging(estudiante.id, true);
        service.cambiarEstadoEstudiante(estudiante.id, new EstadoRequest(!estudiante.activo))
                .enqueue(new Callback<EstudianteResponse>() {
                    @Override public void onResponse(Call<EstudianteResponse> call, Response<EstudianteResponse> response) {
                        adapter.setStatusChanging(estudiante.id, false);
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.replace(response.body());
                            UiNotifier.success(EstudianteListActivity.this, estudiante.activo
                                    ? R.string.estudiante_desactivado
                                    : R.string.estudiante_activado);
                        } else {
                            UiNotifier.error(EstudianteListActivity.this,
                                    EstudianteErrorMapper.fromResponse(EstudianteListActivity.this, response));
                        }
                    }
                    @Override public void onFailure(Call<EstudianteResponse> call, Throwable t) {
                        if (call.isCanceled()) return;
                        adapter.setStatusChanging(estudiante.id, false);
                        UiNotifier.error(EstudianteListActivity.this,
                                EstudianteErrorMapper.fromFailure(EstudianteListActivity.this, t));
                    }
                });
    }
}

