package gt.com.ro.devumgapp.permiso.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import gt.com.ro.devumgapp.R;

public class PermisoFormActivity extends AppCompatActivity {

    private EditText edtNombrePermiso;
    private Button btnGuardarPermiso, btnCancelarPermiso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permiso_form);

        edtNombrePermiso = findViewById(R.id.edtNombrePermiso);
        btnGuardarPermiso = findViewById(R.id.btnGuardarPermiso);
        btnCancelarPermiso = findViewById(R.id.btnCancelarPermiso);

        // conectar con PermisoApiService para guardar datos
    }
}
