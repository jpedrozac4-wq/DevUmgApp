package gt.com.ro.devumgapp.estudiante.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResponse;

final class EstudianteAdapter extends RecyclerView.Adapter<EstudianteAdapter.ViewHolder> {

    interface Listener {
        void onEdit(EstudianteResponse estudiante);
        void onToggleStatus(EstudianteResponse estudiante);
        void onDetail(EstudianteResponse estudiante);
    }

    private final Listener listener;
    private final List<EstudianteResponse> estudiantes = new ArrayList<>();
    private final Set<Long> changingIds = new HashSet<>();
    private final Set<Long> animatedIds = new HashSet<>();

    EstudianteAdapter(Listener listener) {
        this.listener = listener;
    }

    void submitList(List<EstudianteResponse> items) {
        estudiantes.clear();
        animatedIds.clear();

        if (items != null) {
            estudiantes.addAll(items);
        }

        notifyDataSetChanged();
    }

    void setStatusChanging(long id, boolean changing) {
        if (changing) {
            changingIds.add(id);
        } else {
            changingIds.remove(id);
        }

        for (int i = 0; i < estudiantes.size(); i++) {
            if (estudiantes.get(i).id == id) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    void replace(EstudianteResponse updated) {
        if (updated == null) {
            return;
        }

        for (int i = 0; i < estudiantes.size(); i++) {
            if (estudiantes.get(i).id == updated.id) {
                estudiantes.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_estudiante, parent, false);

        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        EstudianteResponse item = estudiantes.get(position);

        holder.bind(
                item,
                changingIds.contains(item.id)
        );

        if (!animatedIds.contains(item.id)) {
            animatedIds.add(item.id);

            holder.itemView.startAnimation(
                    AnimationUtils.loadAnimation(
                            holder.itemView.getContext(),
                            R.anim.dashboard_item_enter
                    )
            );
        }
    }

    @Override
    public int getItemCount() {
        return estudiantes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView codigo;
        final TextView nombre;
        final TextView identificacion;
        final TextView correo;
        final TextView estado;

        final MaterialButton editar;
        final MaterialButton estadoBtn;

        private final Listener listener;

        ViewHolder(
                @NonNull View itemView,
                Listener listener
        ) {
            super(itemView);

            this.listener = listener;

            codigo = itemView.findViewById(
                    R.id.txtEstudianteCodigo
            );

            nombre = itemView.findViewById(
                    R.id.txtEstudianteNombre
            );

            identificacion = itemView.findViewById(
                    R.id.txtEstudianteIdentificacion
            );

            correo = itemView.findViewById(
                    R.id.txtEstudianteCorreo
            );

            estado = itemView.findViewById(
                    R.id.txtEstudianteEstado
            );

            editar = itemView.findViewById(
                    R.id.btnEditarEstudiante
            );

            estadoBtn = itemView.findViewById(
                    R.id.btnCambiarEstadoEstudiante
            );
        }

        void bind(
                EstudianteResponse e,
                boolean changing
        ) {
            codigo.setText(
                    nonNull(e.codigoEstudiantil)
            );

            nombre.setText(
                    (nonNull(e.nombres) + " "
                            + nonNull(e.apellidos)).trim()
            );

            identificacion.setText(
                    itemView.getContext().getString(
                            R.string.estudiante_item_identificacion,
                            nonNull(e.numeroIdentificacion)
                    )
            );

            correo.setText(
                    nonNull(e.correo)
            );

            estado.setText(
                    e.activo
                            ? R.string.estudiante_estado_activo
                            : R.string.estudiante_estado_inactivo
            );

            estado.setBackgroundResource(
                    e.activo
                            ? R.drawable.bg_estudiante_status_active
                            : R.drawable.bg_estudiante_status_inactive
            );

            estadoBtn.setContentDescription(
                    itemView.getContext().getString(
                            changing
                                    ? R.string.estudiante_accion_procesando
                                    : e.activo
                                      ? R.string.estudiante_accion_inactivar
                                      : R.string.estudiante_accion_activar
                    )
            );

            estadoBtn.setEnabled(!changing);
            editar.setEnabled(!changing);

            editar.setOnClickListener(
                    v -> listener.onEdit(e)
            );

            estadoBtn.setOnClickListener(
                    v -> listener.onToggleStatus(e)
            );

            itemView.setOnClickListener(
                    v -> listener.onDetail(e)
            );
        }

        private String nonNull(String value) {
            return value == null ? "" : value;
        }
    }
}