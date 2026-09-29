package gt.com.ro.devumgapp.usuario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.util.Collections;

import gt.com.ro.devumgapp.usuario.dto.UsuarioAltaConjuntaRequest;
import gt.com.ro.devumgapp.usuario.network.UsuarioApiService;
import retrofit2.http.POST;

public class AltaConjuntaContractTest {
    private final Gson gson = new Gson();

    @Test public void endpointIsTheSingleJointCreationPost() throws Exception {
        POST post = UsuarioApiService.class.getMethod("altaConjunta", UsuarioAltaConjuntaRequest.class)
                .getAnnotation(POST.class);
        assertEquals("/api/usuarios/alta-conjunta", post.value());
    }

    @Test public void teacherRequestHasOneRoleAndOnlyTeacherProfileBlock() {
        UsuarioAltaConjuntaRequest request = base();
        request.docente = new UsuarioAltaConjuntaRequest.Docente();
        request.docente.codigoDocente = "D-2026-1";
        request.docente.telefono = "55551234";
        request.docente.especialidad = "Matemática";

        JsonObject json = new JsonParser().parse(gson.toJson(request)).getAsJsonObject();

        assertEquals("teacher@example.test", json.get("correo").getAsString());
        assertEquals(1, json.getAsJsonArray("rolIds").size());
        assertTrue(json.has("docente"));
        assertFalse(json.has("estudiante"));
        assertFalse(json.has("email"));
    }

    @Test public void studentRequestHasOneRoleAndOnlyStudentProfileBlock() {
        UsuarioAltaConjuntaRequest request = base();
        request.estudiante = new UsuarioAltaConjuntaRequest.Estudiante();
        request.estudiante.codigoEstudiantil = "E-2026-1";
        request.estudiante.numeroIdentificacion = "3012345670101";
        request.estudiante.fechaNacimiento = "2000-01-31";
        request.estudiante.telefono = "55551234";
        request.estudiante.direccion = "Ciudad de Guatemala";

        JsonObject json = new JsonParser().parse(gson.toJson(request)).getAsJsonObject();

        assertEquals(1, json.getAsJsonArray("rolIds").size());
        assertTrue(json.has("estudiante"));
        assertFalse(json.has("docente"));
    }

    @Test public void nonAcademicRoleOmitsBothProfileBlocks() {
        JsonObject json = new JsonParser().parse(gson.toJson(base())).getAsJsonObject();

        assertEquals(1, json.getAsJsonArray("rolIds").size());
        assertFalse(json.has("docente"));
        assertFalse(json.has("estudiante"));
    }

    private UsuarioAltaConjuntaRequest base() {
        UsuarioAltaConjuntaRequest request = new UsuarioAltaConjuntaRequest();
        request.username = "testuser";
        request.password = "Secure123*";
        request.nombre = "Test";
        request.apellido = "User";
        request.correo = "teacher@example.test";
        request.rolIds = Collections.singletonList(7L);
        return request;
    }
}
