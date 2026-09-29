package gt.com.ro.devumgapp.rol.ui;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

public class PermissionSelectionStateTest {
    @Test public void selectionSurvivesUnrelatedFiltering() {
        PermissionSelectionState state = new PermissionSelectionState();
        state.toggle(11L);
        assertTrue(PermissionPresentation.matches("roles", "ROLES_LEER", "Leer roles"));
        assertFalse(PermissionPresentation.matches("roles", "USUARIOS_LEER", "Leer usuarios"));
        assertTrue(state.contains(11L));
        assertEquals(1, state.size());
    }

    @Test public void loadsExistingAndSavesExactFinalSelection() {
        PermissionSelectionState state = new PermissionSelectionState();
        state.replace(Arrays.asList(1L, 2L, 3L));
        state.toggle(2L);
        state.toggle(4L);
        assertEquals(new java.util.HashSet<>(Arrays.asList(1L, 3L, 4L)), state.snapshot());
    }
}
