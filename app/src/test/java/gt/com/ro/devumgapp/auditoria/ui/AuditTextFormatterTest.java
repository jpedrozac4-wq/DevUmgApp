package gt.com.ro.devumgapp.auditoria.ui;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

public class AuditTextFormatterTest {
    @Test public void identifiesOnlyFieldsWhoseValuesChanged() {
        JsonObject row = new JsonParser().parse("{"
                + "\"valorAnterior\":{\"nombre\":\"Ana\",\"apellido\":\"López\",\"id\":7},"
                + "\"valorNuevo\":{\"nombre\":\"Ana\",\"apellido\":\"Pérez\",\"id\":7}}")
                .getAsJsonObject();

        assertEquals("apellido", AuditTextFormatter.editedFields(row));
    }

    @Test public void supportsSerializedAuditValues() {
        JsonObject row = new JsonObject();
        row.addProperty("valorAnterior", "{\"email\":\"antes@test.com\"}");
        row.addProperty("valorNuevo", "{\"email\":\"nuevo@test.com\"}");

        assertEquals("correo electrónico", AuditTextFormatter.editedFields(row));
    }

    @Test public void removesHttpMethodButKeepsUsefulDescription() {
        assertEquals("Usuario actualizado",
                AuditTextFormatter.visibleDescription("Método utilizado: PUT\nUsuario actualizado"));
        assertEquals("", AuditTextFormatter.visibleDescription("Método HTTP: PATCH"));
        assertEquals("", AuditTextFormatter.visibleDescription("PUT /api/auth/perfil"));
        assertEquals("PUT", AuditTextFormatter.httpMethod("PUT /api/auth/perfil"));
    }
}
