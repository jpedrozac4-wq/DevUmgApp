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
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.estudiante.dto.DetalleCursoResponse;
import gt.com.ro.devumgapp.estudiante.dto.EstadoGeneralResponse;
import gt.com.ro.devumgapp.estudiante.dto.HistorialAcademicoResponse;
import gt.com.ro.devumgapp.estudiante.dto.NotaDetalleResponse;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResumenResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstudianteDetailActivity extends AppCompatActivity {
    private EstudianteApiService service;
    private LinearProgressIndicator progress;
    private TextView resumen, historialResumen, cursos, estadoGeneral;
    private long estudianteId;
    private int pending = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void cargarTodo() {
        pending = 3;
        progress.setVisibility(View.VISIBLE);
        cargarResumen();
        cargarHistorial();
        cargarEstadoGeneral();
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

    private String join(String a, String b) { return (safe(a) + " " + safe(b)).trim(); }
    private String safe(String s) { return s == null || s.trim().isEmpty() ? "—" : s; }
    private String value(Object o) { return o == null ? "—" : String.valueOf(o); }
}
