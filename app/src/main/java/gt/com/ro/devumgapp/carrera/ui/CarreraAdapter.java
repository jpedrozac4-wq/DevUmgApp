package gt.com.ro.devumgapp.carrera.ui;

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
import gt.com.ro.devumgapp.carrera.dto.CarreraResponse;

class CarreraAdapter extends RecyclerView.Adapter<CarreraAdapter.CarreraViewHolder> {

    interface Listener {
        void onEdit(CarreraResponse carrera);

        void onToggleStatus(CarreraResponse carrera);
    }

    private final Listener listener;
    private final List<CarreraResponse> carreras = new ArrayList<>();
    private final Set<Long> changingStatusIds = new HashSet<>();
    private final Set<Long> animatedIds = new HashSet<>();

    CarreraAdapter(Listener listener) {
        this.listener = listener;
    }

    void submitList(List<CarreraResponse> items) {
        carreras.clear();
        animatedIds.clear();
        if (items != null) {
            carreras.addAll(items);
        }
        notifyDataSetChanged();
    }

    void setStatusChanging(long carreraId, boolean changing) {
        if (changing) {
            changingStatusIds.add(carreraId);
        } else {
            changingStatusIds.remove(carreraId);
        }
        notifyItemChangedById(carreraId);
    }

    void replace(CarreraResponse updated) {
        if (updated == null) {
            return;
        }
        for (int index = 0; index < carreras.size(); index++) {
            if (carreras.get(index).id == updated.id) {
                carreras.set(index, updated);
                notifyItemChanged(index);
                return;
            }
        }
    }

    @NonNull
    @Override
    public CarreraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carrera, parent, false);
        return new CarreraViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarreraViewHolder holder, int position) {
        holder.bind(carreras.get(position), changingStatusIds.contains(carreras.get(position).id));
        animateEntrance(holder.itemView, carreras.get(position).id, position);
    }

    @Override
    public int getItemCount() {
        return carreras.size();
    }

    private void notifyItemChangedById(long carreraId) {
        for (int index = 0; index < carreras.size(); index++) {
            if (carreras.get(index).id == carreraId) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    private void animateEntrance(View view, long carreraId, int position) {
        if (animatedIds.contains(carreraId)) {
            return;
        }
        animatedIds.add(carreraId);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.carrera_item_enter);
        animation.setStartOffset((long) Math.min(position, 6) * 45L);
        view.startAnimation(animation);
    }

    class CarreraViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtCodigo;
        private final TextView txtNombre;
        private final TextView txtDescripcion;
        private final TextView txtDuracion;
        private final TextView txtEstado;
        private final MaterialButton btnEditar;
        private final MaterialButton btnEstado;

        CarreraViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCodigo = itemView.findViewById(R.id.txtCarreraCodigo);
            txtNombre = itemView.findViewById(R.id.txtCarreraNombre);
            txtDescripcion = itemView.findViewById(R.id.txtCarreraDescripcion);
            txtDuracion = itemView.findViewById(R.id.txtCarreraDuracion);
            txtEstado = itemView.findViewById(R.id.txtCarreraEstado);
            btnEditar = itemView.findViewById(R.id.btnEditarCarrera);
            btnEstado = itemView.findViewById(R.id.btnCambiarEstadoCarrera);
        }

        void bind(CarreraResponse carrera, boolean changingStatus) {
            txtCodigo.setText(nonNull(carrera.codigo));
            txtNombre.setText(nonNull(carrera.nombre));
            txtDescripcion.setText(nonNull(carrera.descripcion));
            txtDuracion.setText(itemView.getContext().getResources().getQuantityString(
                    R.plurals.carrera_duracion_anios,
                    carrera.duracionAnios,
                    carrera.duracionAnios));
            txtEstado.setText(carrera.activo
                    ? R.string.carrera_estado_activo
                    : R.string.carrera_estado_inactivo);
            txtEstado.setBackgroundResource(carrera.activo
                    ? R.drawable.bg_carrera_status_active
                    : R.drawable.bg_carrera_status_inactive);
            btnEstado.setText(null);
            btnEstado.setContentDescription(itemView.getContext().getString(changingStatus
                    ? R.string.carrera_accion_procesando
                    : carrera.activo
                    ? R.string.carrera_accion_inactivar
                    : R.string.carrera_accion_activar));
            btnEstado.setEnabled(!changingStatus);
            btnEditar.setEnabled(!changingStatus);
            btnEditar.setOnClickListener(view -> animatePress(view, () -> listener.onEdit(carrera)));
            btnEstado.setOnClickListener(view -> animatePress(view, () -> listener.onToggleStatus(carrera)));
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
