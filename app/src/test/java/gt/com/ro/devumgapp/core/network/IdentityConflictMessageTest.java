package gt.com.ro.devumgapp.core.network;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IdentityConflictMessageTest {
    @Test public void validationDetailsKeepsFieldMessagesFromApiErrorBody() {
        String body = "{\"status\":400,\"message\":\"Validation failed\",\"errors\":["
                + "{\"field\":\"codigoDocente\",\"defaultMessage\":\"must match pattern\"},"
                + "{\"field\":\"telefono\",\"message\":\"must not be blank\"}]}";

        String details = IdentityConflictMessage.validationDetails(body);

        assertTrue(details.contains("codigoDocente: must match pattern"));
        assertTrue(details.contains("telefono: must not be blank"));
    }

    @Test public void validationDetailsKeepsSpringFieldErrorMaps() {
        String body = "{\"message\":\"Bad Request\",\"errors\":{"
                + "\"codigoDocente\":\"must not be blank\",\"telefono\":[\"invalid format\"]}}";

        String details = IdentityConflictMessage.validationDetails(body);

        assertTrue(details.contains("codigoDocente: must not be blank"));
        assertTrue(details.contains("telefono: invalid format"));
    }
}
