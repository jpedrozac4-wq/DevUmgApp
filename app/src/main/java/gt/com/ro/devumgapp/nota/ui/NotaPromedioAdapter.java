package gt.com.ro.devumgapp.nota.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import gt.com.ro.devumgapp.R;

class NotaPromedioAdapter extends RecyclerView.Adapter<NotaPromedioAdapter.NotaPromedioViewHolder> {

    static class NotaPromedioRow {

        final String cursoNombre;
        final String tipoEvaluacion;
        final int cicloAnio;
        final double calificacion;

        NotaPromedioRow(String cursoNombre, String tipoEvaluacion, int cicloAnio, double calificacion) {
            this.cursoNombre = cursoNombre;
            this.tipoEvaluacion = tipoEvaluacion;
            this.cicloAnio = cicloAnio;
            this.calificacion = calificacion;
        }
    }

    private final List<NotaPromedioRow> rows = new ArrayList<>();

    void submitList(List<NotaPromedioRow> items) {
        rows.clear();
        if (items != null) {
            rows.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotaPromedioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nota_promedio, parent, false);
        return new NotaPromedioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotaPromedioViewHolder holder, int position) {
        holder.bind(rows.get(position));
        animateEntrance(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private void animateEntrance(View view, int position) {
        Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.nota_item_enter);
        animation.setStartOffset((long) Math.min(position, 6) * 45L);
        view.startAnimation(animation);
    }

    class NotaPromedioViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtCurso;
        private final TextView txtTipo;
        private final TextView txtCalificacion;

        NotaPromedioViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCurso = itemView.findViewById(R.id.txtItemNotaPromedioCurso);
            txtTipo = itemView.findViewById(R.id.txtItemNotaPromedioTipo);
            txtCalificacion = itemView.findViewById(R.id.txtItemNotaPromedioCalificacion);
        }

        void bind(NotaPromedioRow row) {
            txtCurso.setText(nonNull(row.cursoNombre));
            txtTipo.setText(itemView.getContext().getString(
                    R.string.nota_promedio_item_tipo,
                    nonNull(row.tipoEvaluacion),
                    row.cicloAnio));
            txtCalificacion.setText(String.format(Locale.US, "%.2f", row.calificacion));
        }

        private String nonNull(String value) {
            return value == null ? "" : value;
        }
    }
}
