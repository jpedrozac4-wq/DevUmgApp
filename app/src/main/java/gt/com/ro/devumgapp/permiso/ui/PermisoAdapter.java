package gt.com.ro.devumgapp.permiso.ui;

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
import gt.com.ro.devumgapp.permiso.dto.PermisoResponse;

class PermisoAdapter extends RecyclerView.Adapter<PermisoAdapter.PermisoViewHolder> {

    interface Listener {
        void onEdit(PermisoResponse permiso);

        void onToggleStatus(PermisoResponse permiso);
    }

    private final Listener listener;
    private final List<PermisoResponse> permisos = new ArrayList<>();
    private final Set<Long> changingStatusIds = new HashSet<>();
    private final Set<Long> animatedIds = new HashSet<>();

    PermisoAdapter(List<PermisoResponse> items, Listener listener) {
        this.listener = listener;
        submitList(items);
    }

    void submitList(List<PermisoResponse> items) {
        permisos.clear();
        animatedIds.clear();
        if (items != null) {
            permisos.addAll(items);
        }
        notifyDataSetChanged();
    }

    void setStatusChanging(long permisoId, boolean changing) {
        if (changing) {
            changingStatusIds.add(permisoId);
        } else {
            changingStatusIds.remove(permisoId);
        }
        notifyItemChangedById(permisoId);
    }

    void replace(PermisoResponse updated) {
        if (updated == null) {
            return;
        }
        for (int index = 0; index < permisos.size(); index++) {
            if (permisos.get(index).id == updated.id) {
                permisos.set(index, updated);
                notifyItemChanged(index);
                return;
            }
        }
    }

    @NonNull
    @Override
    public PermisoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_permiso, parent, false);
        return new PermisoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PermisoViewHolder holder, int position) {
        holder.bind(permisos.get(position), changingStatusIds.contains(permisos.get(position).id));
        animateEntrance(holder.itemView, permisos.get(position).id, position);
    }

    @Override
    public int getItemCount() {
        return permisos.size();
    }

    private void notifyItemChangedById(long permisoId) {
        for (int index = 0; index < permisos.size(); index++) {
            if (permisos.get(index).id == permisoId) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    private void animateEntrance(View view, long permisoId, int position) {
        if (animatedIds.contains(permisoId)) {
            return;
        }
        animatedIds.add(permisoId);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.dashboard_item_enter);
        animation.setStartOffset((long) Math.min(position, 6) * 45L);
        view.startAnimation(animation);
    }

    class PermisoViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtCodigo;
        private final TextView txtNombre;
        private final TextView txtEstado;
        private final MaterialButton btnEstado;

        PermisoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCodigo = itemView.findViewById(R.id.txtPermisoCodigo);
            txtNombre = itemView.findViewById(R.id.txtPermisoNombre);
            txtEstado = itemView.findViewById(R.id.txtPermisoEstado);
            btnEstado = itemView.findViewById(R.id.btnTogglePermisoEstado);
        }

        void bind(PermisoResponse permiso, boolean changingStatus) {
            txtCodigo.setText(nonNull(permiso.codigo));
            txtNombre.setText(nonNull(permiso.nombre));
            txtEstado.setText(permiso.activo
                    ? R.string.permiso_estado_activo
                    : R.string.permiso_estado_inactivo);
            txtEstado.setBackgroundResource(permiso.activo
                    ? R.drawable.bg_carrera_status_active
                    : R.drawable.bg_carrera_status_inactive);
            int actionLabel = changingStatus
                    ? R.string.permiso_accion_procesando
                    : permiso.activo
                    ? R.string.permiso_accion_desactivar
                    : R.string.permiso_accion_activar;
            btnEstado.setText(actionLabel);
            btnEstado.setContentDescription(itemView.getContext().getString(actionLabel));
            btnEstado.setEnabled(!changingStatus);
            itemView.setClickable(Permissions.has("PERMISOS_EDITAR"));
            itemView.setEnabled(Permissions.has("PERMISOS_EDITAR") && !changingStatus);
            btnEstado.setVisibility(Permissions.has("PERMISOS_CAMBIAR_ESTADO") ? View.VISIBLE : View.GONE);
            itemView.setEnabled(!changingStatus);
            itemView.setOnClickListener(view -> listener.onEdit(permiso));
            btnEstado.setOnClickListener(view -> animatePress(view, () -> listener.onToggleStatus(permiso)));
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
