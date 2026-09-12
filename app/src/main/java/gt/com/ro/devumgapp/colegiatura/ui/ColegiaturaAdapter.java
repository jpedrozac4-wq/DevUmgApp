package gt.com.ro.devumgapp.colegiatura.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaResponse;

public class ColegiaturaAdapter extends RecyclerView.Adapter<ColegiaturaAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(ColegiaturaResponse item);
        void onPay(ColegiaturaResponse item);
        void onChangeState(ColegiaturaResponse item);
    }

    private final List<ColegiaturaResponse> items = new ArrayList<>();
    private final Listener listener;

    public ColegiaturaAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<ColegiaturaResponse> nuevos) {
        items.clear();
        if (nuevos != null) items.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_colegiatura, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ColegiaturaResponse item = items.get(position);
        h.concepto.setText(item.concepto == null || item.concepto.trim().isEmpty()
                ? "Colegiatura" : item.concepto);
        h.estudiante.setText(String.format(Locale.getDefault(), "Estudiante ID: %d", item.estudianteId));
        h.ciclo.setText(String.format(Locale.getDefault(), "Ciclo %d", item.cicloAnio));
        h.total.setText(String.format(Locale.getDefault(), "Q%.2f", item.montoTotal));
        h.pagado.setText(String.format(Locale.getDefault(), "Q%.2f", item.montoPagado));
        h.saldo.setText(String.format(Locale.getDefault(), "Q%.2f", item.saldoPendiente));
        h.emision.setText("Emisión: " + safe(item.fechaEmision));
        h.vencimiento.setText("Vence: " + safe(item.fechaVencimiento));
        h.estado.setText(safe(item.estado));
        setStatusBackground(h.estado, item.estado);

        h.editar.setOnClickListener(v -> listener.onEdit(item));
        h.pagar.setOnClickListener(v -> listener.onPay(item));
        h.estadoAccion.setOnClickListener(v -> listener.onChangeState(item));

        boolean anulada = "ANULADA".equalsIgnoreCase(item.estado);
        h.pagar.setVisibility("PAGADA".equalsIgnoreCase(item.estado) || anulada
                ? View.GONE : View.VISIBLE);
        h.estadoAccion.setVisibility(anulada ? View.GONE : View.VISIBLE);
    }

    private void setStatusBackground(TextView view, String estado) {
        int color = Color.rgb(122, 122, 137);
        if ("PENDIENTE".equalsIgnoreCase(estado)) color = Color.rgb(245, 158, 11);
        if ("PARCIAL".equalsIgnoreCase(estado)) color = Color.rgb(79, 70, 229);
        if ("PAGADA".equalsIgnoreCase(estado)) color = Color.rgb(34, 160, 107);
        if ("ANULADA".equalsIgnoreCase(estado)) color = Color.rgb(179, 38, 30);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(18f);
        view.setBackground(drawable);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView concepto, estudiante, ciclo, estado, total, pagado, saldo, emision, vencimiento;
        final View editar, pagar, estadoAccion;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            concepto = itemView.findViewById(R.id.txtColegiaturaConcepto);
            estudiante = itemView.findViewById(R.id.txtColegiaturaEstudiante);
            ciclo = itemView.findViewById(R.id.txtColegiaturaCiclo);
            estado = itemView.findViewById(R.id.txtColegiaturaEstado);
            total = itemView.findViewById(R.id.txtColegiaturaTotal);
            pagado = itemView.findViewById(R.id.txtColegiaturaPagado);
            saldo = itemView.findViewById(R.id.txtColegiaturaSaldo);
            emision = itemView.findViewById(R.id.txtColegiaturaEmision);
            vencimiento = itemView.findViewById(R.id.txtColegiaturaVencimiento);
            editar = itemView.findViewById(R.id.btnEditarColegiatura);
            pagar = itemView.findViewById(R.id.btnPagarColegiatura);
            estadoAccion = itemView.findViewById(R.id.btnEstadoColegiatura);
        }
    }
}
