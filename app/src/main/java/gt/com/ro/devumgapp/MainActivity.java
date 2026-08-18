package gt.com.ro.devumgapp;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText edtUsuario;
    private EditText edtPassword;
    private Button btnLogin;
    private CheckBox chkRecordar;
    private ImageButton btnShowPassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Forzar colores de barra de estado para modo oscuro/azul
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_blue_darkest, getTheme()));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.bg_blue_darkest, getTheme()));

        edtUsuario = findViewById(R.id.edtUsuario);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        chkRecordar = findViewById(R.id.chkRecordar);
        btnShowPassword = findViewById(R.id.btnShowPassword);







        // Actividad 4: recuperar la sesión guardada al abrir la app
        cargarSesion();

        btnShowPassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Ocultar contraseña
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnShowPassword.setImageResource(R.drawable.ic_visibility);
            } else {
                // Mostrar contraseña
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnShowPassword.setImageResource(R.drawable.ic_visibility_off);
            }
            isPasswordVisible = !isPasswordVisible;
            edtPassword.setSelection(edtPassword.getText().length());
        });

        // ── Actividad 3: lógica de login ──────────────────────────
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = edtUsuario.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();

                if (user.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this,
                            "Todos los campos son obligatorios",
                            Toast.LENGTH_SHORT).show();

                } else if (user.equals("devumg") && password.equals("devumg2026")) {
                    Toast.makeText(MainActivity.this,
                            "Bienvenido, autenticación exitosa",
                            Toast.LENGTH_SHORT).show();

                    if (chkRecordar.isChecked()) {
                        guardarSesion(user, password);

                        // Actividad 5: Toast dentro de la clase anónima
                        Toast.makeText(MainActivity.this,
                                "Sesión guardada correctamente",
                                Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(MainActivity.this,
                            "Usuario o contraseña incorrectos",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        // ── Fin Actividad 3 ────────────────────────────────────────
    }

    // ── Actividad 4: persistencia con SharedPreferences ────────────
    private void guardarSesion(String user, String password) {
        SharedPreferences prefs = getSharedPreferences("DevUmgPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("user_name", user);
        editor.putString("user_pass", password);
        editor.putString("auth_token", "SIMULATED-TOKEN-12345");

        editor.apply();
    }

    private void cargarSesion() {
        SharedPreferences prefs = getSharedPreferences("DevUmgPrefs", MODE_PRIVATE);

        String savedUser = prefs.getString("user_name", "");
        String savedToken = prefs.getString("auth_token", "");

        if (!savedUser.isEmpty()) {
            edtUsuario.setText(savedUser);
            chkRecordar.setChecked(true);
        }
    }
    // ── Fin Actividad 4 ────────────────────────────────────────────
}
