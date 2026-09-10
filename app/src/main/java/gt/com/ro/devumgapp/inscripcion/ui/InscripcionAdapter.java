package gt.com.ro.devumgapp.inscripcion.ui;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.inscripcion.dto.InscripcionResponse;

class InscripcionAdapter extends RecyclerView.Adapter<InscripcionAdapter.InscripcionViewHolder> {

    interface Listener {
        void onEdit(InscripcionResponse inscripcion);

        void onOpenDetail(InscripcionResponse inscripcion);
    }

    private final Listener listener;
    private final List<InscripcionResponse> inscripciones = new ArrayList<>();
    private final Set<Long> changingStatusIds = new HashSet<>();
    private final Set<Long> animatedIds = new HashSet<>();
    private Map<Long, String> estudianteNombres = new HashMap<>();
    private Map<Long, String> carreraNombres = new HashMap<>();
    private Map<Long, String> cursoNombres = new HashMap<>();

    InscripcionAdapter(Listener listener) {
        this.listener = listener;
    }

    void setNombres(Map<Long, String> estudianteNombres, Map<Long, String> carreraNombres, Map<Long, String> cursoNombres) {
        this.estudianteNombres = estudianteNombres != null ? estudianteNombres : new HashMap<>();
        this.carreraNombres = carreraNombres != null ? carreraNombres : new HashMap<>();
        this.cursoNombres = cursoNombres != null ? cursoNombres : new HashMap<>();
    }

    void submitList(List<InscripcionResponse> items) {
        inscripciones.clear();
        changingStatusIds.clear();
        animatedIds.clear();
        if (items != null) {
            inscripciones.addAll(items);
        }
        notifyDataSetChanged();
    }

    void setStatusChanging(long inscripcionId, boolean changing) {
        if (changing) {
            changingStatusIds.add(inscripcionId);
        } else {
            changingStatusIds.remove(inscripcionId);
        }
        notifyItemChangedById(inscripcionId);
    }

    void replace(InscripcionResponse updated) {
        if (updated == null) {
            return;
        }
        for (int index = 0; index < inscripciones.size(); index++) {
            if (inscripciones.get(index).id == updated.id) {
                inscripciones.set(index, updated);
                notifyItemChanged(index);
                return;
            }
        }
    }

    @NonNull
    @Override
    public InscripcionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inscripcion, parent, false);
        return new InscripcionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InscripcionViewHolder holder, int position) {
        InscripcionResponse inscripcion = inscripciones.get(position);
        holder.bind(inscripcion, changingStatusIds.contains(inscripcion.id));
        animateEntrance(holder.itemView, inscripcion.id, position);
    }

    @Override
    public int getItemCount() {
        return inscripciones.size();
    }

    private void notifyItemChangedById(long inscripcionId) {
        for (int index = 0; index < inscripciones.size(); index++) {
            if (inscripciones.get(index).id == inscripcionId) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    private void animateEntrance(View view, long inscripcionId, int position) {
        if (animatedIds.contains(inscripcionId)) {
            return;
        }
        animatedIds.add(inscripcionId);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.inscripcion_item_enter);
        animation.setStartOffset((long) Math.min(position, 6) * 45L);
        view.startAnimation(animation);
    }

    class InscripcionViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtResumen;
        private final TextView txtCarreraCurso;
        private final TextView txtCurso;
        private final TextView txtCicloSeccion;
        private final TextView txtFecha;
        private final TextView txtObservaciones;
        private final TextView txtEstado;
        private final MaterialButton btnEditar;
        private final MaterialButton btnVer;

        InscripcionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtResumen = itemView.findViewById(R.id.txtInscripcionResumen);
            txtCarreraCurso = itemView.findViewById(R.id.txtInscripcionCarreraCurso);
            txtCurso = itemView.findViewById(R.id.txtInscripcionCurso);
            txtCicloSeccion = itemView.findViewById(R.id.txtInscripcionCicloSeccion);
            txtFecha = itemView.findViewById(R.id.txtInscripcionFecha);
            txtObservaciones = itemView.findViewById(R.id.txtInscripcionObservaciones);
            txtEstado = itemView.findViewById(R.id.txtInscripcionEstado);
            btnEditar = itemView.findViewById(R.id.btnEditarInscripcion);
            btnVer = itemView.findViewById(R.id.btnVerInscripcion);
        }

        void bind(InscripcionResponse inscripcion, boolean changingStatus) {
            String nombreEstudiante = estudianteNombres.get(inscripcion.estudianteId);
            txtResumen.setText(nombreEstudiante != null
                    ? nombreEstudiante
                    : itemView.getContext().getString(R.string.inscripcion_item_id, inscripcion.estudianteId));
            String nombreCarrera = carreraNombres.get(inscripcion.carreraId);
            if (nombreCarrera == null) {
                nombreCarrera = itemView.getContext().getString(
                        R.string.inscripcion_item_id, inscripcion.carreraId);
            }
            txtCarreraCurso.setText(itemView.getContext().getString(
                    R.string.inscripcion_item_carrera,
                    nombreCarrera));
            if (inscripcion.cursoId == null) {
                txtCurso.setText(itemView.getContext().getString(
                        R.string.inscripcion_item_curso,
                        itemView.getContext().getString(R.string.inscripcion_sin_dato)));
            } else {
                String nombreCurso = cursoNombres.get(inscripcion.cursoId);
                if (nombreCurso == null) {
                    nombreCurso = itemView.getContext().getString(
                            R.string.inscripcion_item_id, inscripcion.cursoId);
                }
                txtCurso.setText(itemView.getContext().getString(
                        R.string.inscripcion_item_curso,
                        nombreCurso));
            }
            txtCicloSeccion.setText(itemView.getContext().getString(
                    R.string.inscripcion_item_resumen,
                    inscripcion.cicloAnio,
                    nonNull(inscripcion.seccion)));
            txtFecha.setText(itemView.getContext().getString(
                    R.string.inscripcion_item_fecha,
                    Fechas.formatearFecha(inscripcion.fechaInscripcion)));
            String observaciones = nonNull(inscripcion.observaciones).trim();
            txtObservaciones.setText(observaciones);
            txtObservaciones.setVisibility(observaciones.isEmpty() ? View.GONE : View.VISIBLE);

            boolean anulada = isAnulada(inscripcion);
            txtEstado.setText(anulada
                    ? R.string.inscripcion_estado_anulada
                    : R.string.inscripcion_estado_activa);
            txtEstado.setBackgroundResource(anulada
                    ? R.drawable.bg_inscripcion_status_inactive
                    : R.drawable.bg_inscripcion_status_active);

            btnEditar.setEnabled(!changingStatus);
            btnVer.setEnabled(!changingStatus);
            btnEditar.setOnClickListener(view -> animatePress(view, () -> listener.onEdit(inscripcion)));
            btnVer.setOnClickListener(view -> animatePress(view, () -> listener.onOpenDetail(inscripcion)));
        }

        private boolean isAnulada(InscripcionResponse inscripcion) {
            return "ANULADA".equalsIgnoreCase(nonNull(inscripcion.estado));
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
