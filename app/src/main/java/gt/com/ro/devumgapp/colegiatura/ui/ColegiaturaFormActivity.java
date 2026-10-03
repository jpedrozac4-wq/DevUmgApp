package gt.com.ro.devumgapp.colegiatura.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaRequest;
import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaResponse;
import gt.com.ro.devumgapp.colegiatura.dto.ConfiguracionColegiaturaResponse;
import gt.com.ro.devumgapp.colegiatura.network.ColegiaturaApiService;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.network.ApiResponses;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ColegiaturaFormActivity extends AppCompatActivity {

    private static final String TAG = "ColegiaturaHttp";

    private LinearProgressIndicator progress;
    private MaterialAutoCompleteTextView actEstudiante;
    private TextInputEditText edtCiclo, edtConcepto, edtMontoTotal, edtFechaEmision, edtFechaVencimiento;
    private TextInputLayout tilEstudiante, tilCiclo, tilConcepto, tilMonto, tilFechaEmision, tilFechaVencimiento;
    private final List<EstudianteResumenResponse> estudiantes = new ArrayList<>();
    private long estudianteId = -1L;
    private long colegiaturaId = -1L;
    private ColegiaturaApiService service;
    private Call<ConfiguracionColegiaturaResponse> configuracionCall;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String permission = getIntent().hasExtra("colegiaturaId") ? "COLEGIATURAS_EDITAR" : "COLEGIATURAS_CREAR";
        if (!Permissions.requireAll(this, Permissions.COLEGIATURAS_LEER, permission)) return;
        setContentView(R.layout.activity_colegiatura_form);
        service = RetrofitClient.getClient().create(ColegiaturaApiService.class);
        colegiaturaId = getIntent().getLongExtra("colegiaturaId", -1L);
        bindViews();
        configurarVista();
        if (colegiaturaId <= 0) edtFechaEmision.setText(LocalDate.now().toString());
        cargarEstudiantes();
    }

    private void bindViews() {
        progress = findViewById(R.id.progressColegiaturaForm);
        actEstudiante = findViewById(R.id.actColegiaturaFormEstudiante);
        edtCiclo = findViewById(R.id.edtColegiaturaFormCiclo);
        edtConcepto = findViewById(R.id.edtColegiaturaFormConcepto);
        edtMontoTotal = findViewById(R.id.edtColegiaturaFormMonto);
        edtFechaEmision = findViewById(R.id.edtColegiaturaFormFechaEmision);
        edtFechaVencimiento = findViewById(R.id.edtColegiaturaFormFechaVencimiento);
        tilEstudiante = findViewById(R.id.tilColegiaturaEstudiante);
        tilCiclo = findViewById(R.id.tilColegiaturaCiclo);
        tilConcepto = findViewById(R.id.tilColegiaturaConcepto);
        tilMonto = findViewById(R.id.tilColegiaturaMonto);
        tilFechaEmision = findViewById(R.id.tilColegiaturaFechaEmision);
        tilFechaVencimiento = findViewById(R.id.tilColegiaturaFechaVencimiento);

        MaterialToolbar toolbar = findViewById(R.id.toolbarColegiaturaForm);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void configurarVista() {
        edtFechaEmision.setOnClickListener(v -> seleccionarFecha(edtFechaEmision));
        edtFechaVencimiento.setOnClickListener(v -> seleccionarFecha(edtFechaVencimiento));
        findViewById(R.id.btnGuardarColegiatura).setOnClickListener(v -> guardar());
    }

    private void cargarEstudiantes() {
        setLoading(true);
        service.listarEstudiantesActivos().enqueue(new Callback<List<EstudianteResumenResponse>>() {
            @Override
            public void onResponse(Call<List<EstudianteResumenResponse>> call, Response<List<EstudianteResumenResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    estudiantes.clear();
                    estudiantes.addAll(response.body());
                    List<String> opciones = new ArrayList<>();
                    for (EstudianteResumenResponse e : estudiantes) opciones.add(e.getDisplayName());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ColegiaturaFormActivity.this,
                            android.R.layout.simple_dropdown_item_1line, opciones);
                    actEstudiante.setAdapter(adapter);
                    actEstudiante.setOnItemClickListener((parent, view, position, id) -> {
                        if (position >= 0 && position < estudiantes.size()) {
                            estudianteId = estudiantes.get(position).id;
                            if (colegiaturaId <= 0) cargarConfiguracionEstudiante();
                        }
                    });
                    if (colegiaturaId <= 0) setLoading(false);
                    else cargarColegiatura();
                } else {
                    setLoading(false);
                    Toast.makeText(ColegiaturaFormActivity.this, leerError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<EstudianteResumenResponse>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ColegiaturaFormActivity.this, R.string.colegiatura_error_network, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void cargarConfiguracionEstudiante() {
        if (configuracionCall != null) configuracionCall.cancel();
        setLoading(true);
        configuracionCall = service.obtenerConfiguracionEstudiante(estudianteId);
        configuracionCall.enqueue(new Callback<ConfiguracionColegiaturaResponse>() {
            @Override public void onResponse(Call<ConfiguracionColegiaturaResponse> call,
                    Response<ConfiguracionColegiaturaResponse> response) {
                if (call.isCanceled()) return;
                configuracionCall = null;
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    limpiarConfiguracionAutomatica();
                    UiNotifier.error(ColegiaturaFormActivity.this, leerError(response));
                    return;
                }
                aplicarConfiguracion(response.body());
            }

            @Override public void onFailure(Call<ConfiguracionColegiaturaResponse> call, Throwable error) {
                if (call.isCanceled()) return;
                configuracionCall = null;
                setLoading(false);
                limpiarConfiguracionAutomatica();
                UiNotifier.error(ColegiaturaFormActivity.this, getString(R.string.colegiatura_error_network));
            }
        });
    }

    private void aplicarConfiguracion(ConfiguracionColegiaturaResponse config) {
        LocalDate emision;
        try { emision = LocalDate.parse(config.fechaEmision); }
        catch (Exception ignored) { emision = LocalDate.now(); }
        edtCiclo.setText(String.valueOf(config.cicloAnio));
        edtConcepto.setText("Colegiatura - " + (config.carreraNombre == null ? "Carrera" : config.carreraNombre));
        edtMontoTotal.setText(config.mensualidad == null ? "" : config.mensualidad.toPlainString());
        edtFechaEmision.setText(emision.toString());
        if (config.diaVencimiento != null) {
            YearMonth mes = YearMonth.from(emision);
            int dia = Math.min(config.diaVencimiento, mes.lengthOfMonth());
            LocalDate vencimiento = mes.atDay(dia);
            if (vencimiento.isBefore(emision)) {
                mes = mes.plusMonths(1);
                vencimiento = mes.atDay(Math.min(config.diaVencimiento, mes.lengthOfMonth()));
            }
            edtFechaVencimiento.setText(vencimiento.toString());
        }
    }

    private void limpiarConfiguracionAutomatica() {
        edtCiclo.setText("");
        edtConcepto.setText("");
        edtMontoTotal.setText("");
        edtFechaEmision.setText(LocalDate.now().toString());
        edtFechaVencimiento.setText("");
    }

    private void cargarColegiatura() {
        if (colegiaturaId <= 0) return;
        setLoading(true);
        service.obtener(colegiaturaId).enqueue(new Callback<ColegiaturaResponse>() {
            @Override
            public void onResponse(Call<ColegiaturaResponse> call, Response<ColegiaturaResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) llenarFormulario(response.body());
                else Toast.makeText(ColegiaturaFormActivity.this, leerError(response), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ColegiaturaResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ColegiaturaFormActivity.this, R.string.colegiatura_error_network, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void llenarFormulario(ColegiaturaResponse item) {
        estudianteId = item.estudianteId;
        seleccionarEstudiante(item.estudianteId);
        edtCiclo.setText(String.valueOf(item.cicloAnio));
        edtConcepto.setText(item.concepto);
        edtMontoTotal.setText(String.format(Locale.US, "%.2f", item.montoTotal));
        edtFechaEmision.setText(item.fechaEmision);
        edtFechaVencimiento.setText(item.fechaVencimiento);
    }

    private void seleccionarEstudiante(long id) {
        for (int i = 0; i < estudiantes.size(); i++) {
            if (estudiantes.get(i).id == id) {
                actEstudiante.setText(estudiantes.get(i).getDisplayName(), false);
                return;
            }
        }
    }

    private void guardar() {
        clearFieldErrors();
        if (estudianteId <= 0) {
            tilEstudiante.setError(getString(R.string.colegiatura_form_estudiante_requerido));
            return;
        }

        Integer ciclo = parseInt(edtCiclo);
        BigDecimal monto = parseDecimal(edtMontoTotal);
        String concepto = textOf(edtConcepto);
        String fechaEmision = textOf(edtFechaEmision);
        String fechaVencimiento = textOf(edtFechaVencimiento);
        boolean valid = true;
        if (ciclo == null || ciclo < 2020 || ciclo > 2100) { tilCiclo.setError("Ingresa un ciclo entre 2020 y 2100."); valid = false; }
        if (concepto.isEmpty()) { tilConcepto.setError("El concepto es obligatorio."); valid = false; }
        else if (concepto.length() > 120) { tilConcepto.setError("El concepto no puede superar 120 caracteres."); valid = false; }
        if (monto == null || monto.signum() <= 0) { tilMonto.setError("Ingresa un monto mayor que cero."); valid = false; }
        LocalDate emission = parseDate(fechaEmision), due = parseDate(fechaVencimiento);
        if (emission == null) { tilFechaEmision.setError("Selecciona una fecha de emisión válida."); valid = false; }
        if (due == null) { tilFechaVencimiento.setError("Selecciona una fecha de vencimiento válida."); valid = false; }
        if (emission != null && due != null && due.isBefore(emission)) { tilFechaVencimiento.setError("El vencimiento no puede ser anterior a la emisión."); valid = false; }
        if (!valid) return;

        SgauDialog.confirmSave(this, colegiaturaId > 0,
                "la colegiatura \"" + concepto + "\"", this::enviarColegiatura);
    }

    private void enviarColegiatura() {
        if (progress.getVisibility() == View.VISIBLE) return;
        Integer ciclo = parseInt(edtCiclo);
        BigDecimal monto = parseDecimal(edtMontoTotal);
        String concepto = textOf(edtConcepto);
        String fechaEmision = textOf(edtFechaEmision);
        String fechaVencimiento = textOf(edtFechaVencimiento);

        ColegiaturaRequest request = new ColegiaturaRequest(
                estudianteId, ciclo, concepto, monto, fechaEmision, fechaVencimiento);
        String method = colegiaturaId > 0 ? "PUT" : "POST";
        String path = colegiaturaId > 0 ? "/api/colegiaturas/" + colegiaturaId : "/api/colegiaturas";
        Log.d(TAG, method + " " + path + " request=" + new Gson().toJson(request));

        setLoading(true);
        Call<ColegiaturaResponse> call = colegiaturaId > 0
                ? service.actualizar(colegiaturaId, request)
                : service.crear(request);

        call.enqueue(new Callback<ColegiaturaResponse>() {
            @Override
            public void onResponse(Call<ColegiaturaResponse> call, Response<ColegiaturaResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, method + " " + path + " response=" + response.code() + " " + new Gson().toJson(response.body()));
                    UiNotifier.success(ColegiaturaFormActivity.this,
                            R.string.colegiatura_form_guardado);
                    finish();
                } else {
                    String message = aplicarError(response);
                    UiNotifier.error(ColegiaturaFormActivity.this, message);
                }
            }

            @Override
            public void onFailure(Call<ColegiaturaResponse> call, Throwable t) {
                setLoading(false);
                UiNotifier.error(ColegiaturaFormActivity.this, getString(R.string.colegiatura_error_network));
            }
        });
    }

    private void seleccionarFecha(TextInputEditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                target.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override protected void onDestroy() {
        if (configuracionCall != null) configuracionCall.cancel();
        super.onDestroy();
    }

    private String textOf(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
    private Integer parseInt(TextInputEditText e) { try { return Integer.valueOf(textOf(e)); } catch (Exception ex) { return null; } }
    private BigDecimal parseDecimal(TextInputEditText e) { try { return new BigDecimal(textOf(e)); } catch (Exception ex) { return null; } }
    private LocalDate parseDate(String value) { try { return LocalDate.parse(value); } catch (Exception ex) { return null; } }

    private void clearFieldErrors() {
        tilEstudiante.setError(null); tilCiclo.setError(null); tilConcepto.setError(null);
        tilMonto.setError(null); tilFechaEmision.setError(null); tilFechaVencimiento.setError(null);
    }

    private String aplicarError(Response<?> response) {
        String authorizationMessage = ApiResponses.authorizationMessage(this, response);
        String raw = "";
        try { if (response.errorBody() != null) raw = response.errorBody().string(); } catch (IOException ignored) { }
        Log.w(TAG, (colegiaturaId > 0 ? "PUT /api/colegiaturas/" + colegiaturaId : "POST /api/colegiaturas")
                + " response=" + response.code() + " " + raw);
        if (authorizationMessage != null) return authorizationMessage;
        try {
            JsonObject body = new JsonParser().parse(raw).getAsJsonObject();
            if (body.has("fieldErrors") && body.get("fieldErrors").isJsonObject()) {
                for (java.util.Map.Entry<String, JsonElement> entry : body.getAsJsonObject("fieldErrors").entrySet())
                    setFieldError(entry.getKey(), entry.getValue().getAsString());
            }
            String message = body.has("message") ? body.get("message").getAsString() : "";
            String normalized = message.toLowerCase(Locale.ROOT);
            if (response.code() == 404 || normalized.contains("estudiante"))
                tilEstudiante.setError(message.isEmpty() ? "El estudiante seleccionado no existe o no está disponible." : message);
            if (response.code() == 409 || normalized.contains("duplicad"))
                tilConcepto.setError(message.isEmpty() ? "Ya existe un cobro activo con este concepto para el estudiante y ciclo." : message);
            if (normalized.contains("ciclo")) tilCiclo.setError(message);
            if (normalized.contains("fecha")) tilFechaVencimiento.setError(message);
            if (normalized.contains("concepto")) tilConcepto.setError(message);
            if (normalized.contains("monto")) tilMonto.setError(message);
            if (!message.isEmpty()) return message;
        } catch (Exception ignored) { }
        if (response.code() == 404) { tilEstudiante.setError("El estudiante seleccionado no existe o no está disponible."); return getString(R.string.colegiatura_error_not_found); }
        if (response.code() == 409) { tilConcepto.setError("Ya existe un cobro activo con este concepto para el estudiante y ciclo."); return getString(R.string.colegiatura_error_conflict); }
        return response.code() == 400 ? getString(R.string.colegiatura_error_bad_request) : getString(R.string.colegiatura_error_server);
    }

    private void setFieldError(String field, String message) {
        if ("estudianteId".equals(field)) tilEstudiante.setError(message);
        else if ("cicloAnio".equals(field)) tilCiclo.setError(message);
        else if ("concepto".equals(field)) tilConcepto.setError(message);
        else if ("montoTotal".equals(field)) tilMonto.setError(message);
        else if ("fechaEmision".equals(field)) tilFechaEmision.setError(message);
        else if ("fechaVencimiento".equals(field)) tilFechaVencimiento.setError(message);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnGuardarColegiatura).setEnabled(!loading);
    }

    private String leerError(Response<?> response) {
        String authorizationMessage = ApiResponses.authorizationMessage(this, response);
        if (authorizationMessage != null) return authorizationMessage;
        try {
            if (response.errorBody() != null) {
                String texto = response.errorBody().string();
                if (texto != null && !texto.trim().isEmpty()) return texto.trim();
            }
        } catch (IOException ignored) { }
        if (response.code() == 400) return getString(R.string.colegiatura_error_bad_request);
        if (response.code() == 404) return getString(R.string.colegiatura_error_not_found);
        if (response.code() == 409) return getString(R.string.colegiatura_error_conflict);
        return getString(R.string.colegiatura_error_server);
    }
}
