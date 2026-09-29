package gt.com.ro.devumgapp.auditoria;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import gt.com.ro.devumgapp.auditoria.network.AuditoriaApiService;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class AuditoriaApiContractTest {
    @Test public void auditRouteIncludesEveryDocumentedFilterAndPagination() throws Exception {
        Method method = AuditoriaApiService.class.getMethod("listar", String.class, String.class,
                String.class, String.class, String.class, String.class, Long.class, int.class, int.class);
        assertEquals("api/auditoria", method.getAnnotation(GET.class).value());
        String[] expected = {"fechaDesde", "fechaHasta", "usuario", "modulo", "accion", "tipoEntidad", "entidadId", "page", "size"};
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int index = 0; index < expected.length; index++) {
            Query query = null;
            for (Annotation annotation : annotations[index]) if (annotation instanceof Query) query = (Query) annotation;
            assertNotNull(query);
            assertEquals(expected[index], query.value());
        }
    }
}
