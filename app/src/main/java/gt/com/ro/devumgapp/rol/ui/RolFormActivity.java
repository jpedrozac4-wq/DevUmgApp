package gt.com.ro.devumgapp.rol.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import gt.com.ro.devumgapp.R;

public class RolFormActivity extends AppCompatActivity {

    private EditText edtNombreRol;
    private Button btnGuardarRol, btnCancelarRol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rol_form);

        edtNombreRol = findViewById(R.id.edtNombreRol);
        btnGuardarRol = findViewById(R.id.btnGuardarRol);
        btnCancelarRol = findViewById(R.id.btnCancelarRol);

        // conectar con RolApiService para guardar datos
    }
}
