package gt.com.ro.devumgapp.colegiatura.ui;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.DecisionPago;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.Pago;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.PagoRevision;
import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
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
    private AcademicoApiService pagosApi;

    private final List<EstudianteResumenResponse> estudiantes = new ArrayList<>();
    private long selectedEstudianteId = -1L;
    private int pagina = 0;
    private int totalPaginas = 0;
    private int listRequestGeneration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "COLEGIATURAS_LEER")) return;
        setContentView(R.layout.activity_colegiatura_list);

        service = RetrofitClient.getClient().create(ColegiaturaApiService.class);
        bindViews();
        configurarVista();
        cargarEstudiantes();
        long paymentId = getIntent().getLongExtra("notificationPaymentId", -1L);
        if (paymentId > 0 && Permissions.hasRole("ADMIN")
                && Permissions.has(Permissions.COLEGIATURAS_CAMBIAR_ESTADO)) {
            cargarColegiaturaDeReporte(paymentId);
        } else {
            cargarColegiaturas();
        }
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
        pagosApi = RetrofitClient.getClient().create(AcademicoApiService.class);

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
        final int requestGeneration = ++listRequestGeneration;
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
                            adapter.setPendingPayments(new ArrayList<>());
                            if (Permissions.hasRole("ADMIN") && Permissions.has(Permissions.COLEGIATURAS_CAMBIAR_ESTADO)) {
                                cargarReportesPendientes(requestGeneration, 0, new ArrayList<>());
                            }
                            totalPaginas = page.totalPages;
                            actualizarEstadoLista(page);
                            actualizarPaginacion(page);
                        } else {
                            adapter.setItems(new ArrayList<>());
                            adapter.setPendingPayments(new ArrayList<>());
                            mostrarError(leerError(response));
                        }
                    }

                    @Override
                    public void onFailure(Call<PageResponse<ColegiaturaResponse>> call, Throwable t) {
                        setLoading(false);
                        adapter.setItems(new ArrayList<>());
                        adapter.setPendingPayments(new ArrayList<>());
                        estadoLista.setVisibility(View.VISIBLE);
                        estadoLista.setText(R.string.colegiatura_error_network);
                        paginaInfo.setText("");
                    }
                });
    }

    private void cargarReportesPendientes(int generation, int page, List<PagoRevision> reports) {
        pagosApi.buscarPagosRevision("PENDIENTE", null, page, 100)
                .enqueue(new Callback<PageResponse<PagoRevision>>() {
                    @Override public void onResponse(Call<PageResponse<PagoRevision>> call,
                                                     Response<PageResponse<PagoRevision>> response) {
                        if (generation != listRequestGeneration) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            adapter.setPendingPayments(reports);
                            return;
                        }
                        PageResponse<PagoRevision> result = response.body();
                        if (result.content != null) reports.addAll(result.content);
                        if (page + 1 < result.totalPages) cargarReportesPendientes(generation, page + 1, reports);
                        else adapter.setPendingPayments(reports);
                    }
                    @Override public void onFailure(Call<PageResponse<PagoRevision>> call, Throwable error) {
                        if (generation == listRequestGeneration) adapter.setPendingPayments(reports);
                    }
                });
    }

    private void cargarColegiaturaDeReporte(long paymentId) {
        int generation = ++listRequestGeneration;
        setLoading(true);
        pagosApi.detallePagoRevision(paymentId).enqueue(new Callback<PagoRevision>() {
            @Override public void onResponse(Call<PagoRevision> call, Response<PagoRevision> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setLoading(false);
                    mostrarError("No se encontró el reporte de pago.");
                    return;
                }
                service.obtener(response.body().colegiaturaId).enqueue(new Callback<ColegiaturaResponse>() {
                    @Override public void onResponse(Call<ColegiaturaResponse> detailCall, Response<ColegiaturaResponse> detail) {
                        setLoading(false);
                        if (!detail.isSuccessful() || detail.body() == null) {
                            mostrarError("No se encontró la colegiatura del reporte.");
                            return;
                        }
                        List<ColegiaturaResponse> one = new ArrayList<>();
                        one.add(detail.body());
                        adapter.setItems(one);
                        adapter.setPendingPayments(new ArrayList<>());
                        totalPaginas = 1;
                        estadoLista.setVisibility(View.GONE);
                        paginaInfo.setText("Reporte de pago");
                        btnAnterior.setEnabled(false);
                        btnSiguiente.setEnabled(false);
                        cargarReportesPendientes(generation, 0, new ArrayList<>());
                    }
                    @Override public void onFailure(Call<ColegiaturaResponse> detailCall, Throwable error) {
                        setLoading(false);
                        mostrarError("No se pudo cargar la colegiatura del reporte.");
                    }
                });
            }
            @Override public void onFailure(Call<PagoRevision> call, Throwable error) {
                setLoading(false);
                mostrarError("No se pudo cargar el reporte de pago.");
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
    public void onReviewPayment(PagoRevision payment, String decision) {
        if (!Permissions.hasRole("ADMIN") || !Permissions.has(Permissions.COLEGIATURAS_CAMBIAR_ESTADO)) {
            Toast.makeText(this, "No tienes permiso para revisar reportes de pago.", Toast.LENGTH_LONG).show();
            return;
        }
        if ("RECHAZADO".equals(decision)) {
            EditText reason = new EditText(this);
            reason.setHint("Motivo obligatorio para el estudiante");
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Rechazar pago")
                    .setMessage("Indica por qué se rechaza este reporte.")
                    .setView(reason)
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Rechazar", null)
                    .create();
            dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String value = reason.getText() == null ? "" : reason.getText().toString().trim();
                if (value.isEmpty()) {
                    reason.setError("El motivo es obligatorio");
                    return;
                }
                dialog.dismiss();
                enviarDecisionPago(payment, decision, value);
            }));
            dialog.show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Aceptar pago")
                .setMessage("Se aplicarán Q" + String.format(java.util.Locale.getDefault(), "%.2f", payment.monto)
                        + " al saldo de " + payment.estudianteNombre + ".")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aceptar", (dialog, which) -> enviarDecisionPago(payment, decision, null))
                .show();
    }

    private void enviarDecisionPago(PagoRevision payment, String decision, String reason) {
        setLoading(true);
        pagosApi.revisarPago(payment.id, new DecisionPago(decision, reason)).enqueue(new Callback<Pago>() {
            @Override public void onResponse(Call<Pago> call, Response<Pago> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null
                        && !"PENDIENTE".equalsIgnoreCase(response.body().estado)) {
                    Toast.makeText(ColegiaturaListActivity.this,
                            "Pago " + ("APROBADO".equalsIgnoreCase(response.body().estado) ? "aceptado" : "rechazado") + ".",
                            Toast.LENGTH_LONG).show();
                    cargarColegiaturas();
                } else {
                    Toast.makeText(ColegiaturaListActivity.this,
                            response.isSuccessful() ? "El servidor no confirmó la revisión del pago." : leerError(response),
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<Pago> call, Throwable error) {
                setLoading(false);
                Toast.makeText(ColegiaturaListActivity.this, "No se pudo revisar el pago. Actualiza la lista e inténtalo de nuevo.", Toast.LENGTH_LONG).show();
            }
        });
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
