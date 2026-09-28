package gt.com.ro.devumgapp.permiso.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import gt.com.ro.devumgapp.R;

public class PermisoListActivity extends AppCompatActivity {

    private RecyclerView recyclerPermisos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permiso_list);

        recyclerPermisos = findViewById(R.id.recyclerPermisos);

        // conectar el adapter y el PermisoApiService
    }
}
