package gt.com.ro.devumgapp.rol.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.permiso.dto.PermisoResumenResponse;

final class PermissionSelectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int GROUP = 0;
    private static final int PERMISSION = 1;
    private final PermissionSelectionState selection;
    private final Runnable onSelectionChanged;
    private final List<PermisoResumenResponse> all = new ArrayList<>();
    private final List<Row> visible = new ArrayList<>();
    private final Map<Long, Boolean> inactiveById = new HashMap<>();
    private String query = "";
    private boolean enabled = true;

    PermissionSelectionAdapter(PermissionSelectionState selection, Runnable onSelectionChanged) {
        this.selection = selection;
        this.onSelectionChanged = onSelectionChanged;
    }

    void submit(List<PermisoResumenResponse> permissions, Set<Long> inactiveIds) {
        all.clear(); all.addAll(permissions); inactiveById.clear();
        for (Long id : inactiveIds) inactiveById.put(id, true);
        rebuild();
    }

    void filter(String value) { query = value == null ? "" : value; rebuild(); }
    int visiblePermissionCount() { int count = 0; for (Row row : visible) if (row.permission != null) count++; return count; }
    void setItemsEnabled(boolean value) { enabled = value; notifyDataSetChanged(); }

    private void rebuild() {
        visible.clear();
        for (String module : PermissionPresentation.MODULE_ORDER) {
            List<PermisoResumenResponse> group = new ArrayList<>();
            for (PermisoResumenResponse permission : all) {
                if (module.equals(PermissionPresentation.moduleKey(permission.codigo))
                        && PermissionPresentation.matches(query, permission.codigo, permission.nombre)) group.add(permission);
            }
            if (!group.isEmpty()) {
                visible.add(Row.group(PermissionPresentation.moduleName(module)));
                for (PermisoResumenResponse permission : group) visible.add(Row.permission(permission));
            }
        }
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) { return visible.get(position).permission == null ? GROUP : PERMISSION; }
    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        int layout = type == GROUP ? R.layout.item_permission_group : R.layout.item_permission_select;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return type == GROUP ? new GroupHolder(view) : new PermissionHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = visible.get(position);
        if (holder instanceof GroupHolder) ((GroupHolder) holder).title.setText(row.group);
        else ((PermissionHolder) holder).bind(row.permission);
    }
    @Override public int getItemCount() { return visible.size(); }

    private final class PermissionHolder extends RecyclerView.ViewHolder {
        final TextView name, code;
        final MaterialCheckBox check;
        PermissionHolder(View view) { super(view); name=view.findViewById(R.id.txtPermissionName); code=view.findViewById(R.id.txtPermissionCode); check=view.findViewById(R.id.chkPermission); }
        void bind(PermisoResumenResponse permission) {
            String readableName = PermissionPresentation.displayName(permission.codigo, permission.nombre);
            name.setText(readableName);
            code.setText(Boolean.TRUE.equals(inactiveById.get(permission.id))
                    ? itemView.getContext().getString(R.string.rol_permiso_codigo_inactivo, permission.codigo)
                    : permission.codigo);
            check.setOnCheckedChangeListener(null); check.setChecked(selection.contains(permission.id));
            check.setContentDescription(readableName);
            check.setEnabled(enabled); itemView.setEnabled(enabled); itemView.setAlpha(enabled ? 1f : .55f);
            View.OnClickListener toggle = view -> {
                if (!enabled) return;
                selection.toggle(permission.id);
                check.setChecked(selection.contains(permission.id)); onSelectionChanged.run();
            };
            itemView.setOnClickListener(toggle); check.setOnClickListener(toggle);
        }
    }
    private static final class GroupHolder extends RecyclerView.ViewHolder { final TextView title; GroupHolder(View view){ super(view); title=view.findViewById(R.id.txtPermissionGroup); } }
    private static final class Row { final String group; final PermisoResumenResponse permission; private Row(String group, PermisoResumenResponse permission){this.group=group;this.permission=permission;} static Row group(String value){return new Row(value,null);} static Row permission(PermisoResumenResponse value){return new Row(null,value);} }
}
