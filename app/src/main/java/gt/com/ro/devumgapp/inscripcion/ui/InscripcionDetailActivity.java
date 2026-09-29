package gt.com.ro.devumgapp.inscripcion.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.carrera.dto.CarreraResponse;
import gt.com.ro.devumgapp.carrera.network.CarreraApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.curso.dto.CursoResponse;
import gt.com.ro.devumgapp.curso.network.CursoApiService;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import gt.com.ro.devumgapp.inscripcion.dto.AnulacionRequest;
import gt.com.ro.devumgapp.inscripcion.dto.InscripcionResponse;
import gt.com.ro.devumgapp.inscripcion.network.InscripcionApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InscripcionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_INSCRIPCION_ID = "inscripcionId";

    private MaterialToolbar toolbar;
    private LinearProgressIndicator progressBar;
    private View inscripcionDetailHero;
    private View inscripcionDetailPanel;
    private MaterialButton btnAnular;
    private MaterialButton btnReactivar;
    private TextView txtEstudiante;
    private TextView txtCarrera;
    private TextView txtCurso;
    private TextView txtGrado;
    private TextView txtSeccion;
    private TextView txtCiclo;
    private TextView txtFecha;
    private TextView txtEstado;
    private TextView txtObservaciones;
    private TextView txtActivo;
    private TextView txtFechaCreacion;
    private TextView txtFechaActualizacion;

    private InscripcionApiService apiService;
    private EstudianteApiService estudianteApiService;
    private CarreraApiService carreraApiService;
    private CursoApiService cursoApiService;
    private Call<InscripcionResponse> loadCall;
    private Call<InscripcionResponse> mutationCall;
    private Call<EstudianteResumenResponse> estudianteCall;
    private Call<CarreraResponse> carreraCall;
    private Call<CursoResponse> cursoCall;
    private long inscripcionId;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "INSCRIPCIONES_LEER")) return;
        setContentView(R.layout.activity_inscripcion_detail);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        inscripcionId = getIntent().getLongExtra(EXTRA_INSCRIPCION_ID, -1L);
        apiService = RetrofitClient.getClient().create(InscripcionApiService.class);
        estudianteApiService = RetrofitClient.getClient().create(EstudianteApiService.class);
        carreraApiService = RetrofitClient.getClient().create(CarreraApiService.class);
        cursoApiService = RetrofitClient.getClient().create(CursoApiService.class);
        bindViews();
        setupToolbar();
        setupActions();
        animateIntro();
        loadInscripcion();
    }

    @Override
    protected void onDestroy() {
        cancelCalls();
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbarInscripcionDetail);
        progressBar = findViewById(R.id.progressInscripcionDetail);
        inscripcionDetailHero = findViewById(R.id.inscripcionDetailHero);
        inscripcionDetailPanel = findViewById(R.id.inscripcionDetailPanel);
        btnAnular = findViewById(R.id.btnAnularInscripcion);
        btnReactivar = findViewById(R.id.btnReactivarInscripcion);
        txtEstudiante = findViewById(R.id.txtInscripcionDetalleEstudiante);
        txtCarrera = findViewById(R.id.txtInscripcionDetalleCarrera);
        txtCurso = findViewById(R.id.txtInscripcionDetalleCurso);
        txtGrado = findViewById(R.id.txtInscripcionDetalleGrado);
        txtSeccion = findViewById(R.id.txtInscripcionDetalleSeccion);
        txtCiclo = findViewById(R.id.txtInscripcionDetalleCiclo);
        txtFecha = findViewById(R.id.txtInscripcionDetalleFecha);
        txtEstado = findViewById(R.id.txtInscripcionDetalleEstado);
        txtObservaciones = findViewById(R.id.txtInscripcionDetalleObservaciones);
        txtActivo = findViewById(R.id.txtInscripcionDetalleActivo);
        txtFechaCreacion = findViewById(R.id.txtInscripcionDetalleFechaCreacion);
        txtFechaActualizacion = findViewById(R.id.txtInscripcionDetalleFechaActualizacion);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(view -> finish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void setupActions() {
        btnAnular.setOnClickListener(view -> showAnularDialog());
        btnReactivar.setOnClickListener(view -> showReactivarDialog());
        btnAnular.setVisibility(Permissions.has("INSCRIPCIONES_CAMBIAR_ESTADO") ? View.VISIBLE : View.GONE);
        btnReactivar.setVisibility(Permissions.has("INSCRIPCIONES_CAMBIAR_ESTADO") ? View.VISIBLE : View.GONE);
    }

    private void animateIntro() {
        inscripcionDetailHero.setAlpha(0f);
        inscripcionDetailHero.setTranslationY(20f);
        inscripcionDetailHero.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        inscripcionDetailPanel.setAlpha(0f);
        inscripcionDetailPanel.setTranslationY(18f);
        inscripcionDetailPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(320)
                .start();
    }

    private void loadInscripcion() {
        setLoading(true);
        loadCall = apiService.obtenerInscripcion(inscripcionId);
        loadCall.enqueue(new Callback<InscripcionResponse>() {
            @Override
            public void onResponse(Call<InscripcionResponse> call, Response<InscripcionResponse> response) {
                loadCall = null;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    InscripcionResponse inscripcion = response.body();
                    txtEstudiante.setText("");
                    txtCarrera.setText("");
                    txtCurso.setText("");
                    render(inscripcion);
                    loadNames(inscripcion);
                    return;
                }
                showErrorAndFinish(InscripcionErrorMapper.fromResponse(InscripcionDetailActivity.this, response));
            }

            @Override
            public void onFailure(Call<InscripcionResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                loadCall = null;
                setLoading(false);
                showErrorAndFinish(InscripcionErrorMapper.fromFailure(InscripcionDetailActivity.this, throwable));
            }
        });
    }

    private void render(InscripcionResponse inscripcion) {
        txtGrado.setText(String.valueOf(inscripcion.grado));
        txtSeccion.setText(nonNull(inscripcion.seccion));
        txtCiclo.setText(String.valueOf(inscripcion.cicloAnio));
        txtFecha.setText(Fechas.formatearFecha(inscripcion.fechaInscripcion));

        boolean anulada = isAnulada(inscripcion);
        txtEstado.setText(anulada
                ? R.string.inscripcion_estado_anulada
                : R.string.inscripcion_estado_activa);
        txtEstado.setBackgroundResource(anulada
                ? R.drawable.bg_inscripcion_status_inactive
                : R.drawable.bg_inscripcion_status_active);

        String observaciones = nonNull(inscripcion.observaciones).trim();
        txtObservaciones.setText(observaciones.isEmpty()
                ? getString(R.string.inscripcion_sin_dato)
                : observaciones);
        txtActivo.setText(inscripcion.activo
                ? R.string.inscripcion_si
                : R.string.inscripcion_no);
        txtFechaCreacion.setText(Fechas.formatearFechaHora(inscripcion.fechaCreacion));
        txtFechaActualizacion.setText(Fechas.formatearFechaHora(inscripcion.fechaActualizacion));
        updateActionButtons(anulada);
    }

    private void loadNames(InscripcionResponse i) {
        estudianteCall = estudianteApiService.obtenerEstudiante(i.estudianteId);
        estudianteCall.enqueue(new Callback<EstudianteResumenResponse>() {
            @Override
            public void onResponse(
                    Call<EstudianteResumenResponse> call,
                    Response<EstudianteResumenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EstudianteResumenResponse estudiante = response.body();
                    txtEstudiante.setText(nullToEmpty(estudiante.codigoEstudiantil) + " - "
                            + nullToEmpty(estudiante.nombres) + " " + nullToEmpty(estudiante.apellidos));
                    return;
                }
                txtEstudiante.setText(getString(R.string.inscripcion_item_id, i.estudianteId));
            }

            @Override
            public void onFailure(Call<EstudianteResumenResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                txtEstudiante.setText(getString(R.string.inscripcion_item_id, i.estudianteId));
            }
        });

        carreraCall = carreraApiService.obtenerCarrera(i.carreraId);
        carreraCall.enqueue(new Callback<CarreraResponse>() {
            @Override
            public void onResponse(Call<CarreraResponse> call, Response<CarreraResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CarreraResponse carrera = response.body();
                    txtCarrera.setText(nullToEmpty(carrera.codigo) + " - " + nullToEmpty(carrera.nombre));
                    return;
                }
                txtCarrera.setText(getString(R.string.inscripcion_item_id, i.carreraId));
            }

            @Override
            public void onFailure(Call<CarreraResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                txtCarrera.setText(getString(R.string.inscripcion_item_id, i.carreraId));
            }
        });

        if (i.cursoId != null) {
            cursoCall = cursoApiService.obtenerCurso(i.cursoId);
            cursoCall.enqueue(new Callback<CursoResponse>() {
                @Override
                public void onResponse(Call<CursoResponse> call, Response<CursoResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        CursoResponse curso = response.body();
                        txtCurso.setText(nullToEmpty(curso.codigo) + " - " + nullToEmpty(curso.nombre));
                        return;
                    }
                    txtCurso.setText(getString(R.string.inscripcion_item_id, i.cursoId));
                }

                @Override
                public void onFailure(Call<CursoResponse> call, Throwable throwable) {
                    if (call.isCanceled()) {
                        return;
                    }
                    txtCurso.setText(getString(R.string.inscripcion_item_id, i.cursoId));
                }
            });
        } else {
            txtCurso.setText(getString(R.string.inscripcion_sin_dato));
        }
    }

    private void updateActionButtons(boolean anulada) {
        btnAnular.setEnabled(!loading && !anulada);
        btnReactivar.setEnabled(!loading && anulada);
    }

    private void showAnularDialog() {
        TextInputLayout tilMotivo = new TextInputLayout(this);
        tilMotivo.setHint(getString(R.string.inscripcion_anular_motivo_hint));
        tilMotivo.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText edtMotivo = new TextInputEditText(this);
        edtMotivo.setGravity(Gravity.TOP);
        edtMotivo.setMinLines(2);
        edtMotivo.setMaxLines(4);
        edtMotivo.setTextColor(ContextCompat.getColor(this, R.color.dashboard_text_primary));
        tilMotivo.addView(edtMotivo);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(tilMotivo);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.inscripcion_anular_titulo)
                .setMessage(R.string.inscripcion_anular_mensaje)
                .setView(container)
                .setNegativeButton(R.string.inscripcion_accion_cancelar, null)
                .setPositiveButton(R.string.inscripcion_accion_anular, null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String motivo = edtMotivo.getText() == null
                        ? ""
                        : edtMotivo.getText().toString().trim();
                if (motivo.isEmpty()) {
                    tilMotivo.setError(getString(R.string.inscripcion_error_motivo_requerido));
                    return;
                }
                tilMotivo.setError(null);
                dialog.dismiss();
                SgauDialog.confirm(this, R.drawable.ic_power,
                        getString(R.string.inscripcion_anular_titulo),
                        getString(R.string.inscripcion_anular_mensaje),
                        getString(R.string.inscripcion_accion_anular), () -> callAnular(motivo));
            });
        });
        dialog.show();
    }

    private void showReactivarDialog() {
        SgauDialog.confirmState(this, true, "la inscripción #" + inscripcionId, this::callReactivar);
    }

    private void callAnular(String motivo) {
        setLoading(true);
        mutationCall = apiService.anularInscripcion(inscripcionId, new AnulacionRequest(motivo));
        mutationCall.enqueue(new Callback<InscripcionResponse>() {
            @Override
            public void onResponse(Call<InscripcionResponse> call, Response<InscripcionResponse> response) {
                mutationCall = null;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    render(response.body());
                    UiNotifier.success(InscripcionDetailActivity.this, R.string.inscripcion_anulada);
                    setResult(RESULT_OK);
                    return;
                }
                UiNotifier.error(
                        InscripcionDetailActivity.this,
                        InscripcionErrorMapper.fromResponse(InscripcionDetailActivity.this, response));
            }

            @Override
            public void onFailure(Call<InscripcionResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                mutationCall = null;
                setLoading(false);
                UiNotifier.error(
                        InscripcionDetailActivity.this,
                        InscripcionErrorMapper.fromFailure(InscripcionDetailActivity.this, throwable));
            }
        });
    }

    private void callReactivar() {
        setLoading(true);
        mutationCall = apiService.reactivarInscripcion(inscripcionId);
        mutationCall.enqueue(new Callback<InscripcionResponse>() {
            @Override
            public void onResponse(Call<InscripcionResponse> call, Response<InscripcionResponse> response) {
                mutationCall = null;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    render(response.body());
                    UiNotifier.success(InscripcionDetailActivity.this, R.string.inscripcion_reactivada);
                    setResult(RESULT_OK);
                    return;
                }
                UiNotifier.error(
                        InscripcionDetailActivity.this,
                        InscripcionErrorMapper.fromResponse(InscripcionDetailActivity.this, response));
            }

            @Override
            public void onFailure(Call<InscripcionResponse> call, Throwable throwable) {
                if (call.isCanceled()) {
                    return;
                }
                mutationCall = null;
                setLoading(false);
                UiNotifier.error(
                        InscripcionDetailActivity.this,
                        InscripcionErrorMapper.fromFailure(InscripcionDetailActivity.this, throwable));
            }
        });
    }

    private void setLoading(boolean isLoading) {
        loading = isLoading;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnAnular.setEnabled(!isLoading);
        btnReactivar.setEnabled(!isLoading);
    }

    private boolean isAnulada(InscripcionResponse inscripcion) {
        return "ANULADA".equalsIgnoreCase(nonNull(inscripcion.estado));
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void showErrorAndFinish(String message) {
        UiNotifier.error(this, message);
        finish();
    }

    private void cancelCalls() {
        if (loadCall != null) {
            loadCall.cancel();
        }
        if (mutationCall != null) {
            mutationCall.cancel();
        }
        if (estudianteCall != null) {
            estudianteCall.cancel();
        }
        if (carreraCall != null) {
            carreraCall.cancel();
        }
        if (cursoCall != null) {
            cursoCall.cancel();
        }
    }
}
