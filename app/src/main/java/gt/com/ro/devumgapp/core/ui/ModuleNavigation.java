package gt.com.ro.devumgapp.core.ui;

import android.app.Activity;
import android.content.Intent;
import com.google.android.material.appbar.MaterialToolbar;
import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.auth.ui.HomeActivity;

/** Opens the shared application drawer from top-level module screens. */
public final class ModuleNavigation {
    private ModuleNavigation() {}

    public static void attach(Activity activity, MaterialToolbar toolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        toolbar.setNavigationContentDescription(R.string.nav_open_drawer);
        toolbar.setNavigationOnClickListener(view -> {
            Intent intent = new Intent(activity, HomeActivity.class);
            intent.putExtra(HomeActivity.EXTRA_OPEN_DRAWER, true);
            intent.putExtra(HomeActivity.EXTRA_CURRENT_DESTINATION,
                    destinationFor(activity));
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        });
    }

    private static int destinationFor(Activity activity) {
        switch (activity.getClass().getSimpleName()) {
            case "CarreraListActivity": return R.id.nav_carreras;
            case "CursoListActivity": return R.id.nav_cursos;
            case "EstudianteListActivity": return R.id.nav_estudiantes;
            case "DocenteListActivity": return R.id.nav_docentes;
            case "InscripcionListActivity": return R.id.nav_inscripciones;
            case "NotaListActivity": return R.id.nav_notas;
            case "ColegiaturaListActivity": return R.id.nav_colegiaturas;
            case "UsuarioListActivity": return R.id.nav_usuarios;
            case "RolListActivity": return R.id.nav_roles;
            case "PermisoListActivity": return R.id.nav_permisos;
            case "AuditoriaActivity": return R.id.nav_auditoria;
            default: return R.id.nav_inicio;
        }
    }
}
