package gt.com.ro.devumgapp.colegiatura.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.colegiatura.dto.ColegiaturaResponse;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.PagoRevision;

public class ColegiaturaAdapter extends RecyclerView.Adapter<ColegiaturaAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(ColegiaturaResponse item);
        void onChangeState(ColegiaturaResponse item);
        void onReviewPayment(PagoRevision payment, String decision);
    }

    private final List<ColegiaturaResponse> items = new ArrayList<>();
    private final List<PagoRevision> pendingPayments = new ArrayList<>();
    private final Listener listener;

    public ColegiaturaAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<ColegiaturaResponse> nuevos) {
        items.clear();
        if (nuevos != null) items.addAll(nuevos);
        notifyDataSetChanged();
    }

    public void setPendingPayments(List<PagoRevision> payments) {
        pendingPayments.clear();
        if (payments != null) pendingPayments.addAll(payments);
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

        h.pendingPayments.removeAllViews();
        int matchingPayments = 0;
        if (Permissions.hasRole("ADMIN") && Permissions.has(Permissions.COLEGIATURAS_CAMBIAR_ESTADO)) {
            for (PagoRevision payment : pendingPayments) {
                if (payment.colegiaturaId != item.id || !"PENDIENTE".equalsIgnoreCase(payment.estado)) continue;
                if (matchingPayments++ == 0) {
                    TextView section = new TextView(h.itemView.getContext());
                    section.setText("REPORTES PENDIENTES DE REVISIÓN");
                    section.setTextSize(11);
                    section.setTypeface(null, android.graphics.Typeface.BOLD);
                    section.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.dashboard_primary));
                    section.setPadding(0, dp(h.itemView, 12), 0, dp(h.itemView, 6));
                    h.pendingPayments.addView(section);
                }
                addPendingPayment(h, payment);
            }
        }
        h.pendingPayments.setVisibility(matchingPayments > 0 ? View.VISIBLE : View.GONE);
        h.managementActions.setVisibility(matchingPayments > 0 ? View.GONE : View.VISIBLE);

        h.editar.setOnClickListener(v -> listener.onEdit(item));
        h.estadoAccion.setOnClickListener(v -> listener.onChangeState(item));

        boolean anulada = "ANULADA".equalsIgnoreCase(item.estado);
        h.estadoAccion.setVisibility(anulada ? View.GONE : View.VISIBLE);
        h.editar.setVisibility(Permissions.has("COLEGIATURAS_EDITAR") ? View.VISIBLE : View.GONE);
        h.estadoAccion.setVisibility(Permissions.has("COLEGIATURAS_CAMBIAR_ESTADO") && !anulada
                ? View.VISIBLE : View.GONE);
    }

    private void addPendingPayment(ViewHolder holder, PagoRevision payment) {
        LinearLayout row = new LinearLayout(holder.itemView.getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(holder.itemView, 8), 0, dp(holder.itemView, 8));
        LinearLayout details = new LinearLayout(holder.itemView.getContext());
        details.setOrientation(LinearLayout.VERTICAL);
        TextView amount = new TextView(holder.itemView.getContext());
        amount.setText(String.format(Locale.getDefault(), "Q%.2f · %s", payment.monto, safe(payment.estudianteNombre)));
        amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.dashboard_text_primary));
        amount.setTextSize(14);
        amount.setTypeface(null, android.graphics.Typeface.BOLD);
        details.addView(amount);
        TextView reference = new TextView(holder.itemView.getContext());
        reference.setText("Boleta: " + safe(payment.referencia) + " · " + safe(payment.fechaPago));
        reference.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.dashboard_text_secondary));
        reference.setTextSize(12);
        details.addView(reference);
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        MaterialButton reject = new MaterialButton(holder.itemView.getContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        reject.setText("×"); reject.setTextSize(22); reject.setContentDescription("Rechazar pago");
        reject.setMinWidth(dp(holder.itemView, 44)); reject.setInsetTop(0); reject.setInsetBottom(0);
        reject.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.dashboard_error));
        reject.setOnClickListener(v -> listener.onReviewPayment(payment, "RECHAZADO"));
        row.addView(reject);

        MaterialButton accept = new MaterialButton(holder.itemView.getContext());
        accept.setText("✓"); accept.setTextSize(18); accept.setContentDescription("Aceptar pago");
        accept.setMinWidth(dp(holder.itemView, 44)); accept.setInsetTop(0); accept.setInsetBottom(0);
        accept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.dashboard_payment)));
        accept.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        accept.setOnClickListener(v -> listener.onReviewPayment(payment, "APROBADO"));
        row.addView(accept);
        holder.pendingPayments.addView(row);
    }

    private int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
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
        final View editar, estadoAccion;
        final LinearLayout pendingPayments, managementActions;

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
            estadoAccion = itemView.findViewById(R.id.btnEstadoColegiatura);
            pendingPayments = itemView.findViewById(R.id.pendingPaymentReports);
            managementActions = itemView.findViewById(R.id.colegiaturaManagementActions);
        }
    }
}
