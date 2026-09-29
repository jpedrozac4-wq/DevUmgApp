package gt.com.ro.devumgapp.estudiante.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.ui.UiNotifier;
import gt.com.ro.devumgapp.core.ui.SgauDialog;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteRequest;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstudianteFormActivity extends AppCompatActivity {

    private static final long NEW_ESTUDIANTE_ID = -1L;

    private EstudianteApiService service;
    private long estudianteId = NEW_ESTUDIANTE_ID;
    private Call<EstudianteResponse> loadCall;
    private Call<EstudianteResponse> saveCall;
    private boolean loading;

    private TextInputLayout tilCodigo;
    private TextInputLayout tilIdentificacion;
    private TextInputLayout tilNombres;
    private TextInputLayout tilApellidos;
    private TextInputLayout tilFechaNacimiento;
    private TextInputLayout tilCorreo;
    private TextInputLayout tilTelefono;
    private TextInputLayout tilDireccion;

    private TextInputEditText edtCodigo;
    private TextInputEditText edtIdentificacion;
    private TextInputEditText edtNombres;
    private TextInputEditText edtApellidos;
    private TextInputEditText edtFechaNacimiento;
    private TextInputEditText edtCorreo;
    private TextInputEditText edtTelefono;
    private TextInputEditText edtDireccion;

    private com.google.android.material.button.MaterialButton btnGuardar;
    private LinearProgressIndicator progress;
    private android.widget.TextView heroTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        estudianteId = getIntent().getLongExtra("estudianteId", NEW_ESTUDIANTE_ID);
        if (estudianteId <= 0) {
            android.widget.Toast.makeText(this, "Los estudiantes se crean desde Usuarios para vincular su cuenta y perfil.", android.widget.Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String permission = "ESTUDIANTES_EDITAR";
        if (!Permissions.requireAll(this, Permissions.ESTUDIANTES_LEER, permission)) return;
        setContentView(R.layout.activity_estudiante_form);

        service = RetrofitClient.getClient().create(EstudianteApiService.class);

        bindViews();
        setupToolbar();
        setupDatePicker();
        setupActions();

        if (isEditMode()) {
            heroTitle.setText(R.string.estudiante_form_titulo_editar);
            tilNombres.setHelperText("La identidad pertenece a Usuario. Cámbiala desde Mi perfil.");
            tilApellidos.setHelperText("La identidad pertenece a Usuario. Cámbiala desde Mi perfil.");
            tilCorreo.setHelperText("La identidad pertenece a Usuario. Cámbiala desde Mi perfil.");
            loadEstudiante();
        } else {
            heroTitle.setText(R.string.estudiante_form_titulo_crear);
        }
    }

    @Override
    protected void onDestroy() {
        if (loadCall != null) loadCall.cancel();
        if (saveCall != null) saveCall.cancel();
        super.onDestroy();
    }

    private void bindViews() {
        heroTitle = findViewById(R.id.txtEstudianteFormHeroTitle);
        tilCodigo = findViewById(R.id.tilEstudianteCodigo);
        tilIdentificacion = findViewById(R.id.tilEstudianteIdentificacion);
        tilNombres = findViewById(R.id.tilEstudianteNombres);
        tilApellidos = findViewById(R.id.tilEstudianteApellidos);
        tilFechaNacimiento = findViewById(R.id.tilEstudianteFechaNacimiento);
        tilCorreo = findViewById(R.id.tilEstudianteCorreo);
        tilTelefono = findViewById(R.id.tilEstudianteTelefono);
        tilDireccion = findViewById(R.id.tilEstudianteDireccion);

        edtCodigo = findViewById(R.id.edtEstudianteCodigo);
        edtIdentificacion = findViewById(R.id.edtEstudianteIdentificacion);
        edtNombres = findViewById(R.id.edtEstudianteNombres);
        edtApellidos = findViewById(R.id.edtEstudianteApellidos);
        edtFechaNacimiento = findViewById(R.id.edtEstudianteFechaNacimiento);
        edtCorreo = findViewById(R.id.edtEstudianteCorreo);
        edtTelefono = findViewById(R.id.edtEstudianteTelefono);
        edtDireccion = findViewById(R.id.edtEstudianteDireccion);

        btnGuardar = findViewById(R.id.btnGuardarEstudiante);
        progress = findViewById(R.id.progressEstudianteForm);
    }

    private void setupToolbar() {
        ((MaterialToolbar) findViewById(R.id.toolbarEstudianteForm))
                .setNavigationOnClickListener(v -> finish());
    }

    private void setupDatePicker() {
        View.OnClickListener listener = v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (datePicker, year, month, day) ->
                    edtFechaNacimiento.setText(String.format(Locale.US,
                            "%04d-%02d-%02d", year, month + 1, day)),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        };
        edtFechaNacimiento.setOnClickListener(listener);
        tilFechaNacimiento.setEndIconOnClickListener(listener);
    }

    private void setupActions() {
        btnGuardar.setOnClickListener(v -> saveEstudiante());
    }

    private void loadEstudiante() {
        setLoading(true);
        loadCall = service.obtenerEstudianteDetalle(estudianteId);
        loadCall.enqueue(new Callback<EstudianteResponse>() {
            @Override
            public void onResponse(Call<EstudianteResponse> call, Response<EstudianteResponse> response) {
                loadCall = null;
                if (response.isSuccessful() && response.body() != null) {
                    EstudianteResponse estudiante = response.body();
                    fillForm(estudiante);
                    setLoading(false);
                    return;
                }
                setLoading(false);
                UiNotifier.error(EstudianteFormActivity.this,
                        EstudianteErrorMapper.fromResponse(EstudianteFormActivity.this, response));
                finish();
            }

            @Override
            public void onFailure(Call<EstudianteResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                loadCall = null;
                setLoading(false);
                UiNotifier.error(EstudianteFormActivity.this,
                        EstudianteErrorMapper.fromFailure(EstudianteFormActivity.this, t));
                finish();
            }
        });
    }

    private void saveEstudiante() {
        if (loading || !validateForm()) return;

        SgauDialog.confirmSave(this, isEditMode(),
                "el estudiante \"" + textOf(edtNombres) + " " + textOf(edtApellidos) + "\"",
                this::submitEstudiante);
    }

    private void submitEstudiante() {
        if (loading) return;

        EstudianteRequest request = new EstudianteRequest();
        request.codigoEstudiantil = textOf(edtCodigo);
        request.numeroIdentificacion = textOf(edtIdentificacion);
        request.fechaNacimiento = textOf(edtFechaNacimiento);
        request.telefono = textOf(edtTelefono);
        request.direccion = textOf(edtDireccion);
        if (!isEditMode()) {
            request.nombres = textOf(edtNombres);
            request.apellidos = textOf(edtApellidos);
            request.correo = textOf(edtCorreo);
        }

        setLoading(true);
        saveCall = isEditMode()
                ? service.actualizarEstudiante(estudianteId, request)
                : service.crearEstudiante(request);

        saveCall.enqueue(new Callback<EstudianteResponse>() {
            @Override
            public void onResponse(Call<EstudianteResponse> call, Response<EstudianteResponse> response) {
                saveCall = null;
                setLoading(false);
                if (response.isSuccessful()) {
                    if (isEditMode()) {
                        UiNotifier.success(
                                EstudianteFormActivity.this,
                                R.string.estudiante_actualizado);
                    } else {
                        UiNotifier.success(
                                EstudianteFormActivity.this,
                                R.string.estudiante_creado);
                    }
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                UiNotifier.error(EstudianteFormActivity.this,
                        EstudianteErrorMapper.fromResponse(EstudianteFormActivity.this, response));
            }

            @Override
            public void onFailure(Call<EstudianteResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                saveCall = null;
                setLoading(false);
                UiNotifier.error(EstudianteFormActivity.this,
                        EstudianteErrorMapper.fromFailure(EstudianteFormActivity.this, t));
            }
        });
    }

    private void fillForm(EstudianteResponse e) {
        edtCodigo.setText(nonNull(e.codigoEstudiantil));
        edtIdentificacion.setText(nonNull(e.numeroIdentificacion));
        edtNombres.setText(nonNull(e.nombres));
        edtApellidos.setText(nonNull(e.apellidos));
        edtFechaNacimiento.setText(nonNull(e.fechaNacimiento));
        edtCorreo.setText(nonNull(e.correo));
        edtTelefono.setText(nonNull(e.telefono));
        edtDireccion.setText(nonNull(e.direccion));
        String identityHint = "USUARIO".equalsIgnoreCase(e.identidadFuente)
                ? "Identidad desde Usuario · cambia nombre, apellido y correo en Mi perfil"
                : "Perfil histórico sin cuenta · identidad conservada por el perfil";
        tilNombres.setHelperText(identityHint);
        tilApellidos.setHelperText(identityHint);
        tilCorreo.setHelperText(identityHint);
    }

    private boolean validateForm() {
        boolean valid = true;
        valid = validateRequired(tilCodigo, edtCodigo,
                R.string.estudiante_error_codigo_requerido) && valid;
        valid = validateRequired(tilIdentificacion, edtIdentificacion,
                R.string.estudiante_error_identificacion_requerida) && valid;
        if (!isEditMode()) {
            valid = validateRequired(tilNombres, edtNombres,
                    R.string.estudiante_error_nombres_requeridos) && valid;
            valid = validateRequired(tilApellidos, edtApellidos,
                    R.string.estudiante_error_apellidos_requeridos) && valid;
        }
        valid = validateRequired(tilFechaNacimiento, edtFechaNacimiento,
                R.string.estudiante_error_fecha_requerida) && valid;

        if (!isEditMode()) {
            String correo = textOf(edtCorreo);
            if (correo.isEmpty()) {
                tilCorreo.setError(getString(R.string.estudiante_error_correo_requerido));
                valid = false;
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                tilCorreo.setError(getString(R.string.estudiante_error_correo_formato));
                valid = false;
            } else tilCorreo.setError(null);
        }

        return valid;
    }

    private boolean validateRequired(TextInputLayout layout,
                                     TextInputEditText field,
                                     int errorRes) {
        if (textOf(field).isEmpty()) {
            layout.setError(getString(errorRes));
            return false;
        }
        layout.setError(null);
        return true;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!loading);
        tilCodigo.setEnabled(!loading);
        tilIdentificacion.setEnabled(!loading);
        tilNombres.setEnabled(!loading && !isEditMode());
        tilApellidos.setEnabled(!loading && !isEditMode());
        tilFechaNacimiento.setEnabled(!loading);
        tilCorreo.setEnabled(!loading && !isEditMode());
        tilTelefono.setEnabled(!loading);
        tilDireccion.setEnabled(!loading);
    }

    private String textOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }

    private boolean isEditMode() {
        return estudianteId != NEW_ESTUDIANTE_ID;
    }
}
