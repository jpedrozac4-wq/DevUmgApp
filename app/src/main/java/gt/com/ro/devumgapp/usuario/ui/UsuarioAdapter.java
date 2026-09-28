package gt.com.ro.devumgapp.usuario.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.usuario.dto.UsuarioResponse;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder> {
    private List<UsuarioResponse> usuarios;

    public UsuarioAdapter(List<UsuarioResponse> usuarios) {
        this.usuarios = usuarios;
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_usuario, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        UsuarioResponse usuario = usuarios.get(position);
        holder.txtNombre.setText(usuario.nombre + " " + usuario.apellido);
        holder.txtEmail.setText(usuario.email);
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

    static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtEmail;
        UsuarioViewHolder(View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombreUsuario);
            txtEmail = itemView.findViewById(R.id.txtEmailUsuario);
        }
    }
}
