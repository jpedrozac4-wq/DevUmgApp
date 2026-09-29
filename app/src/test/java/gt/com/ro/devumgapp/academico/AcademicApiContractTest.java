package gt.com.ro.devumgapp.academico;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.lang.reflect.Method;

import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
import retrofit2.http.GET;

public class AcademicApiContractTest {
    @Test public void allAcademicReadsUseJwtScopedMeRoutes() {
        assertGet("docenteMe", "api/academico/docente/me");
        assertGet("cursosDocente", "api/academico/docente/me/cursos");
        assertGet("estudiantesCurso", "api/academico/docente/me/cursos/{cursoId}/estudiantes", long.class);
        assertGet("notasCurso", "api/academico/docente/me/cursos/{cursoId}/notas", long.class);
        assertGet("estudianteMe", "api/academico/estudiante/me");
        assertGet("inscripcionesEstudiante", "api/academico/estudiante/me/inscripciones");
        assertGet("cursosEstudiante", "api/academico/estudiante/me/cursos");
        assertGet("notasEstudiante", "api/academico/estudiante/me/notas");
        assertGet("promedioEstudiante", "api/academico/estudiante/me/promedio");
        assertGet("colegiaturasEstudiante", "api/academico/estudiante/me/colegiaturas");
        assertGet("estadoCuentaEstudiante", "api/academico/estudiante/me/estado-cuenta");
    }

    private void assertGet(String methodName, String route, Class<?>... types) {
        try {
            Method method = AcademicoApiService.class.getMethod(methodName, types);
            assertEquals(route, method.getAnnotation(GET.class).value());
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }
}
