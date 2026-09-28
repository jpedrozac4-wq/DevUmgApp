package gt.com.ro.devumgapp.docente.ui;

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
import gt.com.ro.devumgapp.docente.dto.DocenteResponse;

class DocenteAdapter extends RecyclerView.Adapter<DocenteAdapter.DocenteViewHolder> {

    interface Listener {
        void onEdit(DocenteResponse docente);

        void onToggleStatus(DocenteResponse docente);

    }

    private final Listener listener;
    private final List<DocenteResponse> docentes = new ArrayList<>();
    private final Set<Long> busyIds = new HashSet<>();
    private final Set<Long> animatedIds = new HashSet<>();

    DocenteAdapter(Listener listener) {
        this.listener = listener;
    }

    void submitList(List<DocenteResponse> items) {
        docentes.clear();
        busyIds.clear();
        animatedIds.clear();
        if (items != null) {
            docentes.addAll(items);
        }
        notifyDataSetChanged();
    }

    void setBusy(long docenteId, boolean busy) {
        if (busy) {
            busyIds.add(docenteId);
        } else {
            busyIds.remove(docenteId);
        }
        notifyItemChangedById(docenteId);
    }

    void replace(DocenteResponse updated) {
        if (updated == null) {
            return;
        }
        for (int index = 0; index < docentes.size(); index++) {
            if (docentes.get(index).id == updated.id) {
                docentes.set(index, updated);
                notifyItemChanged(index);
                return;
            }
        }
    }

    @NonNull
    @Override
    public DocenteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_docente, parent, false);
        return new DocenteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocenteViewHolder holder, int position) {
        DocenteResponse docente = docentes.get(position);
        holder.bind(docente, busyIds.contains(docente.id));
        animateEntrance(holder.itemView, docente.id, position);
    }

    @Override
    public int getItemCount() {
        return docentes.size();
    }

    private void notifyItemChangedById(long docenteId) {
        for (int index = 0; index < docentes.size(); index++) {
            if (docentes.get(index).id == docenteId) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    private void animateEntrance(View view, long docenteId, int position) {
        if (animatedIds.contains(docenteId)) {
            return;
        }
        animatedIds.add(docenteId);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.docente_item_enter);
        animation.setStartOffset((long) Math.min(position, 6) * 45L);
        view.startAnimation(animation);
    }

    class DocenteViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtCodigo;
        private final TextView txtNombre;
        private final TextView txtContacto;
        private final TextView txtEspecialidad;
        private final TextView txtEstado;
        private final MaterialButton btnEditar;
        private final MaterialButton btnEstado;

        DocenteViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCodigo = itemView.findViewById(R.id.txtDocenteCodigo);
            txtNombre = itemView.findViewById(R.id.txtDocenteNombre);
            txtContacto = itemView.findViewById(R.id.txtDocenteContacto);
            txtEspecialidad = itemView.findViewById(R.id.txtDocenteEspecialidad);
            txtEstado = itemView.findViewById(R.id.txtDocenteEstado);
            btnEditar = itemView.findViewById(R.id.btnEditarDocente);
            btnEstado = itemView.findViewById(R.id.btnCambiarEstadoDocente);
        }

        void bind(DocenteResponse docente, boolean busy) {
            txtCodigo.setText(nonNull(docente.codigoDocente));
            txtNombre.setText(itemView.getContext().getString(
                    R.string.docente_item_nombre,
                    nonNull(docente.nombre),
                    nonNull(docente.apellido)));
            txtContacto.setText(itemView.getContext().getString(
                    R.string.docente_item_contacto,
                    nonNull(docente.email),
                    docente.telefono == null || docente.telefono.isEmpty()
                            ? itemView.getContext().getString(R.string.docente_sin_telefono)
                            : docente.telefono));
            boolean tieneEspecialidad = docente.especialidad != null && !docente.especialidad.isEmpty();
            txtEspecialidad.setText(tieneEspecialidad
                    ? docente.especialidad
                    : itemView.getContext().getString(R.string.docente_sin_especialidad));
            txtEstado.setText(docente.activo ? R.string.docente_estado_activo : R.string.docente_estado_inactivo);
            txtEstado.setBackgroundResource(docente.activo
                    ? R.drawable.bg_docente_status_active
                    : R.drawable.bg_docente_status_inactive);
            btnEstado.setContentDescription(itemView.getContext().getString(busy
                    ? R.string.docente_accion_procesando
                    : docente.activo ? R.string.docente_accion_inactivar : R.string.docente_accion_activar));

            btnEditar.setEnabled(!busy);
            btnEstado.setEnabled(!busy);
            btnEditar.setVisibility(Permissions.has("DOCENTES_EDITAR") ? View.VISIBLE : View.GONE);
            btnEstado.setVisibility(Permissions.has("DOCENTES_CAMBIAR_ESTADO") ? View.VISIBLE : View.GONE);
            btnEditar.setOnClickListener(view -> animatePress(view, () -> listener.onEdit(docente)));
            btnEstado.setOnClickListener(view -> animatePress(view, () -> listener.onToggleStatus(docente)));
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
