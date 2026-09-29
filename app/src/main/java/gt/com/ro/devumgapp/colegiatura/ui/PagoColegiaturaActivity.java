package gt.com.ro.devumgapp.colegiatura.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaResponse;
import gt.com.ro.devumgapp.colegiatura.dto.PagoRequest;
import gt.com.ro.devumgapp.colegiatura.network.ColegiaturaApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.network.ApiResponses;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PagoColegiaturaActivity extends AppCompatActivity {
    private LinearProgressIndicator progress;
    private TextView txtResumen;
    private TextInputEditText edtMonto, edtFecha, edtObservaciones;
    private long colegiaturaId;
    private ColegiaturaApiService service;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.requireAll(this, Permissions.COLEGIATURAS_LEER, "COLEGIATURAS_REGISTRAR_PAGO")) return;
        setContentView(R.layout.activity_colegiatura_pago);
        colegiaturaId = getIntent().getLongExtra("colegiaturaId", -1L);
        service = RetrofitClient.getClient().create(ColegiaturaApiService.class);
        bindViews();
        cargarColegiatura();
    }

    private void bindViews() {
        progress = findViewById(R.id.progressPagoColegiatura);
        txtResumen = findViewById(R.id.txtPagoColegiaturaResumen);
        edtMonto = findViewById(R.id.edtPagoMonto);
        edtFecha = findViewById(R.id.edtPagoFecha);
        edtObservaciones = findViewById(R.id.edtPagoObservaciones);
        ((MaterialToolbar) findViewById(R.id.toolbarPagoColegiatura)).setNavigationOnClickListener(v -> finish());
        edtFecha.setOnClickListener(v -> seleccionarFecha());
        findViewById(R.id.btnRegistrarPago).setOnClickListener(v -> registrarPago());
    }

    private void cargarColegiatura() {
        if (colegiaturaId <= 0) { finish(); return; }
        setLoading(true);
        service.obtener(colegiaturaId).enqueue(new Callback<ColegiaturaResponse>() {
            @Override public void onResponse(Call<ColegiaturaResponse> call, Response<ColegiaturaResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ColegiaturaResponse c = response.body();
                    txtResumen.setText(getString(R.string.colegiatura_pago_resumen,
                            c.concepto, c.montoTotal, c.montoPagado, c.saldoPendiente, c.estado));
                    edtFecha.setText(fechaHoy());
                } else Toast.makeText(PagoColegiaturaActivity.this, leerError(response), Toast.LENGTH_LONG).show();
            }
            @Override public void onFailure(Call<ColegiaturaResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(PagoColegiaturaActivity.this, R.string.colegiatura_error_network, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registrarPago() {
        Double monto = parseDouble();
        String fecha = textOf(edtFecha);
        if (monto == null || monto <= 0 || fecha.isEmpty()) {
            Toast.makeText(this, R.string.colegiatura_pago_datos_invalidos, Toast.LENGTH_LONG).show();
            return;
        }
        SgauDialog.confirm(this, R.drawable.ic_payment,
                getString(R.string.dialog_title_payment),
                getString(R.string.dialog_message_payment, String.format(Locale.US, "%.2f", monto)),
                getString(R.string.dialog_register), this::enviarPago);
    }

    private void enviarPago() {
        if (progress.getVisibility() == View.VISIBLE) return;
        Double monto = parseDouble();
        String fecha = textOf(edtFecha);
        PagoRequest request = new PagoRequest(monto, fecha, textOf(edtObservaciones));
        setLoading(true);
        service.registrarPago(colegiaturaId, request).enqueue(new Callback<ColegiaturaResponse>() {
            @Override public void onResponse(Call<ColegiaturaResponse> call, Response<ColegiaturaResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ColegiaturaResponse c = response.body();
                    UiNotifier.success(PagoColegiaturaActivity.this,
                            getString(R.string.colegiatura_pago_exito, c.estado, c.saldoPendiente));
                    finish();
                } else UiNotifier.error(PagoColegiaturaActivity.this, leerError(response));
            }
            @Override public void onFailure(Call<ColegiaturaResponse> call, Throwable t) {
                setLoading(false);
                UiNotifier.error(PagoColegiaturaActivity.this, getString(R.string.colegiatura_error_network));
            }
        });
    }

    private void seleccionarFecha() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                edtFecha.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private String fechaHoy() { Calendar c=Calendar.getInstance(); return String.format(Locale.US, "%04d-%02d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH)); }
    private String textOf(TextInputEditText e) { return e.getText()==null?"":e.getText().toString().trim(); }
    private Double parseDouble() { try { return Double.valueOf(textOf(edtMonto)); } catch(Exception e){ return null; } }
    private void setLoading(boolean b) { progress.setVisibility(b?View.VISIBLE:View.GONE); findViewById(R.id.btnRegistrarPago).setEnabled(!b); }
    private String leerError(Response<?> response) {
        String authorizationMessage = ApiResponses.authorizationMessage(this, response);
        if (authorizationMessage != null) return authorizationMessage;
        try { if (response.errorBody()!=null) { String texto=response.errorBody().string(); if (texto!=null && !texto.trim().isEmpty()) return texto.trim(); } } catch(IOException ignored){}
        if(response.code()==400) return getString(R.string.colegiatura_error_bad_request);
        if(response.code()==404) return getString(R.string.colegiatura_error_not_found);
        if(response.code()==409) return getString(R.string.colegiatura_error_conflict);
        return getString(R.string.colegiatura_error_server);
    }
}
