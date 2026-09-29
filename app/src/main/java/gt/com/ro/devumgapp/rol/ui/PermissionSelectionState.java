package gt.com.ro.devumgapp.rol.ui;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Selection source of truth; filtering never mutates it. */
final class PermissionSelectionState {
    private final Set<Long> ids = new HashSet<>();
    void replace(Collection<Long> values) { ids.clear(); ids.addAll(values); }
    void toggle(long id) { if (!ids.remove(id)) ids.add(id); }
    boolean contains(long id) { return ids.contains(id); }
    int size() { return ids.size(); }
    Set<Long> snapshot() { return new HashSet<>(ids); }
}
