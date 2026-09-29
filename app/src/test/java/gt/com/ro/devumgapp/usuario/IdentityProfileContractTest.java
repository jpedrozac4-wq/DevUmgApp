package gt.com.ro.devumgapp.usuario;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;

import org.junit.Test;

import java.lang.reflect.Method;

import gt.com.ro.devumgapp.docente.dto.DocenteRequest;
import gt.com.ro.devumgapp.docente.dto.DocenteResponse;
import gt.com.ro.devumgapp.docente.network.DocenteApiService;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteRequest;
import gt.com.ro.devumgapp.estudiante.dto.EstudianteResponse;
import gt.com.ro.devumgapp.estudiante.network.EstudianteApiService;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class IdentityProfileContractTest {
    @Test public void jointTeacherAndStudentCreationUseProfileEndpoints() throws Exception {
        assertEquals("api/docentes", DocenteApiService.class.getMethod("crearDocente", DocenteRequest.class)
                .getAnnotation(POST.class).value());
        assertEquals("api/estudiantes", EstudianteApiService.class.getMethod("crearEstudiante", EstudianteRequest.class)
                .getAnnotation(POST.class).value());
    }

    @Test public void linkedProfileLookupUsesExistingPagedListContracts() throws Exception {
        Method docentes = gt.com.ro.devumgapp.docente.network.DocenteApiService.class
                .getMethod("listarDocentes", String.class, Boolean.class, int.class, int.class);
        assertEquals("api/docentes", docentes.getAnnotation(GET.class).value());
        assertQuery(docentes, 2, "page");
        assertQuery(docentes, 3, "size");

        Method estudiantes = EstudianteApiService.class.getMethod("listarEstudiantes",
                String.class, Boolean.class, int.class, int.class);
        assertEquals("api/estudiantes", estudiantes.getAnnotation(GET.class).value());
        assertQuery(estudiantes, 2, "pagina");
        assertQuery(estudiantes, 3, "tamanio");
    }

    private void assertQuery(Method method, int parameter, String expected) {
        Query query = method.getParameterAnnotations()[parameter].length == 0 ? null
                : (Query) method.getParameterAnnotations()[parameter][0];
        assertNotNull(query);
        assertEquals(expected, query.value());
    }

    @Test public void jointPayloadsIncludeProfileLinkFieldsWithoutLosingIdentityFields() {
        DocenteRequest docente = new DocenteRequest("D-1", "Ana", "López", "ana@example.com", "5555", "Matemática")
                .withAppAccess("ana", "password123");
        String docenteJson = new Gson().toJson(docente);
        assertTrue(docenteJson.contains("\"accesoApp\":true"));
        assertTrue(docenteJson.contains("\"username\":\"ana\""));
        assertTrue(docenteJson.contains("\"nombre\":\"Ana\""));

        EstudianteRequest estudiante = new EstudianteRequest();
        estudiante.codigoEstudiantil = "E-1";
        estudiante.nombres = "Luis";
        estudiante.correo = "luis@example.com";
        estudiante.accesoApp = true;
        estudiante.usuarioId = 24L;
        String estudianteJson = new Gson().toJson(estudiante);
        assertTrue(estudianteJson.contains("\"accesoApp\":true"));
        assertTrue(estudianteJson.contains("\"usuarioId\":24"));
        assertTrue(estudianteJson.contains("\"correo\":\"luis@example.com\""));
    }

    @Test public void academicEditsOmitIdentityFieldsFromBothRequests() {
        DocenteRequest docente = new DocenteRequest();
        docente.codigoDocente = "D-2";
        docente.telefono = "5555";
        docente.especialidad = "Física";
        String docenteJson = new Gson().toJson(docente);
        assertTrue(docenteJson.contains("\"codigoDocente\":\"D-2\""));
        assertTrue(!docenteJson.contains("nombre") && !docenteJson.contains("apellido")
                && !docenteJson.contains("email"));

        EstudianteRequest estudiante = new EstudianteRequest();
        estudiante.codigoEstudiantil = "E-2";
        estudiante.numeroIdentificacion = "123";
        estudiante.fechaNacimiento = "2000-01-01";
        estudiante.telefono = "5555";
        estudiante.direccion = "Zona 1";
        String estudianteJson = new Gson().toJson(estudiante);
        assertTrue(estudianteJson.contains("\"codigoEstudiantil\":\"E-2\""));
        assertTrue(!estudianteJson.contains("nombres") && !estudianteJson.contains("apellidos")
                && !estudianteJson.contains("correo"));
    }

    @Test public void profileResponsesMapCanonicalIdentityMetadata() {
        DocenteResponse docente = new Gson().fromJson(
                "{\"nombre\":\"Ana\",\"apellido\":\"López\",\"email\":\"ana@example.com\",\"identidadFuente\":\"USUARIO\",\"usuarioId\":4,\"accesoApp\":true}",
                DocenteResponse.class);
        assertEquals("USUARIO", docente.identidadFuente);
        assertEquals(Long.valueOf(4), docente.usuarioId);
        assertTrue(docente.accesoApp);
        assertEquals("Ana", docente.nombre);

        EstudianteResponse estudiante = new Gson().fromJson(
                "{\"nombres\":\"Luis\",\"apellidos\":\"Pérez\",\"correo\":\"luis@example.com\",\"identidadFuente\":\"PERFIL_HISTORICO\"}",
                EstudianteResponse.class);
        assertEquals("PERFIL_HISTORICO", estudiante.identidadFuente);
        assertEquals("Luis", estudiante.nombres);
    }
}
