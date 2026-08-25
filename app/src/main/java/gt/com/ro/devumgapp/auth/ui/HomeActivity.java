package gt.com.ro.devumgapp.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.core.session.SessionManager;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private FrameLayout contentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        contentFrame = findViewById(R.id.contentFrame);
        NavigationView navView = findViewById(R.id.navView);

        // Drawer toggle (hamburger) integrated with the toolbar
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_open_drawer, R.string.nav_close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Header: datos del usuario logueado
        View header = navView.getHeaderView(0);
        TextView tvNavNombre = header.findViewById(R.id.tvNavNombre);
        TextView tvNavRoles = header.findViewById(R.id.tvNavRoles);

        SessionManager session = SessionManager.getInstance();
        tvNavNombre.setText(session.getNombreCompleto());
        tvNavRoles.setText(String.join(", ", session.getRoles()));

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_logout) {
                SessionManager.getInstance().logout();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }

            int moduleLabelRes = moduleLabelFor(id);
            if (moduleLabelRes != 0) {
                showModulePlaceholder(getString(moduleLabelRes));
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });

        // Selección inicial: Inicio
        navView.setCheckedItem(R.id.nav_inicio);
        showModulePlaceholder(getString(R.string.nav_inicio));
    }

    /** Maps a drawer item id to its module label resource, or 0 if unknown. */
    private int moduleLabelFor(int id) {
        // Note: AGP 9 makes R fields non-final, so switch-case is not allowed here.
        if (id == R.id.nav_inicio) {
            return R.string.nav_inicio;
        } else if (id == R.id.nav_estudiantes) {
            return R.string.nav_estudiantes;
        } else if (id == R.id.nav_docentes) {
            return R.string.nav_docentes;
        } else if (id == R.id.nav_carreras) {
            return R.string.nav_carreras;
        } else if (id == R.id.nav_cursos) {
            return R.string.nav_cursos;
        } else if (id == R.id.nav_inscripciones) {
            return R.string.nav_inscripciones;
        } else if (id == R.id.nav_colegiaturas) {
            return R.string.nav_colegiaturas;
        } else if (id == R.id.nav_notas) {
            return R.string.nav_notas;
        } else if (id == R.id.nav_usuarios) {
            return R.string.nav_usuarios;
        }
        return 0;
    }

    /** Replaces the content frame with a centered "under construction" placeholder. */
    private void showModulePlaceholder(String moduleName) {
        contentFrame.removeAllViews();

        TextView placeholder = new TextView(this);
        placeholder.setText(getString(R.string.module_under_construction, moduleName));
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setTextSize(18);
        placeholder.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));

        contentFrame.addView(placeholder, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }
}
