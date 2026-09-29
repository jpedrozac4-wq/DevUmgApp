package gt.com.ro.devumgapp.estudiante.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.IOException;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.estudiante.dto.DetalleCursoResponse;
import gt.com.ro.devumgapp.estudiante.dto.EstadoGeneralResponse;
import gt.com.ro.devumgapp.estudiante.dto.HistorialAcademicoResponse;
import gt.com.ro.devumgapp.estudiante.dto.NotaDetalleResponse;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstudianteDetailActivity extends AppCompatActivity {
    private EstudianteApiService service;
    private LinearProgressIndicator progress;
    private TextView resumen, historialResumen, cursos, estadoGeneral;
    private long estudianteId;
    private Call<EstudianteResponse> profileCall;
    private TextView vinculo;
    private int pending = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Permissions.require(this, "ESTUDIANTES_LEER")) return;
        setContentView(R.layout.activity_estudiante_detail);
        estudianteId = getIntent().getLongExtra("estudianteId", -1L);
        if (estudianteId <= 0) { finish(); return; }
        service = RetrofitClient.getClient().create(EstudianteApiService.class);
        bindViews();
        ((MaterialToolbar) findViewById(R.id.toolbarEstudianteDetalle))
                .setNavigationOnClickListener(v -> finish());
        cargarTodo();
    }

    private void bindViews() {
        progress = findViewById(R.id.progressEstudianteDetalle);
        resumen = findViewById(R.id.txtEstudianteDetalleResumen);
        historialResumen = findViewById(R.id.txtEstudianteHistorialResumen);
        cursos = findViewById(R.id.txtEstudianteCursos);
        estadoGeneral = findViewById(R.id.txtEstudianteEstadoGeneral);
        vinculo = findViewById(R.id.txtEstudianteDetalleVinculo);
    }

    private void cargarTodo() {
        pending = 4;
        progress.setVisibility(View.VISIBLE);
        cargarHistorial();
        cargarEstadoGeneral();
        cargarVinculo();
    }

    private void cargarVinculo() {
        profileCall = service.obtenerEstudianteDetalle(estudianteId);
        profileCall.enqueue(new Callback<EstudianteResponse>() {
            @Override public void onResponse(Call<EstudianteResponse> call, Response<EstudianteResponse> response) {
                EstudianteResponse profile = response.body();
                if (response.isSuccessful() && profile != null) {
                    String canonicalName = join(profile.nombres, profile.apellidos);
                    resumen.setText(getString(R.string.estudiante_detalle_resumen,
                            safe(profile.codigoEstudiantil), canonicalName,
                            profile.activo ? getString(R.string.estudiante_estado_activo)
                                    : getString(R.string.estudiante_estado_inactivo)));
                } else {
                    resumen.setText(getString(R.string.estudiante_detalle_no_disponible));
                }
                boolean accountIdentity = response.isSuccessful() && profile != null
                        && "USUARIO".equalsIgnoreCase(profile.identidadFuente);
                boolean linked = accountIdentity || (response.isSuccessful() && profile != null
                        && profile.usuarioId != null && profile.usuarioId > 0 && profile.accesoApp);
                vinculo.setText(accountIdentity ? "Identidad desde cuenta Usuario"
                        : response.isSuccessful() && profile != null && "PERFIL_HISTORICO".equalsIgnoreCase(profile.identidadFuente)
                        ? "Perfil histórico · sin cuenta"
                        : linked ? "Cuenta Usuario vinculada y con acceso"
                        : "Sin vínculo confirmado con Usuario · revisar antes de habilitar acceso");
                vinculo.setTextColor(androidx.core.content.ContextCompat.getColor(EstudianteDetailActivity.this,
                        linked ? R.color.dashboard_text_secondary : R.color.dashboard_error));
                done();
                done();
            }
            @Override public void onFailure(Call<EstudianteResponse> call, Throwable error) {
                vinculo.setText("No se pudo verificar el vínculo con Usuario.");
                resumen.setText(getString(R.string.estudiante_detalle_no_disponible));
                done();
                done();
            }
        });
    }

    private void cargarResumen() {
        service.obtenerResumen(estudianteId).enqueue(new Callback<EstudianteResumenResponse>() {
            @Override public void onResponse(Call<EstudianteResumenResponse> call, Response<EstudianteResumenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EstudianteResumenResponse e = response.body();
                    String nombre = join(e.nombres, e.apellidos);
                    resumen.setText(getString(R.string.estudiante_detalle_resumen,
                            safe(e.codigoEstudiantil), nombre,
                            e.activo ? getString(R.string.estudiante_estado_activo) : getString(R.string.estudiante_estado_inactivo)));
                } else {
                    resumen.setText(getString(R.string.estudiante_detalle_no_disponible));
                }
                done();
            }
            @Override public void onFailure(Call<EstudianteResumenResponse> call, Throwable t) {
                resumen.setText(getString(R.string.estudiante_detalle_no_disponible));
                done();
            }
        });
    }

    private void cargarHistorial() {
        service.obtenerHistorialAcademico(estudianteId).enqueue(new Callback<HistorialAcademicoResponse>() {
            @Override public void onResponse(Call<HistorialAcademicoResponse> call, Response<HistorialAcademicoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HistorialAcademicoResponse h = response.body();
                    historialResumen.setText(getString(R.string.estudiante_detalle_historial,
                            value(h.promedioGeneral), value(h.totalCursos), value(h.cursosAprobados),
                            value(h.cursosReprobados), value(h.cursosSinCalificacion)));
                    StringBuilder sb = new StringBuilder();
                    if (h.detalleCursos == null || h.detalleCursos.isEmpty()) {
                        sb.append(getString(R.string.estudiante_detalle_sin_cursos));
                    } else {
                        for (DetalleCursoResponse c : h.detalleCursos) {
                            sb.append("• ").append(safe(c.codigoCurso)).append(" - ")
                                    .append(safe(c.nombreCurso)).append("\n")
                                    .append("  Ciclo: ").append(value(c.cicloAnio))
                                    .append(" | Promedio: ").append(value(c.promedioCurso))
                                    .append(" | Resultado: ").append(safe(c.resultado)).append("\n");
                            if (c.notas != null && !c.notas.isEmpty()) {
                                for (NotaDetalleResponse n : c.notas) {
                                    sb.append("    ")
                                            .append(safe(n.tipoEvaluacion)).append(": ")
                                            .append(value(n.calificacion)).append("\n");
                                }
                            }
                            sb.append("\n");
                        }
                    }
                    cursos.setText(sb.toString().trim());
                } else {
                    historialResumen.setText(R.string.estudiante_detalle_no_disponible);
                    cursos.setText(R.string.estudiante_detalle_no_disponible);
                }
                done();
            }
            @Override public void onFailure(Call<HistorialAcademicoResponse> call, Throwable t) {
                historialResumen.setText(R.string.estudiante_detalle_no_disponible);
                cursos.setText(R.string.estudiante_detalle_no_disponible);
                done();
            }
        });
    }

    private void cargarEstadoGeneral() {
        service.obtenerEstadoGeneral(estudianteId).enqueue(new Callback<EstadoGeneralResponse>() {
            @Override public void onResponse(Call<EstadoGeneralResponse> call, Response<EstadoGeneralResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EstadoGeneralResponse e = response.body();
                    estadoGeneral.setText(getString(R.string.estudiante_detalle_estado_general,
                            safe(e.codigoEstudiantil), safe(e.correo), safe(e.fechaRegistro), safe(e.estadoGeneral)));
                } else {
                    estadoGeneral.setText(getString(R.string.estudiante_detalle_estado_general_no_disponible));
                }
                done();
            }
            @Override public void onFailure(Call<EstadoGeneralResponse> call, Throwable t) {
                estadoGeneral.setText(getString(R.string.estudiante_detalle_estado_general_no_disponible));
                done();
            }
        });
    }

    private void done() {
        pending--;
        if (pending <= 0) progress.setVisibility(View.GONE);
    }

    @Override protected void onDestroy() {
        if (profileCall != null) profileCall.cancel();
        super.onDestroy();
    }

    private String join(String a, String b) { return (safe(a) + " " + safe(b)).trim(); }
    private String safe(String s) { return s == null || s.trim().isEmpty() ? "—" : s; }
    private String value(Object o) { return o == null ? "—" : String.valueOf(o); }
}
