package gt.com.ro.devumgapp.inscripcion.ui;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import gt.com.ro.devumgapp.R;

/**
 * Dropdown adapter for searchable catalogs. Unlike the default ArrayAdapter
 * filter (prefix match), it matches the typed query as a substring anywhere in
 * the label, case-insensitive, so searching by last name still matches
 * "EST-001 - Ana Maria Garcia Lopez".
 */
class BusquedaAdapter<T> extends ArrayAdapter<NamedItem<T>> {

    private final List<NamedItem<T>> originalItems;

    BusquedaAdapter(Context context, List<NamedItem<T>> items) {
        super(context, R.layout.item_spinner_dropdown, new ArrayList<>(items));
        this.originalItems = new ArrayList<>(items);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<NamedItem<T>> filtrados = new ArrayList<>();
                String q = constraint == null ? "" : constraint.toString().trim().toLowerCase(Locale.US);
                for (NamedItem<T> item : originalItems) {
                    if (q.isEmpty() || item.toString().toLowerCase(Locale.US).contains(q)) {
                        filtrados.add(item);
                    }
                }
                results.values = filtrados;
                results.count = filtrados.size();
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                addAll((List<NamedItem<T>>) results.values);
                notifyDataSetChanged();
            }
        };
    }
}
