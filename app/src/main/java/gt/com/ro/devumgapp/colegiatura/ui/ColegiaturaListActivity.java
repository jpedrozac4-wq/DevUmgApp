package gt.com.ro.devumgapp.colegiatura.ui;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaResponse;
import gt.com.ro.devumgapp.colegiatura.network.ColegiaturaApiService;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.network.ApiResponses;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.ModuleNavigation;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ColegiaturaListActivity extends AppCompatActivity implements ColegiaturaAdapter.Listener {

    private static final int PAGE_SIZE = 10;

    private LinearProgressIndicator progress;
    private RecyclerView recycler;
    private android.widget.TextView estadoLista, paginaInfo;
    private android.widget.Button btnAnterior, btnSiguiente;
    private MaterialAutoCompleteTextView actEstudiante, actEstado;
    private TextInputEditText edtCiclo, edtConcepto;
    private MaterialButtonToggleGroup toggleActivo;
    private View filtersHeader, filterControls;
    private boolean filtersExpanded;

    private ColegiaturaAdapter adapter;
    private ColegiaturaApiService service;

    private final List<EstudianteResumenResponse> estudiantes = new ArrayList<>();
    private long selectedEstudianteId = -1L;
    private int pagina = 0;
    private int totalPaginas = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "COLEGIATURAS_LEER")) return;
        setContentView(R.layout.activity_colegiatura_list);

        service = RetrofitClient.getClient().create(ColegiaturaApiService.class);
        bindViews();
        configurarVista();
        cargarEstudiantes();
        cargarColegiaturas();
    }

    private void bindViews() {
        progress = findViewById(R.id.progressColegiatura);
        recycler = findViewById(R.id.recyclerColegiaturas);
        estadoLista = findViewById(R.id.txtColegiaturaEstadoLista);
        paginaInfo = findViewById(R.id.txtColegiaturaPagina);
        btnAnterior = findViewById(R.id.btnColegiaturaAnterior);
        btnSiguiente = findViewById(R.id.btnColegiaturaSiguiente);
        actEstudiante = findViewById(R.id.actColegiaturaEstudiante);
        actEstado = findViewById(R.id.actColegiaturaEstado);
        edtCiclo = findViewById(R.id.edtColegiaturaCiclo);
        edtConcepto = findViewById(R.id.edtColegiaturaConcepto);
        toggleActivo = findViewById(R.id.toggleColegiaturaActivo);
        filtersHeader = findViewById(R.id.colegiaturaFiltersHeader);
        filterControls = findViewById(R.id.colegiaturaFilterControls);

        findViewById(R.id.toolbarColegiaturas).setOnClickListener(v -> { });
        findViewById(R.id.toolbarColegiaturas).setOnTouchListener((v, event) -> false);
        ModuleNavigation.attach(this, findViewById(R.id.toolbarColegiaturas));
    }

    private void configurarVista() {
        adapter = new ColegiaturaAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        gt.com.ro.devumgapp.core.ui.FilterPanelTouch.bind(filtersHeader, filterControls, () -> setFiltersExpanded(!filtersExpanded));

        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line,
                new String[]{
                        getString(R.string.colegiatura_todos_estados),
                        "PENDIENTE", "PARCIAL", "PAGADA", "ANULADA"
                });
        actEstado.setAdapter(estadoAdapter);
        actEstado.setText(estadoAdapter.getItem(0), false);

        findViewById(R.id.btnBuscarColegiatura).setOnClickListener(v -> {
            pagina = 0;
            cargarColegiaturas();
        });

        findViewById(R.id.btnAgregarColegiatura).setVisibility(Permissions.has("COLEGIATURAS_CREAR") ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnAgregarColegiatura).setOnClickListener(v ->
                startActivity(new Intent(this, ColegiaturaFormActivity.class)));
        View revisionPagos = findViewById(R.id.btnRevisionPagos);
        revisionPagos.setVisibility(Permissions.hasRole("ADMIN") && Permissions.has(Permissions.COLEGIATURAS_CAMBIAR_ESTADO) ? View.VISIBLE : View.GONE);
        revisionPagos.setOnClickListener(v -> startActivity(new Intent(this, RevisionPagosActivity.class)));

        btnAnterior.setOnClickListener(v -> {
            if (pagina > 0) {
                pagina--;
                cargarColegiaturas();
            }
        });

        btnSiguiente.setOnClickListener(v -> {
            if (pagina + 1 < totalPaginas) {
                pagina++;
                cargarColegiaturas();
            }
        });

        actEstudiante.setOnItemClickListener((parent, view, position, id) -> {
            // El elemento 0 es "Todos los estudiantes"; los estudiantes empiezan en 1.
            if (position <= 0) {
                selectedEstudianteId = -1L;
            } else {
                int studentIndex = position - 1;
                if (studentIndex < estudiantes.size()) {
                    selectedEstudianteId = estudiantes.get(studentIndex).id;
                }
            }
        });

        toggleActivo.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                pagina = 0;
                cargarColegiaturas();
            }
        });
    }

    private void setFiltersExpanded(boolean expanded) {
        filtersExpanded = expanded;
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

    private void cargarEstudiantes() {
        setLoading(true);
        service.listarEstudiantesActivos().enqueue(new Callback<List<EstudianteResumenResponse>>() {
            @Override
            public void onResponse(Call<List<EstudianteResumenResponse>> call, Response<List<EstudianteResumenResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    estudiantes.clear();
                    estudiantes.addAll(response.body());
                    List<String> nombres = new ArrayList<>();
                    nombres.add(getString(R.string.colegiatura_todos_estudiantes));
                    for (EstudianteResumenResponse estudiante : estudiantes) {
                        nombres.add(estudiante.getDisplayName());
                    }
                    ArrayAdapter<String> adapterEstudiantes = new ArrayAdapter<>(
                            ColegiaturaListActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            nombres);
                    actEstudiante.setAdapter(adapterEstudiantes);
                    actEstudiante.setText(adapterEstudiantes.getItem(0), false);
                    selectedEstudianteId = -1L;
                    setLoading(false);
                } else {
                    setLoading(false);
                    Toast.makeText(ColegiaturaListActivity.this, leerError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<EstudianteResumenResponse>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ColegiaturaListActivity.this, getString(R.string.colegiatura_error_network), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cargarColegiaturas() {
        setLoading(true);
        Long estudianteId = selectedEstudianteId > 0 ? selectedEstudianteId : null;
        Integer cicloAnio = parseInteger(edtCiclo.getText() == null ? null : edtCiclo.getText().toString().trim());
        String estado = valorEstado();
        Boolean activo = valorActivo();
        String concepto = edtConcepto.getText() == null ? null : edtConcepto.getText().toString().trim();
        if (concepto != null && concepto.isEmpty()) concepto = null;

        service.listar(estudianteId, cicloAnio, estado, activo, concepto, pagina, PAGE_SIZE)
                .enqueue(new Callback<PageResponse<ColegiaturaResponse>>() {
                    @Override
                    public void onResponse(Call<PageResponse<ColegiaturaResponse>> call,
                                           Response<PageResponse<ColegiaturaResponse>> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            PageResponse<ColegiaturaResponse> page = response.body();
                            adapter.setItems(page.content);
                            totalPaginas = page.totalPages;
                            actualizarEstadoLista(page);
                            actualizarPaginacion(page);
                        } else {
                            adapter.setItems(new ArrayList<>());
                            mostrarError(leerError(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<PageResponse<ColegiaturaResponse>> call, Throwable t) {
                        setLoading(false);
                        adapter.setItems(new ArrayList<>());
                        estadoLista.setVisibility(View.VISIBLE);
                        estadoLista.setText(R.string.colegiatura_error_network);
                        paginaInfo.setText("");
                    }
                });
    }

    private void actualizarEstadoLista(PageResponse<ColegiaturaResponse> page) {
        boolean empty = page.content == null || page.content.isEmpty();
        estadoLista.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) estadoLista.setText(R.string.colegiatura_empty);
    }

    private void mostrarError(String mensaje) {
        estadoLista.setVisibility(View.VISIBLE);
        estadoLista.setText(mensaje);
    }

    private void actualizarPaginacion(PageResponse<ColegiaturaResponse> page) {
        int total = Math.max(page.totalPages, 1);
        paginaInfo.setText(getString(R.string.colegiatura_pagina_info, pagina + 1, total));
        btnAnterior.setEnabled(!page.first);
        btnSiguiente.setEnabled(!page.last);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnBuscarColegiatura).setEnabled(!loading);
        findViewById(R.id.btnAgregarColegiatura).setEnabled(!loading);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String valorEstado() {
        String value = actEstado.getText() == null ? "" : actEstado.getText().toString().trim();
        if (value.isEmpty() || value.equals(getString(R.string.colegiatura_todos_estados))) return null;
        return value;
    }

    private Boolean valorActivo() {
        int checked = toggleActivo.getCheckedButtonId();
        if (checked == View.NO_ID || checked == R.id.btnActivoTodos) return null;
        if (checked == R.id.btnActivoSi) return true;
        if (checked == R.id.btnActivoNo) return false;
        return null;
    }

    private String leerError(Response<?> response) {
        String authorizationMessage = ApiResponses.authorizationMessage(this, response);
        if (authorizationMessage != null) return authorizationMessage;
        try {
            if (response.errorBody() != null) {
                String texto = response.errorBody().string();
                if (texto != null && !texto.trim().isEmpty()) return texto.trim();
            }
        } catch (IOException ignored) {
        }

        if (response.code() == 400) return getString(R.string.colegiatura_error_bad_request);
        if (response.code() == 404) return getString(R.string.colegiatura_error_not_found);
        if (response.code() == 409) return getString(R.string.colegiatura_error_conflict);
        return getString(R.string.colegiatura_error_server);
    }

    @Override
    public void onEdit(ColegiaturaResponse item) {
        Intent intent = new Intent(this, ColegiaturaFormActivity.class);
        intent.putExtra("colegiaturaId", item.id);
        startActivity(intent);
    }

    @Override
    public void onPay(ColegiaturaResponse item) {
        Intent intent = new Intent(this, PagoColegiaturaActivity.class);
        intent.putExtra("colegiaturaId", item.id);
        startActivity(intent);
    }

    @Override
    public void onChangeState(ColegiaturaResponse item) {
        SgauDialog.confirmState(this, false,
                "la colegiatura \"" + item.concepto + "\"", () -> inactivarColegiatura(item.id));
    }

    private void inactivarColegiatura(long id) {
        setLoading(true);
        service.cambiarEstado(id, new gt.com.ro.devumgapp.colegiatura.dto.EstadoRequest(false))
                .enqueue(new Callback<ColegiaturaResponse>() {
                    @Override
                    public void onResponse(Call<ColegiaturaResponse> call, Response<ColegiaturaResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            UiNotifier.success(ColegiaturaListActivity.this,
                                    R.string.colegiatura_inactivada_exito);
                            cargarColegiaturas();
                        } else {
                            UiNotifier.error(ColegiaturaListActivity.this, leerError(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<ColegiaturaResponse> call, Throwable t) {
                        setLoading(false);
                        UiNotifier.error(ColegiaturaListActivity.this,
                                getString(R.string.colegiatura_error_network));
                    }
                });
    }
}
