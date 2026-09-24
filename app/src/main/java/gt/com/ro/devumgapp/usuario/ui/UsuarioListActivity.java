package gt.com.ro.devumgapp.usuario.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.usuario.dto.UsuarioResponse;
import gt.com.ro.devumgapp.usuario.network.UsuarioApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuarioListActivity extends AppCompatActivity {

    private RecyclerView recyclerUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuario_list);

        recyclerUsuarios = findViewById(R.id.recyclerUsuarios);
        recyclerUsuarios.setLayoutManager(new LinearLayoutManager(this));

        cargarUsuarios();
    }

    private void cargarUsuarios() {
        UsuarioApiService apiService = RetrofitClient.getClient().create(UsuarioApiService.class);


        apiService.listar().enqueue(new Callback<List<UsuarioResponse>>() {
            @Override
            public void onResponse(Call<List<UsuarioResponse>> call, Response<List<UsuarioResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<UsuarioResponse> usuarios = response.body();
                    recyclerUsuarios.setAdapter(new UsuarioAdapter(usuarios));
                } else {
                    Toast.makeText(UsuarioListActivity.this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<UsuarioResponse>> call, Throwable t) {
                Toast.makeText(UsuarioListActivity.this, "Fallo de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
