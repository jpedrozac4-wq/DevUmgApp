package gt.com.ro.devumgapp.academico.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;

public final class AcademicJson {
    private AcademicJson() { }

    public static JsonArray array(JsonElement element) {
        if (element == null || element.isJsonNull()) return new JsonArray();
        if (element.isJsonArray()) return element.getAsJsonArray();
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : new String[]{"content", "datos", "items", "cursos", "estudiantes", "notas", "inscripciones", "colegiaturas"}) {
                JsonElement value = object.get(key);
                if (value != null && value.isJsonArray()) return value.getAsJsonArray();
            }
        }
        return new JsonArray();
    }

    public static JsonObject object(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    public static String text(JsonObject object, String... keys) {
        if (object == null) return "";
        for (String key : keys) {
            JsonElement value = object.get(key);
            if (value != null && !value.isJsonNull() && value.isJsonPrimitive()) return value.getAsString();
        }
        return "";
    }

    public static long id(JsonObject object, String... keys) {
        String value = text(object, keys);
        try { return Long.parseLong(value); } catch (Exception ignored) { return -1L; }
    }
}
