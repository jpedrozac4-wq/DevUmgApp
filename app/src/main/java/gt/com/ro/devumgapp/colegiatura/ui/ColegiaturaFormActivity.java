package gt.com.ro.devumgapp.colegiatura.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaRequest;
import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaResponse;
import gt.com.ro.devumgapp.colegiatura.network.ColegiaturaApiService;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ColegiaturaFormActivity extends AppCompatActivity {

    private LinearProgressIndicator progress;
    private MaterialAutoCompleteTextView actEstudiante;
    private TextInputEditText edtCiclo, edtConcepto, edtMontoTotal, edtFechaEmision, edtFechaVencimiento;
    private final List<EstudianteResumenResponse> estudiantes = new ArrayList<>();
    private long estudianteId = -1L;
    private long colegiaturaId = -1L;
    private ColegiaturaApiService service;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colegiatura_form);
        service = RetrofitClient.getClient().create(ColegiaturaApiService.class);
        bindViews();
        configurarVista();
        cargarEstudiantes();

        colegiaturaId = getIntent().getLongExtra("colegiaturaId", -1L);
    }

    private void bindViews() {
        progress = findViewById(R.id.progressColegiaturaForm);
        actEstudiante = findViewById(R.id.actColegiaturaFormEstudiante);
        edtCiclo = findViewById(R.id.edtColegiaturaFormCiclo);
        edtConcepto = findViewById(R.id.edtColegiaturaFormConcepto);
        edtMontoTotal = findViewById(R.id.edtColegiaturaFormMonto);
        edtFechaEmision = findViewById(R.id.edtColegiaturaFormFechaEmision);
        edtFechaVencimiento = findViewById(R.id.edtColegiaturaFormFechaVencimiento);

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
        if (estudianteId <= 0) {
            Toast.makeText(this, R.string.colegiatura_form_estudiante_requerido, Toast.LENGTH_SHORT).show();
            return;
        }

        Integer ciclo = parseInt(edtCiclo);
        Double monto = parseDouble(edtMontoTotal);
        String concepto = textOf(edtConcepto);
        String fechaEmision = textOf(edtFechaEmision);
        String fechaVencimiento = textOf(edtFechaVencimiento);

        if (ciclo == null || monto == null || monto <= 0 || concepto.isEmpty() || fechaEmision.isEmpty() || fechaVencimiento.isEmpty()) {
            Toast.makeText(this, R.string.colegiatura_form_datos_invalidos, Toast.LENGTH_LONG).show();
            return;
        }

        ColegiaturaRequest request = new ColegiaturaRequest(
                estudianteId, ciclo, concepto, monto, fechaEmision, fechaVencimiento);

        setLoading(true);
        Call<ColegiaturaResponse> call = colegiaturaId > 0
                ? service.actualizar(colegiaturaId, request)
                : service.crear(request);

        call.enqueue(new Callback<ColegiaturaResponse>() {
            @Override
            public void onResponse(Call<ColegiaturaResponse> call, Response<ColegiaturaResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ColegiaturaFormActivity.this,
                            R.string.colegiatura_form_guardado, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ColegiaturaFormActivity.this, leerError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ColegiaturaResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ColegiaturaFormActivity.this, R.string.colegiatura_error_network, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void seleccionarFecha(TextInputEditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                target.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String textOf(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
    private Integer parseInt(TextInputEditText e) { try { return Integer.valueOf(textOf(e)); } catch (Exception ex) { return null; } }
    private Double parseDouble(TextInputEditText e) { try { return Double.valueOf(textOf(e)); } catch (Exception ex) { return null; } }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnGuardarColegiatura).setEnabled(!loading);
    }

    private String leerError(Response<?> response) {
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
