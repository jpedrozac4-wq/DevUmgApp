package gt.com.ro.devumgapp.auditoria.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AuditTextFormatter {
    private AuditTextFormatter() { }

    static String editedFields(JsonObject row) {
        JsonObject previous = object(row, "valorAnterior", "datosAnteriores", "before");
        JsonObject current = object(row, "valorNuevo", "datosNuevos", "after");
        if (previous == null || current == null) return "";

        Set<String> changed = new LinkedHashSet<>();
        for (String key : current.keySet()) {
            if (isTechnicalField(key)) continue;
            JsonElement before = previous.get(key);
            JsonElement after = current.get(key);
            if (!same(before, after)) changed.add(fieldLabel(key));
        }
        for (String key : previous.keySet()) {
            if (!current.has(key) && !isTechnicalField(key)) changed.add(fieldLabel(key));
        }
        return naturalList(new ArrayList<>(changed));
    }

    static String visibleDescription(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String[] lines = raw.split("\\r?\\n");
        List<String> visible = new ArrayList<>();
        for (String line : lines) {
            String normalized = normalize(line);
            if (normalized.startsWith("metodo utilizado")
                    || normalized.startsWith("metodo http")
                    || normalized.startsWith("http method")
                    || isHttpRequestLine(normalized)) continue;
            if (!line.trim().isEmpty()) visible.add(line.trim());
        }
        return join(visible, "\n");
    }

    static String httpMethod(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toUpperCase(Locale.ROOT);
        for (String method : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE"}) {
            if (value.equals(method) || value.startsWith(method + " ") || value.startsWith(method + "/")) {
                return method;
            }
        }
        return "";
    }

    private static boolean isHttpRequestLine(String value) {
        for (String method : new String[]{"get", "post", "put", "patch", "delete"}) {
            if (value.startsWith(method + " /") || value.startsWith(method + ": /")) return true;
        }
        return false;
    }

    private static JsonObject object(JsonObject row, String... names) {
        for (String name : names) {
            JsonElement value = row.get(name);
            if (value == null || value.isJsonNull()) continue;
            if (value.isJsonObject()) return value.getAsJsonObject();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                try {
                    JsonElement parsed = new JsonParser().parse(value.getAsString());
                    if (parsed.isJsonObject()) return parsed.getAsJsonObject();
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private static boolean same(JsonElement first, JsonElement second) {
        if (first == null || first.isJsonNull()) return second == null || second.isJsonNull();
        return first.equals(second);
    }

    private static boolean isTechnicalField(String key) {
        String value = normalize(key).replace("_", "");
        return value.equals("id") || value.endsWith("id") || value.equals("fechacreacion")
                || value.equals("fechaactualizacion") || value.equals("createdat")
                || value.equals("updatedat") || value.equals("password")
                || value.equals("contrasena");
    }

    private static String fieldLabel(String key) {
        String spaced = key.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ');
        String normalized = normalize(spaced);
        if (normalized.equals("apellidos")) return "apellidos";
        if (normalized.equals("nombres")) return "nombres";
        if (normalized.equals("apellido")) return "apellido";
        if (normalized.equals("nombre")) return "nombre";
        if (normalized.equals("email") || normalized.equals("correo")) return "correo electrónico";
        if (normalized.equals("username") || normalized.equals("usuario")) return "nombre de usuario";
        if (normalized.equals("activo") || normalized.equals("estado")) return "estado";
        return spaced.trim().toLowerCase(Locale.ROOT);
    }

    private static String naturalList(List<String> values) {
        if (values.isEmpty()) return "";
        if (values.size() == 1) return values.get(0);
        if (values.size() == 2) return values.get(0) + " y " + values.get(1);
        return join(values.subList(0, values.size() - 1), ", ") + " y " + values.get(values.size() - 1);
    }

    private static String join(List<String> values, String separator) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(separator);
            result.append(value);
        }
        return result.toString();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n')
                .trim();
    }
}
