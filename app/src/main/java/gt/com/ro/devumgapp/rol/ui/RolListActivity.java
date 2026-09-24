package gt.com.ro.devumgapp.rol.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import gt.com.ro.devumgapp.R;

public class RolListActivity extends AppCompatActivity {

    private RecyclerView recyclerRoles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rol_list);

        recyclerRoles = findViewById(R.id.recyclerRoles);

        // conectar el adapter y el RolApiService
    }
}
