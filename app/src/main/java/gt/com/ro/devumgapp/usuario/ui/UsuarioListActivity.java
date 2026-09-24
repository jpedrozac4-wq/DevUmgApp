package gt.com.ro.devumgapp.usuario.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import gt.com.ro.devumgapp.R;

public class UsuarioListActivity extends AppCompatActivity {

    private RecyclerView recyclerUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuario_list);

        recyclerUsuarios = findViewById(R.id.recyclerUsuarios);

        // Aquí luego conectarás el adapter y el UsuarioApiService
    }
}
