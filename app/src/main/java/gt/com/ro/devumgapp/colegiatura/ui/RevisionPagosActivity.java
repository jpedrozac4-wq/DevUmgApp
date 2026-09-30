package gt.com.ro.devumgapp.colegiatura.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.*;
import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.academico.dto.AcademicDtos.*;
import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
import gt.com.ro.devumgapp.core.dto.PageResponse;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import gt.com.ro.devumgapp.core.session.Permissions;
import retrofit2.*;

/** Revisión administrativa aislada; el backend valida usuario, rol activo y permiso vigente. */
public class RevisionPagosActivity extends AppCompatActivity {
    private static final int SIZE=10;
    private AcademicoApiService api;
    private Spinner estado;
    private EditText texto;
    private LinearLayout rows;
    private TextView pager,empty;
    private ProgressBar progress;
    private Button anterior,siguiente,buscar;
    private int page=0,totalPages=1;
    private Call<PageResponse<PagoRevision>> listCall;
    private Call<Pago> reviewCall;

    @Override protected void onCreate(@Nullable Bundle state){
        super.onCreate(state);
        if(!Permissions.require(this,Permissions.COLEGIATURAS_CAMBIAR_ESTADO))return;
        if(!Permissions.hasRole("ADMIN")){finish();return;}
        api=RetrofitClient.getClient().create(AcademicoApiService.class);
        buildUi(); long detailId=getIntent().getLongExtra("paymentId",-1L); if(detailId>0)loadDetail(detailId);else load();
    }
    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(getColor(R.color.dashboard_background));
        MaterialToolbar toolbar=new MaterialToolbar(this);toolbar.setTitle("Revisión de pagos");toolbar.setNavigationIcon(R.drawable.ic_arrow_back);toolbar.setNavigationOnClickListener(v->finish());root.addView(toolbar);
        LinearLayout filters=new LinearLayout(this);filters.setPadding(dp(16),dp(8),dp(16),dp(8));filters.setOrientation(LinearLayout.VERTICAL);
        estado=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"PENDIENTE","APROBADO","RECHAZADO","TODOS"});estado.setAdapter(adapter);filters.addView(estado);
        texto=new TextInputEditText(this);texto.setSingleLine(true);texto.setHint("Buscar estudiante, carrera o boleta");filters.addView(texto);
        buscar=new Button(this);buscar.setText("Buscar");buscar.setOnClickListener(v->{page=0;load();});filters.addView(buscar);root.addView(filters);
        progress=new ProgressBar(this);progress.setVisibility(View.GONE);root.addView(progress);
        empty=new TextView(this);empty.setPadding(dp(20),dp(12),dp(20),dp(4));root.addView(empty);
        ScrollView scroll=new ScrollView(this);rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);rows.setPadding(dp(16),dp(8),dp(16),dp(8));scroll.addView(rows);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout paging=new LinearLayout(this);paging.setGravity(android.view.Gravity.CENTER);anterior=new Button(this);anterior.setText("Anterior");siguiente=new Button(this);siguiente.setText("Siguiente");pager=new TextView(this);pager.setPadding(dp(14),0,dp(14),0);anterior.setOnClickListener(v->{if(page>0){page--;load();}});siguiente.setOnClickListener(v->{if(page+1<totalPages){page++;load();}});paging.addView(anterior);paging.addView(pager);paging.addView(siguiente);root.addView(paging);setContentView(root);
    }
    private void load(){setBusy(true);String filter=String.valueOf(estado.getSelectedItem());if("TODOS".equals(filter))filter=null;String q=texto.getText()==null?null:texto.getText().toString().trim();listCall=api.buscarPagosRevision(filter,q,page,SIZE);listCall.enqueue(new Callback<PageResponse<PagoRevision>>(){public void onResponse(Call<PageResponse<PagoRevision>>c,Response<PageResponse<PagoRevision>>r){setBusy(false);if(!r.isSuccessful()||r.body()==null){empty.setText("No fue posible cargar los pagos. Verifica tu permiso y vuelve a intentar.");return;}PageResponse<PagoRevision> result=r.body();totalPages=Math.max(1,result.totalPages);pager.setText((page+1)+" / "+totalPages);anterior.setEnabled(page>0);siguiente.setEnabled(page+1<totalPages);render(result.content==null?Collections.emptyList():result.content);}public void onFailure(Call<PageResponse<PagoRevision>>c,Throwable t){setBusy(false);empty.setText("Error de conexión. Los filtros se conservaron.");}});}
    private void loadDetail(long id){setBusy(true);api.detallePagoRevision(id).enqueue(new Callback<PagoRevision>(){public void onResponse(Call<PagoRevision>c,Response<PagoRevision>r){setBusy(false);if(r.isSuccessful()&&r.body()!=null){empty.setText("Detalle del pago · ID "+id);render(Collections.singletonList(r.body()));}else empty.setText("El pago no está disponible o tu autorización expiró.");}public void onFailure(Call<PagoRevision>c,Throwable t){setBusy(false);empty.setText("No se pudo cargar el pago. Vuelve a intentarlo desde Colegiaturas.");}});}
    private void render(List<PagoRevision> items){rows.removeAllViews();empty.setText(items.isEmpty()?"No hay pagos para este filtro.":"");for(PagoRevision p:items){MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(14));card.setCardElevation(dp(2));card.setCardBackgroundColor(getColor(R.color.dashboard_surface));LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(14),dp(16),dp(14));
        add(box,(p.estudianteNombre==null?"Estudiante":p.estudianteNombre)+" · "+(p.estudianteCodigo==null?"":p.estudianteCodigo),true);
        add(box,"Carrera: "+safe(p.carreraNombre)+"\nCuota: "+safe(p.concepto)+" (ID "+p.colegiaturaId+")",false);
        add(box,"Boleta: "+safe(p.referencia)+" · Medio: "+safe(p.metodoPago)+"\nFecha: "+safe(p.fechaPago)+" · Monto: Q"+String.format(Locale.getDefault(),"%.2f",p.monto)+"\nEstado: "+safe(p.estado)+(p.motivoRechazo==null?"":"\nMotivo: "+p.motivoRechazo),false);
        if(p.comprobanteUrl!=null&&!p.comprobanteUrl.trim().isEmpty()){Button proof=new Button(this);proof.setText("Ver comprobante");proof.setOnClickListener(v->{try{Uri uri=Uri.parse(p.comprobanteUrl);String scheme=uri.getScheme();if(!"https".equalsIgnoreCase(scheme)&&!"http".equalsIgnoreCase(scheme))throw new IllegalArgumentException();startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){Toast.makeText(this,"El enlace del comprobante no es seguro o no se pudo abrir.",Toast.LENGTH_SHORT).show();}});box.addView(proof);}
        if("PENDIENTE".equals(p.estado)){LinearLayout actions=new LinearLayout(this);Button approve=new Button(this);approve.setText("Aprobar");Button reject=new Button(this);reject.setText("Rechazar");approve.setOnClickListener(v->confirm(p,"APROBADO",null));reject.setOnClickListener(v->reject(p));actions.addView(approve,new LinearLayout.LayoutParams(0,-2,1));actions.addView(reject,new LinearLayout.LayoutParams(0,-2,1));box.addView(actions);}
        card.addView(box);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);rows.addView(card,lp);
    }}
    private void reject(PagoRevision p){EditText reason=new EditText(this);reason.setHint("Motivo obligatorio");new MaterialAlertDialogBuilder(this).setTitle("Rechazar pago").setMessage("Indica el motivo que verá el estudiante.").setView(reason).setNegativeButton("Cancelar",null).setPositiveButton("Continuar",(d,w)->{String value=reason.getText()==null?"":reason.getText().toString().trim();if(value.isEmpty()){Toast.makeText(this,"El motivo es obligatorio.",Toast.LENGTH_LONG).show();reject(p);return;}confirm(p,"RECHAZADO",value);}).show();}
    private void confirm(PagoRevision p,String decision,String reason){String message="APROBADO".equals(decision)?"El saldo oficial se actualizará una sola vez.":"El pago se rechazará y se liberará el importe reservado.";new MaterialAlertDialogBuilder(this).setTitle("Confirmar "+decision.toLowerCase(Locale.ROOT)).setMessage(message).setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->send(p,decision,reason)).show();}
    private void send(PagoRevision p,String decision,String reason){setBusy(true);reviewCall=api.revisarPago(p.id,new DecisionPago(decision,reason));reviewCall.enqueue(new Callback<Pago>(){public void onResponse(Call<Pago>c,Response<Pago>r){setBusy(false);if(r.isSuccessful()){Toast.makeText(RevisionPagosActivity.this,"Pago "+decision.toLowerCase(Locale.ROOT)+".",Toast.LENGTH_LONG).show();load();}else Toast.makeText(RevisionPagosActivity.this,"No se pudo revisar: "+r.code()+". Actualiza la bandeja.",Toast.LENGTH_LONG).show();}public void onFailure(Call<Pago>c,Throwable t){setBusy(false);Toast.makeText(RevisionPagosActivity.this,"No se pudo completar. La bandeja conserva el estado para reintentar.",Toast.LENGTH_LONG).show();}});}
    private void add(LinearLayout box,String value,boolean title){TextView t=new TextView(this);t.setText(value);t.setTextSize(title?17:14);t.setTextColor(getColor(R.color.dashboard_text_primary));if(title)t.setTypeface(null,1);t.setPadding(0,dp(4),0,dp(6));box.addView(t);}
    private String safe(String s){return s==null?"—":s;}private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void setBusy(boolean busy){progress.setVisibility(busy?View.VISIBLE:View.GONE);buscar.setEnabled(!busy);anterior.setEnabled(!busy&&page>0);siguiente.setEnabled(!busy&&page+1<totalPages);}
    @Override protected void onDestroy(){if(listCall!=null)listCall.cancel();if(reviewCall!=null)reviewCall.cancel();super.onDestroy();}
}
