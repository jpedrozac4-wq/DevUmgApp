package gt.com.ro.devumgapp.rol.ui;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Consistent grouping, readable labels and accent-insensitive search for permission codes. */
final class PermissionPresentation {
    static final List<String> MODULE_ORDER = Arrays.asList(
            "USUARIOS", "ROLES", "PERMISOS", "CARRERAS", "CURSOS",
            "ESTUDIANTES", "DOCENTES", "INSCRIPCIONES", "NOTAS", "COLEGIATURAS", "OTROS");
    private static final Map<String, String> MODULE_NAMES = new HashMap<>();
    private static final Map<String, String> ACTION_NAMES = new HashMap<>();

    static {
        MODULE_NAMES.put("USUARIOS", "Usuarios"); MODULE_NAMES.put("ROLES", "Roles");
        MODULE_NAMES.put("PERMISOS", "Permisos"); MODULE_NAMES.put("CARRERAS", "Carreras");
        MODULE_NAMES.put("CURSOS", "Cursos"); MODULE_NAMES.put("ESTUDIANTES", "Estudiantes");
        MODULE_NAMES.put("DOCENTES", "Docentes"); MODULE_NAMES.put("INSCRIPCIONES", "Inscripciones");
        MODULE_NAMES.put("NOTAS", "Notas"); MODULE_NAMES.put("COLEGIATURAS", "Colegiaturas");
        MODULE_NAMES.put("OTROS", "Otros");
        ACTION_NAMES.put("LEER", "Leer"); ACTION_NAMES.put("CREAR", "Crear");
        ACTION_NAMES.put("EDITAR", "Editar"); ACTION_NAMES.put("ELIMINAR", "Eliminar");
        ACTION_NAMES.put("CAMBIAR_ESTADO", "Cambiar estado de");
        ACTION_NAMES.put("ASIGNAR_ROLES", "Asignar roles de");
        ACTION_NAMES.put("ASIGNAR_PERMISOS", "Asignar permisos de");
        ACTION_NAMES.put("ASIGNAR_DOCENTE", "Asignar docente de");
        ACTION_NAMES.put("REGISTRAR_PAGO", "Registrar pago de");
    }

    private PermissionPresentation() {}

    static String moduleKey(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        for (String module : MODULE_ORDER) {
            if (!"OTROS".equals(module) && (normalized.equals(module) || normalized.startsWith(module + "_"))) return module;
        }
        return "OTROS";
    }

    static String moduleName(String code) { return MODULE_NAMES.get(moduleKey(code)); }

    static String displayName(String code, String backendName) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        String module = moduleKey(normalized);
        String action = "OTROS".equals(module) ? normalized : normalized.substring(Math.min(normalized.length(), module.length()));
        if (action.startsWith("_")) action = action.substring(1);
        String verb = ACTION_NAMES.get(action);
        if (verb != null) return verb + " " + MODULE_NAMES.get(module).toLowerCase(Locale.ROOT);
        if (backendName != null && !backendName.trim().isEmpty()) return backendName.trim();
        String readable = action.replace('_', ' ').toLowerCase(Locale.ROOT);
        return readable.isEmpty() ? normalized : Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }

    static boolean matches(String query, String code, String backendName) {
        String needle = normalize(query);
        if (needle.isEmpty()) return true;
        String haystack = normalize(code) + " " + normalize(backendName) + " "
                + normalize(moduleName(code)) + " " + normalize(displayName(code, backendName));
        return haystack.contains(needle);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
}
