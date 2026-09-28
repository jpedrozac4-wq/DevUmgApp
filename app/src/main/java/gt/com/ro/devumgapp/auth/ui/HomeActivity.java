package gt.com.ro.devumgapp.auth.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.colegiatura.ui.ColegiaturaListActivity;
import gt.com.ro.devumgapp.estudiante.ui.EstudianteListActivity;
import gt.com.ro.devumgapp.carrera.ui.CarreraListActivity;
import gt.com.ro.devumgapp.core.session.SessionManager;
import gt.com.ro.devumgapp.curso.ui.CursoListActivity;
import gt.com.ro.devumgapp.docente.ui.DocenteListActivity;
import gt.com.ro.devumgapp.inscripcion.ui.InscripcionListActivity;
import gt.com.ro.devumgapp.nota.ui.NotaListActivity;
import gt.com.ro.devumgapp.usuario.ui.UsuarioListActivity;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private GridLayout modulesGrid;
    private View dashboardHeader;
    private View txtModulesTitle;
    private TextView txtGreeting;
    private TextView txtUserIdentity;
    private TextView txtToolbarTitle;
    private TextView txtToolbarAvatar;
    private SessionManager sessionManager;
    private boolean syncingBottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = SessionManager.getInstance();

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_home);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        bindViews();
        setupToolbar();
        setupDrawer();
        applySystemInsets();
        setupBottomNavigation();
        setupBackHandling();
        renderUserSummary();
        renderModules();
        animateDashboardIntro();
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        modulesGrid = findViewById(R.id.modulesGrid);
        dashboardHeader = findViewById(R.id.dashboardHeader);
        txtModulesTitle = findViewById(R.id.txtModulesTitle);
        txtGreeting = findViewById(R.id.txtGreeting);
        txtUserIdentity = findViewById(R.id.txtUserIdentity);
        txtToolbarTitle = findViewById(R.id.txtToolbarTitle);
        txtToolbarAvatar = findViewById(R.id.txtToolbarAvatar);
    }

    private void setupToolbar() {
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.nav_open_drawer,
                R.string.nav_close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        Drawable navigationIcon = toolbar.getNavigationIcon();
        if (navigationIcon != null) {
            navigationIcon.setTint(ContextCompat.getColor(this, R.color.dashboard_text_primary));
        }
    }

    private void setupDrawer() {
        renderDrawerHeader();
        navView.setCheckedItem(R.id.nav_inicio);
        navView.setNavigationItemSelectedListener(item -> {
            handleNavigationItem(item);
            return true;
        });
    }

    private void applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (view, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            int baseHeight = getResources().getDimensionPixelSize(R.dimen.dashboard_bottom_nav_height);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = baseHeight + bottomInset;
            view.setLayoutParams(layoutParams);
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), bottomInset);
            return insets;
        });
        ViewCompat.requestApplyInsets(bottomNavigation);
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void renderDrawerHeader() {
        View header = navView.getHeaderView(0);
        TextView tvNavNombre = header.findViewById(R.id.tvNavNombre);
        TextView tvNavRoles = header.findViewById(R.id.tvNavRoles);
        TextView tvNavAvatar = header.findViewById(R.id.tvNavAvatar);

        String displayName = getDisplayName();
        tvNavAvatar.setText(getInitial(displayName));
        tvNavNombre.setText(displayName.isEmpty()
                ? getString(R.string.dashboard_user_fallback)
                : displayName);

        String roles = formatRoles();
        tvNavRoles.setText(roles.isEmpty()
                ? getString(R.string.dashboard_roles_fallback)
                : roles);
    }

    private void renderUserSummary() {
        String displayName = getDisplayName();
        txtToolbarAvatar.setText(getInitial(displayName));
        txtGreeting.setText(displayName.isEmpty()
                ? getString(R.string.dashboard_greeting_default)
                : getString(R.string.dashboard_greeting_named, displayName));

        String username = sessionManager.getUsername().trim();
        String roles = formatRoles();
        List<String> identityRows = new ArrayList<>();
        if (!username.isEmpty()) {
            identityRows.add(getString(R.string.dashboard_user_label, username));
        }
        if (!roles.isEmpty()) {
            identityRows.add(getString(R.string.dashboard_role_label, roles));
        }
        txtUserIdentity.setText(String.join("\n", identityRows));
        txtUserIdentity.setVisibility(identityRows.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderModules() {
        modulesGrid.removeAllViews();
        List<DashboardModule> modules = createModules();

        for (int index = 0; index < modules.size(); index++) {
            DashboardModule module = modules.get(index);
            MaterialCardView card = (MaterialCardView) getLayoutInflater()
                    .inflate(R.layout.item_dashboard_module, modulesGrid, false);

            ImageView icon = card.findViewById(R.id.imgModuleIcon);
            TextView name = card.findViewById(R.id.txtModuleName);
            TextView description = card.findViewById(R.id.txtModuleDescription);
            View iconContainer = card.findViewById(R.id.iconContainer);

            icon.setImageResource(module.iconRes);
            name.setText(module.titleRes);
            description.setText(module.descriptionRes);
            applyModuleStyle(module, card, icon, iconContainer);
            card.setContentDescription(getString(R.string.dashboard_module_available,
                    getString(module.titleRes)));
            card.setOnClickListener(view -> animateModulePress(card, () -> openModule(module)));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(
                    getHorizontalMargin(index),
                    getResources().getDimensionPixelSize(R.dimen.dashboard_grid_gap) / 2,
                    getHorizontalMargin(index + 1),
                    getResources().getDimensionPixelSize(R.dimen.dashboard_grid_gap) / 2);
            card.setLayoutParams(params);
            modulesGrid.addView(card);
            animateModuleEntrance(card, index);
        }
    }

    private void applyModuleStyle(
            DashboardModule module,
            MaterialCardView card,
            ImageView icon,
            View iconContainer) {
        int accentColor = ContextCompat.getColor(this, module.accentColorRes);
        int iconAccent = blendWithWhite(accentColor, 0.12f);

        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.dashboard_border));
        icon.setImageTintList(ColorStateList.valueOf(accentColor));

        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setColor(iconAccent);
        iconBackground.setCornerRadius(getResources().getDimension(R.dimen.dashboard_card_radius));
        iconContainer.setBackground(iconBackground);
    }

    private void animateModuleEntrance(View card, int index) {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.dashboard_item_enter);
        animation.setStartOffset((long) index * 55L);
        card.startAnimation(animation);
    }

    private void animateDashboardIntro() {
        dashboardHeader.setAlpha(0f);
        dashboardHeader.setTranslationY(22f);
        dashboardHeader.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .start();

        txtModulesTitle.setAlpha(0f);
        txtModulesTitle.setTranslationY(14f);
        txtModulesTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(130)
                .setDuration(300)
                .start();
    }

    private void animateModulePress(View view, Runnable endAction) {
        view.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(80)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(130)
                        .withEndAction(endAction)
                        .start())
                .start();
    }

    private int getHorizontalMargin(int index) {
        int gap = getResources().getDimensionPixelSize(R.dimen.dashboard_grid_gap);
        return index % 2 == 0 ? 0 : gap / 2;
    }

    private void handleNavigationItem(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_logout) {
            drawerLayout.closeDrawer(GravityCompat.START);
            showLogoutConfirmation();
            return;
        }

        navView.setCheckedItem(itemId);
        syncBottomSelection(itemId);
        drawerLayout.closeDrawer(GravityCompat.START);
        setDashboardTitle(itemId == R.id.nav_inicio
                ? R.string.dashboard_title
                : moduleTitleFor(itemId));

        if (itemId == R.id.nav_inicio) {
            return;
        }

        if (!openModule(itemId)) {
            Toast.makeText(this, R.string.dashboard_module_coming_soon, Toast.LENGTH_SHORT).show();
        }
    }

    private void openModule(DashboardModule module) {
        navView.setCheckedItem(module.menuItemId);
        syncBottomSelection(module.menuItemId);
        setDashboardTitle(module.titleRes);
        if (!openModule(module.menuItemId)) {
            Toast.makeText(this, R.string.dashboard_module_coming_soon, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (syncingBottomNavigation) {
                return true;
            }
            if (itemId == R.id.nav_inicio) {
                navView.setCheckedItem(R.id.nav_inicio);
                setDashboardTitle(R.string.dashboard_title);
                return true;
            }
            if (itemId == R.id.nav_profile) {
                setDashboardTitle(R.string.bottom_profile);
                Toast.makeText(this, R.string.dashboard_module_coming_soon, Toast.LENGTH_SHORT).show();
                return true;
            }
            navView.setCheckedItem(itemId);
            setDashboardTitle(moduleTitleFor(itemId));
            if (!openModule(itemId)) {
                Toast.makeText(this, R.string.dashboard_module_coming_soon, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_inicio);
    }

    private boolean openModule(int itemId) {
        Intent intent = moduleIntentFor(itemId);
        if (intent == null) {
            return false;
        }
        startActivity(intent);
        return true;
    }

    private Intent moduleIntentFor(int itemId) {
        if (itemId == R.id.nav_carreras) {
            return new Intent(this, CarreraListActivity.class);
        }
        if (itemId == R.id.nav_cursos) {
            return new Intent(this, CursoListActivity.class);
        }
        if (itemId == R.id.nav_inscripciones) {
            return new Intent(this, InscripcionListActivity.class);
        }
        if (itemId == R.id.nav_notas) {
            return new Intent(this, NotaListActivity.class);
        }
        if (itemId == R.id.nav_colegiaturas) {
            return new Intent(this, ColegiaturaListActivity.class);
        }
        if (itemId == R.id.nav_estudiantes) {
            return new Intent(this, EstudianteListActivity.class);
        }
        if (itemId == R.id.nav_docentes) {
            return new Intent(this, DocenteListActivity.class);
        }
        if (itemId == R.id.nav_usuarios) {
            return new Intent(this, UsuarioListActivity.class);
        }
        return null;
    }

    private void syncBottomSelection(int itemId) {
        if (itemId == R.id.nav_inicio
                || itemId == R.id.nav_cursos
                || itemId == R.id.nav_estudiantes
                || itemId == R.id.nav_notas) {
            syncingBottomNavigation = true;
            try {
                bottomNavigation.setSelectedItemId(itemId);
            } finally {
                syncingBottomNavigation = false;
            }
        }
    }

    private void setDashboardTitle(int titleRes) {
        txtToolbarTitle.setText(titleRes);
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dashboard_logout_title)
                .setMessage(R.string.dashboard_logout_message)
                .setNegativeButton(R.string.dashboard_logout_cancel, null)
                .setPositiveButton(R.string.dashboard_logout_confirm, (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        sessionManager.logout();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int moduleTitleFor(int itemId) {
        for (DashboardModule module : createModules()) {
            if (module.menuItemId == itemId) {
                return module.titleRes;
            }
        }
        return R.string.dashboard_title;
    }

    private List<DashboardModule> createModules() {
        List<DashboardModule> modules = new ArrayList<>();
        modules.add(new DashboardModule(
                R.id.nav_carreras,
                R.string.nav_carreras,
                R.string.module_carreras_description,
                R.drawable.ic_career,
                R.color.dashboard_career));
        modules.add(new DashboardModule(
                R.id.nav_cursos,
                R.string.nav_cursos,
                R.string.module_cursos_description,
                R.drawable.ic_course,
                R.color.dashboard_course));
        modules.add(new DashboardModule(
                R.id.nav_estudiantes,
                R.string.nav_estudiantes,
                R.string.module_estudiantes_description,
                R.drawable.ic_student,
                R.color.dashboard_student));
        modules.add(new DashboardModule(
                R.id.nav_docentes,
                R.string.nav_docentes,
                R.string.module_docentes_description,
                R.drawable.ic_teacher,
                R.color.dashboard_teacher));
        modules.add(new DashboardModule(
                R.id.nav_inscripciones,
                R.string.nav_inscripciones,
                R.string.module_inscripciones_description,
                R.drawable.ic_enrollment,
                R.color.dashboard_enrollment));
        modules.add(new DashboardModule(
                R.id.nav_notas,
                R.string.nav_notas,
                R.string.module_notas_description,
                R.drawable.ic_grade,
                R.color.dashboard_grade));
        modules.add(new DashboardModule(
                R.id.nav_colegiaturas,
                R.string.nav_colegiaturas,
                R.string.module_colegiaturas_description,
                R.drawable.ic_payment,
                R.color.dashboard_payment));
        modules.add(new DashboardModule(
                R.id.nav_usuarios,
                R.string.nav_usuarios,
                R.string.module_usuarios_description,
                R.drawable.ic_users,
                R.color.dashboard_users));
        return modules;
    }

    private int blendWithWhite(int color, float accentRatio) {
        int red = (int) (Color.red(color) * accentRatio + 255 * (1f - accentRatio));
        int green = (int) (Color.green(color) * accentRatio + 255 * (1f - accentRatio));
        int blue = (int) (Color.blue(color) * accentRatio + 255 * (1f - accentRatio));
        return Color.rgb(red, green, blue);
    }

    private String getDisplayName() {
        String fullName = sessionManager.getNombreCompleto().trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        return sessionManager.getUsername().trim();
    }

    private String getInitial(String displayName) {
        String source = displayName == null || displayName.trim().isEmpty()
                ? sessionManager.getUsername().trim()
                : displayName.trim();
        return source.isEmpty() ? "S" : source.substring(0, 1).toUpperCase();
    }

    private String formatRoles() {
        Set<String> roles = sessionManager.getRoles();
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return String.join(", ", roles);
    }

    private static class DashboardModule {
        private final int menuItemId;
        private final int titleRes;
        private final int descriptionRes;
        private final int iconRes;
        private final int accentColorRes;

        private DashboardModule(
                int menuItemId,
                int titleRes,
                int descriptionRes,
                int iconRes,
                int accentColorRes) {
            this.menuItemId = menuItemId;
            this.titleRes = titleRes;
            this.descriptionRes = descriptionRes;
            this.iconRes = iconRes;
            this.accentColorRes = accentColorRes;
        }
    }
}
