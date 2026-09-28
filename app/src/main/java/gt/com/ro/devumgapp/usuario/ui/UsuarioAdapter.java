package gt.com.ro.devumgapp.usuario.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.rol.dto.RolResumenResponse;
import gt.com.ro.devumgapp.usuario.dto.UsuarioResponse;

class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder> {

    interface Listener {
        void onEdit(UsuarioResponse usuario);

        void onDelete(UsuarioResponse usuario);

        void onManageRoles(UsuarioResponse usuario);
    }

    private final Listener listener;
    private final List<UsuarioResponse> usuarios = new ArrayList<>();
    private final Set<Long> animatedIds = new HashSet<>();

    UsuarioAdapter(List<UsuarioResponse> items, Listener listener) {
        this.listener = listener;
        submitList(items);
    }

    static String displayName(UsuarioResponse usuario) {
        String nombre = nonNull(usuario.nombre).trim();
        String apellido = nonNull(usuario.apellido).trim();
        String fullName = (nombre + " " + apellido).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        return nonNull(usuario.username).trim();
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }

    void submitList(List<UsuarioResponse> items) {
        usuarios.clear();
        animatedIds.clear();
        if (items != null) {
            usuarios.addAll(items);
        }
        notifyDataSetChanged();
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
        holder.bind(usuarios.get(position));
        animateEntrance(holder.itemView, usuarios.get(position).id, position);
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

    private void animateEntrance(View view, long usuarioId, int position) {
        if (animatedIds.contains(usuarioId)) {
            return;
        }
        animatedIds.add(usuarioId);
        Animation animation = AnimationUtils.loadAnimation(
                view.getContext(),
                R.anim.dashboard_item_enter);
        animation.setStartOffset((long) Math.min(position, 6) * 45L);
        view.startAnimation(animation);
    }

    class UsuarioViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtNombre;
        private final TextView txtEmail;
        private final TextView txtRoles;
        private final MaterialButton btnRoles;
        private final MaterialButton btnEliminar;

        UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombreUsuario);
            txtEmail = itemView.findViewById(R.id.txtEmailUsuario);
            txtRoles = itemView.findViewById(R.id.txtRolesUsuario);
            btnRoles = itemView.findViewById(R.id.btnRolesUsuario);
            btnEliminar = itemView.findViewById(R.id.btnEliminarUsuario);
        }

        void bind(UsuarioResponse usuario) {
            txtNombre.setText(displayName(usuario));
            txtEmail.setText(nonNull(usuario.email));
            txtRoles.setText(buildRolesText(usuario.roles));
            btnEliminar.setText(R.string.usuario_accion_eliminar);
            btnEliminar.setContentDescription(
                    itemView.getContext().getString(R.string.usuario_accion_eliminar));
            btnRoles.setText(R.string.usuario_accion_roles);
            btnRoles.setContentDescription(
                    itemView.getContext().getString(R.string.usuario_accion_roles));
            itemView.setOnClickListener(view -> listener.onEdit(usuario));
            itemView.setClickable(Permissions.has("USUARIOS_EDITAR"));
            itemView.setEnabled(Permissions.has("USUARIOS_EDITAR"));
            btnRoles.setVisibility(Permissions.has("USUARIOS_ASIGNAR_ROLES") ? View.VISIBLE : View.GONE);
            btnEliminar.setVisibility(Permissions.has("USUARIOS_ELIMINAR") ? View.VISIBLE : View.GONE);
            btnRoles.setOnClickListener(view -> listener.onManageRoles(usuario));
            btnEliminar.setOnClickListener(view ->
                    animatePress(view, () -> listener.onDelete(usuario)));
        }

        private String buildRolesText(List<RolResumenResponse> roles) {
            String emptyLabel = itemView.getContext().getString(R.string.usuario_sin_roles);
            if (roles == null || roles.isEmpty()) {
                return emptyLabel;
            }
            String separator = itemView.getContext().getString(R.string.usuario_roles_separador);
            StringBuilder builder = new StringBuilder();
            for (RolResumenResponse rol : roles) {
                if (rol == null) {
                    continue;
                }
                String label = nonNull(rol.nombre).trim();
                if (label.isEmpty()) {
                    label = nonNull(rol.codigo).trim();
                }
                if (label.isEmpty()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(separator);
                }
                builder.append(label);
            }
            return builder.length() == 0 ? emptyLabel : builder.toString();
        }

        private void animatePress(View view, Runnable endAction) {
            view.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(70)
                    .withEndAction(() -> view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(110)
                            .withEndAction(endAction)
                            .start())
                    .start();
        }
    }
}
