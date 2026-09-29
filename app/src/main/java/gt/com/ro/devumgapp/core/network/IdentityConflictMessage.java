package gt.com.ro.devumgapp.core.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

/** Presents identity conflicts without attempting to overwrite either side of the link. */
public final class IdentityConflictMessage {
    private IdentityConflictMessage() { }

    public static String fromResponse(Response<?> response) {
        String body = readBody(response);
        String backend = message(body);
        if (response.code() != 409) {
            String validation = validationDetails(body);
            if (!validation.isEmpty() && !validation.equals(backend)) {
                return (backend.isEmpty() ? "" : backend + "\n") + validation;
            }
        }
        if (response.code() == 409) {
            String detail = conflicts(body);
            String base = backend.isEmpty() ? "La identidad de la cuenta contradice los datos del perfil." : backend;
            return base + (detail.isEmpty() ? "" : "\nDatos en conflicto: " + detail)
                    + "\nRevisa la cuenta y el perfil con administración. No se realizó ningún reemplazo automático.";
        }
        return backend.isEmpty() ? "No fue posible completar la operación (HTTP " + response.code() + ")." : backend;
    }

    private static String readBody(Response<?> response) {
        try { return response.errorBody() == null ? "" : response.errorBody().string(); }
        catch (IOException ignored) { return ""; }
    }

    private static JsonObject object(String body) {
        try {
            JsonElement value = new JsonParser().parse(body);
            return value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
        } catch (Exception ignored) { return new JsonObject(); }
    }

    private static String message(String body) {
        JsonElement value = object(body).get("message");
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    static String validationDetails(String body) {
        JsonObject root = object(body);
        StringBuilder result = new StringBuilder();
        for (String key : root.keySet()) {
            if (key.equals("message") || key.equals("status") || key.equals("error")
                    || key.equals("code") || key.equals("path") || key.equals("timestamp")) continue;
            JsonElement value = root.get(key);
            if (value == null || value.isJsonNull()) continue;
            if (key.equals("errors") || key.equals("fieldErrors") || key.equals("validationErrors")) {
                appendErrors(result, value);
            } else if (value.isJsonPrimitive()) {
                append(result, key + ": " + value.getAsString());
            }
        }
        return result.toString();
    }

    private static void appendErrors(StringBuilder result, JsonElement errors) {
        if (errors.isJsonArray()) {
            for (JsonElement error : errors.getAsJsonArray()) {
                if (error.isJsonObject()) {
                    JsonObject item = error.getAsJsonObject();
                    JsonElement field = first(item, "field", "property", "name");
                    JsonElement detail = first(item, "defaultMessage", "message", "error", "code");
                    append(result, (field == null ? "" : field.getAsString() + ": ")
                            + (detail == null ? item.toString() : detail.getAsString()));
                } else if (error.isJsonPrimitive()) append(result, error.getAsString());
            }
        } else if (errors.isJsonObject()) {
            for (String field : errors.getAsJsonObject().keySet()) {
                JsonElement detail = errors.getAsJsonObject().get(field);
                if (detail == null || detail.isJsonNull()) continue;
                if (detail.isJsonArray()) {
                    for (JsonElement message : detail.getAsJsonArray()) append(result, field + ": " + message.getAsString());
                } else append(result, field + ": " + detail.getAsString());
            }
        } else if (errors.isJsonPrimitive()) append(result, errors.getAsString());
    }

    private static JsonElement first(JsonObject object, String... names) {
        for (String name : names) {
            JsonElement value = object.get(name);
            if (value != null && !value.isJsonNull()) return value;
        }
        return null;
    }

    private static void append(StringBuilder target, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (target.length() > 0) target.append("\n");
        target.append(value);
    }

    private static String conflicts(String body) {
        JsonObject root = object(body);
        List<String> values = new ArrayList<>();
        for (String key : root.keySet()) {
            if (key.equals("message") || key.equals("status") || key.equals("error")
                    || key.equals("code") || key.equals("path") || key.equals("timestamp")) continue;
            JsonElement value = root.get(key);
            if (value != null && !value.isJsonNull()) values.add(key + ": " + value.toString());
        }
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append("; ");
            result.append(value);
        }
        return result.toString();
    }
}
