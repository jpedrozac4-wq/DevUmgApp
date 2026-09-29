package gt.com.ro.devumgapp.academico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import gt.com.ro.devumgapp.academico.network.AcademicoApiService;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class AcademicApiContractTest {
    @Test public void allAcademicReadsUseJwtScopedMeRoutes() throws Exception {
        assertGet("docenteMe", "api/academico/docente/me");
        assertGet("cursosDocente", "api/academico/docente/me/cursos", Integer.class);
        assertGet("estudiantesCurso", "api/academico/docente/me/cursos/{cursoId}/estudiantes",
                long.class, Integer.class, int.class, int.class);
        assertGet("notasCurso", "api/academico/docente/me/cursos/{cursoId}/notas",
                long.class, Integer.class, int.class, int.class);
        assertGet("estudianteMe", "api/academico/estudiante/me");
        assertGet("carreraEstudiante", "api/academico/estudiante/me/carrera");
        assertGet("planCarreraEstudiante", "api/academico/estudiante/me/plan-carrera");
        assertGet("cursosInscritosEstudiante", "api/academico/estudiante/me/cursos-inscritos");
        assertGet("docentesEstudiante", "api/academico/estudiante/me/docentes");
        assertGet("inscripcionesEstudiante", "api/academico/estudiante/me/inscripciones", int.class, int.class);
        assertGet("notasEstudiante", "api/academico/estudiante/me/notas", int.class, int.class);
        assertGet("promedioEstudiante", "api/academico/estudiante/me/promedio");
    }

    @Test public void documentedFiltersAndPaginationAreSent() throws Exception {
        assertQueries("cursosDocente", new Class<?>[]{Integer.class}, "cicloAnio");
        assertQueries("estudiantesCurso", new Class<?>[]{long.class, Integer.class, int.class, int.class}, null, "cicloAnio", "page", "size");
        assertQueries("notasCurso", new Class<?>[]{long.class, Integer.class, int.class, int.class}, null, "cicloAnio", "page", "size");
        assertQueries("inscripcionesEstudiante", new Class<?>[]{int.class, int.class}, "page", "size");
        assertQueries("notasEstudiante", new Class<?>[]{int.class, int.class}, "page", "size");
    }

    private void assertGet(String methodName, String route, Class<?>... types) throws Exception {
        Method method = AcademicoApiService.class.getMethod(methodName, types);
        assertEquals(route, method.getAnnotation(GET.class).value());
    }

    private void assertQueries(String methodName, Class<?>[] types, String... expected) throws Exception {
        Annotation[][] annotations = AcademicoApiService.class.getMethod(methodName, types).getParameterAnnotations();
        for (int index = 0; index < expected.length; index++) {
            if (expected[index] == null) continue;
            Query query = null;
            for (Annotation annotation : annotations[index]) if (annotation instanceof Query) query = (Query) annotation;
            assertNotNull(query);
            assertEquals(expected[index], query.value());
        }
    }
}
