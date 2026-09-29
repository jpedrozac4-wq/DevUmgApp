package gt.com.ro.devumgapp.auth;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;

import org.junit.Test;

import gt.com.ro.devumgapp.auth.dto.LoginResponse;

public class LoginAcademicIdentityContractTest {
    @Test public void loginReadsOptionalAcademicProfileIds() {
        LoginResponse response = new Gson().fromJson("{\"usuarioId\":1,\"docenteId\":12,\"estudianteId\":34}", LoginResponse.class);
        assertEquals(Long.valueOf(12), response.docenteId);
        assertEquals(Long.valueOf(34), response.estudianteId);
    }

    @Test public void existingAccountsWithoutAcademicProfileRemainCompatible() {
        LoginResponse response = new Gson().fromJson("{\"usuarioId\":1}", LoginResponse.class);
        assertEquals(null, response.docenteId);
        assertEquals(null, response.estudianteId);
    }
}
