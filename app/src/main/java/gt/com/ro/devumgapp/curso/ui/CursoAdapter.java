package gt.com.ro.devumgapp.curso.ui;

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
import gt.com.ro.devumgapp.curso.dto.CursoResponse;

class CursoAdapter extends RecyclerView.Adapter<CursoAdapter.CursoViewHolder> {

    interface Listener {
        void onEdit(CursoResponse curso);

        void onToggleStatus(CursoResponse curso);

        void onAssignTeacher(CursoResponse curso);

        void onRemoveTeacher(CursoResponse curso);
    }

    private final Listener listener;
    private final List<CursoResponse> cursos = new ArrayList<>();
    private final Set<Long> busyIds = new HashSet<>();
    private final Set<Long> animatedIds = new HashSet<>();

    CursoAdapter(Listener listener) {
        this.listener = listener;
    }

    void submitList(List<CursoResponse> items) {
        cursos.clear();
        busyIds.clear();
        animatedIds.clear();
        if (items != null) {
            cursos.addAll(items);
        }
        notifyDataSetChanged();
    }

    void setBusy(long cursoId, boolean busy) {
        if (busy) {
            busyIds.add(cursoId);
        } else {
            busyIds.remove(cursoId);
        }
        notifyItemChangedById(cursoId);
    }

    void replace(CursoResponse updated) {
        if (updated == null) {
            return;
        }
        for (int index = 0; index < cursos.size(); index++) {
            if (cursos.get(index).id == updated.id) {
                cursos.set(index, updated);
                notifyItemChanged(index);
                return;
            }
        }
    }

    @NonNull
    @Override
    public CursoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_curso, parent, false);
        return new CursoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CursoViewHolder holder, int position) {
        CursoResponse curso = cursos.get(position);
        holder.bind(curso, busyIds.contains(curso.id));
        animateEntrance(holder.itemView, curso.id, position);
    }

    @Override
    public int getItemCount() {
        return cursos.size();
    }

    private void notifyItemChangedById(long cursoId) {
        for (int index = 0; index < cursos.size(); index++) {
            if (cursos.get(index).id == cursoId) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    private void animateEntrance(View view, long cursoId, int position) {
        if (animatedIds.contains(cursoId)) {
            return;
        }
        animatedIds.add(cursoId);
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.curso_item_enter);
        animation.setStartOffset((long) Math.min(position, 6) * 45L);
        view.startAnimation(animation);
    }

    class CursoViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtCodigo;
        private final TextView txtNombre;
        private final TextView txtDescripcion;
        private final TextView txtMeta;
        private final TextView txtDocente;
        private final TextView txtEstado;
        private final MaterialButton btnEditar;
        private final MaterialButton btnEstado;
        private final MaterialButton btnDocente;
        private final MaterialButton btnQuitarDocente;

        CursoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCodigo = itemView.findViewById(R.id.txtCursoCodigo);
            txtNombre = itemView.findViewById(R.id.txtCursoNombre);
            txtDescripcion = itemView.findViewById(R.id.txtCursoDescripcion);
            txtMeta = itemView.findViewById(R.id.txtCursoMeta);
            txtDocente = itemView.findViewById(R.id.txtCursoDocente);
            txtEstado = itemView.findViewById(R.id.txtCursoEstado);
            btnEditar = itemView.findViewById(R.id.btnEditarCurso);
            btnEstado = itemView.findViewById(R.id.btnCambiarEstadoCurso);
            btnDocente = itemView.findViewById(R.id.btnAsignarDocenteCurso);
            btnQuitarDocente = itemView.findViewById(R.id.btnQuitarDocenteCurso);
        }

        void bind(CursoResponse curso, boolean busy) {
            txtCodigo.setText(nonNull(curso.codigo));
            txtNombre.setText(nonNull(curso.nombre));
            txtDescripcion.setText(nonNull(curso.descripcion));
            txtMeta.setText(itemView.getContext().getString(
                    R.string.curso_item_meta,
                    curso.creditos,
                    curso.horasSemanales,
                    curso.cicloAnio,
                    curso.carreraId));
            txtDocente.setText(curso.docenteId == null
                    ? itemView.getContext().getString(R.string.curso_docente_sin_asignar)
                    : itemView.getContext().getString(R.string.curso_docente_id, curso.docenteId));
            txtEstado.setText(curso.activo ? R.string.curso_estado_activo : R.string.curso_estado_inactivo);
            txtEstado.setBackgroundResource(curso.activo
                    ? R.drawable.bg_curso_status_active
                    : R.drawable.bg_curso_status_inactive);
            btnEstado.setContentDescription(itemView.getContext().getString(busy
                    ? R.string.curso_accion_procesando
                    : curso.activo ? R.string.curso_accion_inactivar : R.string.curso_accion_activar));
            btnQuitarDocente.setVisibility(curso.docenteId == null ? View.GONE : View.VISIBLE);
            btnEditar.setVisibility(Permissions.has("CURSOS_EDITAR") ? View.VISIBLE : View.GONE);
            btnEstado.setVisibility(Permissions.has("CURSOS_CAMBIAR_ESTADO") ? View.VISIBLE : View.GONE);
            btnDocente.setVisibility(Permissions.has("CURSOS_ASIGNAR_DOCENTE") ? View.VISIBLE : View.GONE);
            if (!Permissions.has("CURSOS_ASIGNAR_DOCENTE")) btnQuitarDocente.setVisibility(View.GONE);

            btnEditar.setEnabled(!busy);
            btnEstado.setEnabled(!busy);
            btnDocente.setEnabled(!busy);
            btnQuitarDocente.setEnabled(!busy);
            btnEditar.setOnClickListener(view -> animatePress(view, () -> listener.onEdit(curso)));
            btnEstado.setOnClickListener(view -> animatePress(view, () -> listener.onToggleStatus(curso)));
            btnDocente.setOnClickListener(view -> animatePress(view, () -> listener.onAssignTeacher(curso)));
            btnQuitarDocente.setOnClickListener(view -> animatePress(view, () -> listener.onRemoveTeacher(curso)));
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
