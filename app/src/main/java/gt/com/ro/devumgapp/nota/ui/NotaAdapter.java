package gt.com.ro.devumgapp.nota.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.nota.dto.NotaResponse;

class NotaAdapter extends RecyclerView.Adapter<NotaAdapter.NotaViewHolder> {

    interface Listener {
        void onEdit(NotaResponse nota);

        void onOpenPromedio(NotaResponse nota);

        void onToggleActivo(NotaResponse nota);
    }

    private final Listener listener;
    private final List<NotaResponse> notas = new ArrayList<>();
    private final Set<Long> changingStatusIds = new HashSet<>();
    private final Set<Long> animatedIds = new HashSet<>();
    private Map<Long, String> estudianteNombres = new HashMap<>();
    private Map<Long, String> cursoNombres = new HashMap<>();

    NotaAdapter(Listener listener) {
        this.listener = listener;
    }

    void setNombres(Map<Long, String> estudianteNombres, Map<Long, String> cursoNombres) {
        this.estudianteNombres = estudianteNombres != null ? estudianteNombres : new HashMap<>();
        this.cursoNombres = cursoNombres != null ? cursoNombres : new HashMap<>();
    }

    void submitList(List<NotaResponse> items) {
        notas.clear();
        changingStatusIds.clear();
        animatedIds.clear();
        if (items != null) {
            notas.addAll(items);
        }
        notifyDataSetChanged();
    }

    void setStatusChanging(long notaId, boolean changing) {
        if (changing) {
            changingStatusIds.add(notaId);
        } else {
            changingStatusIds.remove(notaId);
        }
        notifyItemChangedById(notaId);
    }

    void replace(NotaResponse updated) {
        if (updated == null) {
            return;
        }
        for (int index = 0; index < notas.size(); index++) {
            if (notas.get(index).id == updated.id) {
                notas.set(index, updated);
                notifyItemChanged(index);
                return;
            }
        }
    }

    @NonNull
    @Override
    public NotaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nota, parent, false);
        return new NotaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotaViewHolder holder, int position) {
        NotaResponse nota = notas.get(position);
        holder.bind(nota, changingStatusIds.contains(nota.id));
        animateEntrance(holder.itemView, nota.id, position);
    }

    @Override
    public int getItemCount() {
        return notas.size();
    }

    private void notifyItemChangedById(long notaId) {
        for (int index = 0; index < notas.size(); index++) {
            if (notas.get(index).id == notaId) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    private void animateEntrance(View view, long notaId, int position) {
        if (animatedIds.contains(notaId)) {
            return;
        }
        animatedIds.add(notaId);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.nota_item_enter);
        animation.setStartOffset((long) Math.min(position, 6) * 45L);
        view.startAnimation(animation);
    }

    class NotaViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtResumen;
        private final TextView txtCurso;
        private final TextView txtCicloTipo;
        private final TextView txtCalificacion;
        private final TextView txtFecha;
        private final TextView txtObservaciones;
        private final TextView txtEstado;
        private final SwitchCompat switchActivo;
        private final MaterialButton btnEditar;
        private final MaterialButton btnPromedio;

        NotaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtResumen = itemView.findViewById(R.id.txtNotaResumen);
            txtCurso = itemView.findViewById(R.id.txtNotaCurso);
            txtCicloTipo = itemView.findViewById(R.id.txtNotaCicloTipo);
            txtCalificacion = itemView.findViewById(R.id.txtNotaCalificacion);
            txtFecha = itemView.findViewById(R.id.txtNotaFecha);
            txtObservaciones = itemView.findViewById(R.id.txtNotaObservaciones);
            txtEstado = itemView.findViewById(R.id.txtNotaEstado);
            switchActivo = itemView.findViewById(R.id.switchNotaActivo);
            btnEditar = itemView.findViewById(R.id.btnEditarNota);
            btnPromedio = itemView.findViewById(R.id.btnPromedioNota);
        }

        void bind(NotaResponse nota, boolean changingStatus) {
            String nombreEstudiante = estudianteNombres.get(nota.estudianteId);
            txtResumen.setText(nombreEstudiante != null
                    ? nombreEstudiante
                    : itemView.getContext().getString(R.string.nota_item_id, nota.estudianteId));
            String nombreCurso = cursoNombres.get(nota.cursoId);
            txtCurso.setText(itemView.getContext().getString(
                    R.string.nota_item_curso,
                    nombreCurso != null
                            ? nombreCurso
                            : itemView.getContext().getString(R.string.nota_item_id, nota.cursoId)));
            txtCicloTipo.setText(itemView.getContext().getString(
                    R.string.nota_item_ciclo_tipo,
                    nota.cicloAnio,
                    nonNull(nota.tipoEvaluacion)));
            txtCalificacion.setText(itemView.getContext().getString(
                    R.string.nota_item_calificacion,
                    String.format(Locale.US, "%.2f", nota.calificacion)));
            txtFecha.setText(itemView.getContext().getString(
                    R.string.nota_item_fecha,
                    Fechas.formatearFecha(nota.fechaActualizacion)));
            String observaciones = nonNull(nota.observaciones).trim();
            txtObservaciones.setText(observaciones);
            txtObservaciones.setVisibility(observaciones.isEmpty() ? View.GONE : View.VISIBLE);

            txtEstado.setText(nota.activo
                    ? R.string.nota_estado_activa
                    : R.string.nota_estado_inactiva);
            txtEstado.setBackgroundResource(nota.activo
                    ? R.drawable.bg_nota_status_active
                    : R.drawable.bg_nota_status_inactive);

            switchActivo.setChecked(nota.activo);
            switchActivo.setEnabled(!changingStatus);
            switchActivo.setVisibility(Permissions.has("NOTAS_CAMBIAR_ESTADO") ? View.VISIBLE : View.GONE);
            switchActivo.setOnCheckedChangeListener(null);
            switchActivo.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {
                        if (isChecked != nota.activo) {
                            listener.onToggleActivo(nota);
                        }
                    });

            btnEditar.setEnabled(!changingStatus);
            btnPromedio.setEnabled(!changingStatus);
            btnEditar.setVisibility(Permissions.has("NOTAS_EDITAR") ? View.VISIBLE : View.GONE);
            btnEditar.setOnClickListener(view -> animatePress(view, () -> listener.onEdit(nota)));
            btnPromedio.setOnClickListener(view -> animatePress(view, () -> listener.onOpenPromedio(nota)));
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
