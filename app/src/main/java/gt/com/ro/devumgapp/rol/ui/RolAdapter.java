package gt.com.ro.devumgapp.rol.ui;

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
import gt.com.ro.devumgapp.rol.dto.RolResponse;

class RolAdapter extends RecyclerView.Adapter<RolAdapter.RolViewHolder> {

    interface Listener {
        void onEdit(RolResponse rol);

        void onToggleStatus(RolResponse rol);

        void onManagePermissions(RolResponse rol);
    }

    private final Listener listener;
    private final List<RolResponse> roles = new ArrayList<>();
    private final Set<Long> changingStatusIds = new HashSet<>();
    private final Set<Long> animatedIds = new HashSet<>();

    RolAdapter(List<RolResponse> items, Listener listener) {
        this.listener = listener;
        submitList(items);
    }

    void submitList(List<RolResponse> items) {
        roles.clear();
        animatedIds.clear();
        if (items != null) {
            roles.addAll(items);
        }
        notifyDataSetChanged();
    }

    void setStatusChanging(long rolId, boolean changing) {
        if (changing) {
            changingStatusIds.add(rolId);
        } else {
            changingStatusIds.remove(rolId);
        }
        notifyItemChangedById(rolId);
    }

    void replace(RolResponse updated) {
        if (updated == null) {
            return;
        }
        for (int index = 0; index < roles.size(); index++) {
            if (roles.get(index).id == updated.id) {
                roles.set(index, updated);
                notifyItemChanged(index);
                return;
            }
        }
    }

    @NonNull
    @Override
    public RolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rol, parent, false);
        return new RolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RolViewHolder holder, int position) {
        holder.bind(roles.get(position), changingStatusIds.contains(roles.get(position).id));
        animateEntrance(holder.itemView, roles.get(position).id, position);
    }

    @Override
    public int getItemCount() {
        return roles.size();
    }

    private void notifyItemChangedById(long rolId) {
        for (int index = 0; index < roles.size(); index++) {
            if (roles.get(index).id == rolId) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    private void animateEntrance(View view, long rolId, int position) {
        if (animatedIds.contains(rolId)) {
            return;
        }
        animatedIds.add(rolId);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.dashboard_item_enter);
        animation.setStartOffset((long) Math.min(position, 6) * 45L);
        view.startAnimation(animation);
    }

    class RolViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtCodigo;
        private final TextView txtNombre;
        private final TextView txtEstado;
        private final MaterialButton btnEstado;
        private final MaterialButton btnPermisos;

        RolViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCodigo = itemView.findViewById(R.id.txtRolCodigo);
            txtNombre = itemView.findViewById(R.id.txtRolNombre);
            txtEstado = itemView.findViewById(R.id.txtRolEstado);
            btnEstado = itemView.findViewById(R.id.btnToggleRolEstado);
            btnPermisos = itemView.findViewById(R.id.btnGestionarPermisosRol);
        }

        void bind(RolResponse rol, boolean changingStatus) {
            txtCodigo.setText(nonNull(rol.codigo));
            txtNombre.setText(nonNull(rol.nombre));
            txtEstado.setText(rol.activo
                    ? R.string.rol_estado_activo
                    : R.string.rol_estado_inactivo);
            txtEstado.setBackgroundResource(rol.activo
                    ? R.drawable.bg_carrera_status_active
                    : R.drawable.bg_carrera_status_inactive);
            int actionLabel = changingStatus
                    ? R.string.rol_accion_procesando
                    : rol.activo
                    ? R.string.rol_accion_desactivar
                    : R.string.rol_accion_activar;
            btnEstado.setText(actionLabel);
            btnEstado.setContentDescription(itemView.getContext().getString(actionLabel));
            btnEstado.setEnabled(!changingStatus);
            itemView.setClickable(Permissions.has("ROLES_EDITAR"));
            itemView.setEnabled(Permissions.has("ROLES_EDITAR") && !changingStatus);
            btnEstado.setVisibility(Permissions.has("ROLES_CAMBIAR_ESTADO") ? View.VISIBLE : View.GONE);
            btnPermisos.setVisibility(Permissions.has("ROLES_ASIGNAR_PERMISOS") ? View.VISIBLE : View.GONE);
            itemView.setOnClickListener(view -> listener.onEdit(rol));
            btnEstado.setOnClickListener(view -> animatePress(view, () -> listener.onToggleStatus(rol)));
            btnPermisos.setOnClickListener(view -> listener.onManagePermissions(rol));
        }

        private String nonNull(String value) {
            return value == null ? "" : value;
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
