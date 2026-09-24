package gt.com.ro.devumgapp.usuario.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import gt.com.ro.devumgapp.R;

public class UsuarioFormActivity extends AppCompatActivity {

    private EditText edtNombreUsuario, edtCorreoUsuario;
    private Button btnGuardarUsuario, btnCancelarUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuario_form);

        edtNombreUsuario = findViewById(R.id.edtNombreUsuario);
        edtCorreoUsuario = findViewById(R.id.edtCorreoUsuario);
        btnGuardarUsuario = findViewById(R.id.btnGuardarUsuario);
        btnCancelarUsuario = findViewById(R.id.btnCancelarUsuario);

        // conectar con UsuarioApiService para guardar datos
    }
}
