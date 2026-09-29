package gt.com.ro.devumgapp.rol.ui;

import static org.junit.Assert.*;
import org.junit.Test;

public class PermissionPresentationTest {
    @Test public void groupsAndCreatesReadableNames() {
        assertEquals("Usuarios", PermissionPresentation.moduleName("USUARIOS_LEER"));
        assertEquals("Leer usuarios", PermissionPresentation.displayName("USUARIOS_LEER", "technical"));
        assertEquals("Registrar pago de colegiaturas", PermissionPresentation.displayName("COLEGIATURAS_REGISTRAR_PAGO", null));
    }

    @Test public void searchMatchesNameModuleCodeAndIgnoresAccents() {
        assertTrue(PermissionPresentation.matches("usuarios", "USUARIOS_LEER", "Lectura"));
        assertTrue(PermissionPresentation.matches("leer", "USUARIOS_LEER", "Lectura"));
        assertTrue(PermissionPresentation.matches("inscripcion", "INSCRIPCIONES_EDITAR", "Edición"));
        assertTrue(PermissionPresentation.matches("edicion", "OTRO", "Edición avanzada"));
        assertFalse(PermissionPresentation.matches("notas", "CURSOS_LEER", "Leer cursos"));
    }
}
