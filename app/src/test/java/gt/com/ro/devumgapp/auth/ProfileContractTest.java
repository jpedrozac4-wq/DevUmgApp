package gt.com.ro.devumgapp.auth;

import static org.junit.Assert.*;
import com.google.gson.Gson;
import org.junit.Test;
import java.lang.reflect.Method;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import gt.com.ro.devumgapp.auth.dto.PasswordChangeRequest;
import gt.com.ro.devumgapp.auth.dto.ProfileUpdateRequest;
import gt.com.ro.devumgapp.auth.network.AuthApiService;
import retrofit2.http.PUT;

public class ProfileContractTest {
    private final Gson gson = new Gson();

    @Test public void meAcceptsCurrentAndHistoricalId() {
        assertEquals(7, gson.fromJson("{\"usuarioId\":7}", LoginResponse.class).usuarioId);
        assertEquals(8, gson.fromJson("{\"id\":8}", LoginResponse.class).usuarioId);
    }

    @Test public void updateRequestContainsOnlyEditableFields() {
        String json = gson.toJson(new ProfileUpdateRequest("ana", "a@b.com", "Ana", "López"));
        assertEquals(4, gson.fromJson(json, com.google.gson.JsonObject.class).size());
        assertFalse(json.contains("password"));
        assertFalse(json.contains("usuarioId"));
    }

    @Test public void passwordRequestUsesBackendFieldNames() {
        String json = gson.toJson(new PasswordChangeRequest("actual-segura", "nueva-segura"));
        assertTrue(json.contains("\"currentPassword\""));
        assertTrue(json.contains("\"newPassword\""));
    }

    @Test public void putEndpointsMatchBackendContract() throws Exception {
        Method profile = AuthApiService.class.getMethod("updateProfile", ProfileUpdateRequest.class);
        Method password = AuthApiService.class.getMethod("changePassword", PasswordChangeRequest.class);
        assertEquals("api/auth/me", profile.getAnnotation(PUT.class).value());
        assertEquals("api/auth/me/password", password.getAnnotation(PUT.class).value());
    }
}
