package gt.com.ro.devumgapp.usuario.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import gt.com.ro.devumgapp.R;

public class UsuarioFormActivity extends AppCompatActivity {

    private EditText edtUsuarioUsername, edtUsuarioEmail, edtUsuarioPassword;
    private Button btnGuardarUsuario, btnCancelarUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuario_form);

        edtUsuarioUsername = findViewById(R.id.edtUsuarioUsername);
        edtUsuarioEmail = findViewById(R.id.edtUsuarioEmail);
        edtUsuarioPassword = findViewById(R.id.edtUsuarioPassword);

        btnGuardarUsuario = findViewById(R.id.btnGuardarUsuario);
        btnCancelarUsuario = findViewById(R.id.btnCancelarUsuario);

        // Aquí luego conectas la lógica de guardar/actualizar
    }
}

