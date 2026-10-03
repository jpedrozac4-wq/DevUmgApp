package gt.com.ro.devumgapp.auth.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import gt.com.ro.devumgapp.R;
import gt.com.ro.devumgapp.colegiatura.ui.ColegiaturaListActivity;
import gt.com.ro.devumgapp.estudiante.ui.EstudianteListActivity;
import gt.com.ro.devumgapp.carrera.ui.CarreraListActivity;
import gt.com.ro.devumgapp.core.session.SessionManager;
import gt.com.ro.devumgapp.core.session.Permissions;
import gt.com.ro.devumgapp.auth.dto.LoginResponse;
import gt.com.ro.devumgapp.auth.network.AuthApiService;
import gt.com.ro.devumgapp.core.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import gt.com.ro.devumgapp.curso.ui.CursoListActivity;
import gt.com.ro.devumgapp.docente.ui.DocenteListActivity;
import gt.com.ro.devumgapp.inscripcion.ui.InscripcionListActivity;
import gt.com.ro.devumgapp.nota.ui.NotaListActivity;
import gt.com.ro.devumgapp.permiso.ui.PermisoListActivity;
import gt.com.ro.devumgapp.rol.ui.RolListActivity;
import gt.com.ro.devumgapp.usuario.ui.UsuarioListActivity;
import gt.com.ro.devumgapp.academico.ui.DocenteAcademicActivity;
import gt.com.ro.devumgapp.academico.ui.EstudianteAcademicActivity;
import gt.com.ro.devumgapp.auditoria.ui.AuditoriaActivity;
import gt.com.ro.devumgapp.notificacion.NotificationBadge;
import gt.com.ro.devumgapp.notificacion.NotificationPermission;
import gt.com.ro.devumgapp.notificacion.PushRegistrationManager;
import gt.com.ro.devumgapp.notificacion.push.SgauFirebaseMessagingService;

public class HomeActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_DRAWER = "open_drawer";
    public static final String EXTRA_CURRENT_DESTINATION = "current_destination";

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private GridLayout modulesGrid;
    private View dashboardHeader;
    private View txtModulesTitle;
    private TextView txtGreeting;
    private TextView txtWelcome;
    private TextView txtUserIdentity;
    private TextView txtAdminAccessSummary;
    private TextView txtToolbarTitle;
    private TextView txtToolbarAvatar;
    private View contextSummaryCard;
    private View contextActions;
    private TextView txtContextTitle;
    private TextView txtContextDescription;
    private MaterialButton btnContextCourses;
    private MaterialButton btnContextGrades;
    private SessionManager sessionManager;
    private boolean syncingBottomNavigation;
    private boolean profileRefreshNeeded;
    private Call<LoginResponse> profileCall;
    private boolean drawerOnlyMode;
    private boolean drawerDestinationSelected;
    private boolean notificationReceiverRegistered;
    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (toolbar != null) NotificationBadge.refresh(toolbar);
        }
    };

    @Override protected void onStart() {
        super.onStart();
        if (notificationReceiverRegistered) return;
        IntentFilter filter = new IntentFilter(SgauFirebaseMessagingService.ACTION_NOTIFICATION_RECEIVED);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(notificationReceiver, filter);
        }
        notificationReceiverRegistered = true;
    }

    @Override protected void onStop() {
        if (notificationReceiverRegistered) {
            unregisterReceiver(notificationReceiver);
            notificationReceiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (modulesGrid != null) {
            applyMenuPermissions();
            if (drawerOnlyMode) {
                navView.setCheckedItem(getIntent().getIntExtra(
                        EXTRA_CURRENT_DESTINATION, R.id.nav_inicio));
                return;
            }
            renderContextualDashboard();
            renderModules();
            navView.setCheckedItem(R.id.nav_inicio);
            syncingBottomNavigation = true;
            try {
                bottomNavigation.setSelectedItemId(R.id.nav_inicio);
            } finally {
                syncingBottomNavigation = false;
            }
            setDashboardTitle(R.string.dashboard_title);
            NotificationBadge.refresh(toolbar);
            if (profileRefreshNeeded && profileCall == null) refreshProfileThenInitialize();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drawerOnlyMode = getIntent().getBooleanExtra(EXTRA_OPEN_DRAWER, false);
        sessionManager = SessionManager.getInstance();

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // The drawer launched from a module is only a lightweight overlay. It
        // must not wait for a profile refresh or briefly render the dashboard.
        if (drawerOnlyMode) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            initializeHomeUi();
            refreshDrawerEmailIfMissing();
            return;
        }

        if (!getIntent().getBooleanExtra("profileVerified", false) || savedInstanceState != null) {
            refreshProfileThenInitialize();
            return;
        }
        initializeHomeUi();
    }

    private void initializeHomeUi() {

        setContentView(R.layout.activity_home);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        bindViews();
        setupToolbar();
        NotificationBadge.attach(this, toolbar);
        PushRegistrationManager.register(this);
        if (!drawerOnlyMode) NotificationPermission.requestIfNeeded(this);
        setupDrawer();
        setupDrawerOnlyMode();
        openDrawerIfRequested();
        applySystemInsets();
        setupBottomNavigation();
        setupBackHandling();
        renderUserSummary();
        renderContextualDashboard();
        renderModules();
        animateDashboardIntro();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openDrawerIfRequested();
    }

    private void openDrawerIfRequested() {
        if (drawerLayout != null && getIntent().getBooleanExtra(EXTRA_OPEN_DRAWER, false)) {
            getIntent().removeExtra(EXTRA_OPEN_DRAWER);
            drawerLayout.post(() -> drawerLayout.openDrawer(GravityCompat.START));
        }
    }

    /**
     * Module screens launch Home only as a transparent drawer host. Keeping the
     * module Activity underneath prevents opening or dismissing the menu from
     * unexpectedly returning the user to the dashboard.
     */
    private void setupDrawerOnlyMode() {
        if (!drawerOnlyMode) return;

        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        drawerLayout.setBackgroundColor(Color.TRANSPARENT);
        View dashboardContent = drawerLayout.getChildAt(0);
        dashboardContent.setVisibility(View.INVISIBLE);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerClosed(View drawerView) {
                if (!drawerDestinationSelected && !isFinishing()) {
                    finish();
                    overridePendingTransition(0, 0);
                }
            }
        });
    }

    private void refreshProfileThenInitialize() {
        AuthApiService auth = RetrofitClient.getClient().create(AuthApiService.class);
        profileCall = auth.me();
        profileCall.enqueue(new Callback<LoginResponse>() {
            @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.refreshProfile(response.body());
                    getIntent().putExtra("profileVerified", true);
                    initializeHomeUi();
                } else if (response.code() == 401) {
                    SessionManager.handleUnauthorized(HomeActivity.this);
                } else {
                    profileRefreshNeeded = true;
                    Toast.makeText(HomeActivity.this,
                            "No se pudo actualizar el perfil. Se muestra la sesion guardada.", Toast.LENGTH_LONG).show();
                    initializeHomeUi();
                }
            }
            @Override public void onFailure(Call<LoginResponse> call, Throwable error) {
                if (call.isCanceled() || isFinishing()) return;
                profileRefreshNeeded = true;
                Toast.makeText(HomeActivity.this,
                        "No se pudo actualizar el perfil. Se muestra la sesion guardada.", Toast.LENGTH_LONG).show();
                initializeHomeUi();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (profileCall != null) profileCall.cancel();
        super.onDestroy();
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
        txtWelcome = findViewById(R.id.txtWelcome);
        txtUserIdentity = findViewById(R.id.txtUserIdentity);
        txtAdminAccessSummary = findViewById(R.id.txtAdminAccessSummary);
        txtToolbarTitle = findViewById(R.id.txtToolbarTitle);
        txtToolbarAvatar = findViewById(R.id.txtToolbarAvatar);
        contextSummaryCard = findViewById(R.id.contextSummaryCard);
        contextActions = findViewById(R.id.contextActions);
        txtContextTitle = findViewById(R.id.txtContextTitle);
        txtContextDescription = findViewById(R.id.txtContextDescription);
        btnContextCourses = findViewById(R.id.btnContextCourses);
        btnContextGrades = findViewById(R.id.btnContextGrades);
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
        txtToolbarAvatar.setOnClickListener(view -> showProfile());
    }

    private void setupDrawer() {
        renderDrawerHeader();
        applyMenuPermissions();
        navView.setCheckedItem(drawerOnlyMode
                ? getIntent().getIntExtra(EXTRA_CURRENT_DESTINATION, R.id.nav_inicio)
                : R.id.nav_inicio);
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
        String username = sessionManager.getUsername().trim();
        tvNavNombre.setText(username.isEmpty()
                ? getString(R.string.dashboard_user_fallback)
                : username);

        String email = sessionManager.getEmail().trim();
        if (!email.isEmpty()) {
            tvNavRoles.setText(email);
        } else {
            Set<String> roles = sessionManager.getRoles();
            tvNavRoles.setText(roles.isEmpty() ? username : String.join(" · ", roles));
        }
    }

    private void refreshDrawerEmailIfMissing() {
        if (!sessionManager.getEmail().trim().isEmpty() || profileCall != null) return;

        profileCall = RetrofitClient.getClient().create(AuthApiService.class).me();
        profileCall.enqueue(new Callback<LoginResponse>() {
            @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                profileCall = null;
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.refreshProfile(response.body());
                    renderDrawerHeader();
                } else if (response.code() == 401) {
                    SessionManager.handleUnauthorized(HomeActivity.this);
                }
            }

            @Override public void onFailure(Call<LoginResponse> call, Throwable error) {
                profileCall = null;
                // The drawer remains usable with the locally stored identity.
            }
        });
    }

    private void renderUserSummary() {
        String displayName = getDisplayName();
        txtToolbarAvatar.setText(getInitial(displayName));
        txtGreeting.setText(getTimeGreeting());
        txtWelcome.setText(displayName.isEmpty()
                ? getString(R.string.dashboard_user_fallback)
                : displayName);

        String username = sessionManager.getUsername().trim();
        String email = sessionManager.getEmail().trim();
        List<String> identityRows = new ArrayList<>();
        if (!email.isEmpty()) {
            identityRows.add(email);
        }
        if (!username.isEmpty()) {
            identityRows.add(username);
        }
        txtUserIdentity.setText(String.join("\n", identityRows));
        txtUserIdentity.setVisibility(identityRows.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String getTimeGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return getString(R.string.dashboard_greeting_morning);
        if (hour >= 12 && hour < 19) return getString(R.string.dashboard_greeting_afternoon);
        return getString(R.string.dashboard_greeting_evening);
    }

    private void renderContextualDashboard() {
        Set<String> roles = sessionManager.getRoles();
        boolean admin = hasRole(roles, "ADMIN");
        boolean student = hasRole(roles, "ESTUDIANTE");
        boolean teacher = hasRole(roles, "DOCENTE");

        btnContextCourses.setOnClickListener(view -> openModule(R.id.nav_cursos));
        btnContextGrades.setOnClickListener(view -> openModule(R.id.nav_notas));
        btnContextCourses.setVisibility(Permissions.canOpenDestination(R.id.nav_cursos) ? View.VISIBLE : View.GONE);
        btnContextGrades.setVisibility(Permissions.canOpenDestination(R.id.nav_notas) ? View.VISIBLE : View.GONE);
        contextActions.setVisibility(
                btnContextCourses.getVisibility() == View.VISIBLE
                        || btnContextGrades.getVisibility() == View.VISIBLE
                        ? View.VISIBLE : View.GONE);

        if (admin || (!student && !teacher)) {
            int availableModules = countAvailableModules();
            contextSummaryCard.setVisibility(View.GONE);
            txtAdminAccessSummary.setText(
                    getString(R.string.dashboard_admin_access_summary, availableModules));
            txtAdminAccessSummary.setVisibility(View.VISIBLE);
            return;
        }

        contextSummaryCard.setVisibility(View.VISIBLE);
        txtAdminAccessSummary.setVisibility(View.GONE);
        if (student) {
            txtContextTitle.setText(R.string.dashboard_student_summary_title);
            txtContextDescription.setText(R.string.dashboard_student_summary_unavailable);
        } else {
            txtContextTitle.setText(R.string.dashboard_teacher_summary_title);
            txtContextDescription.setText(R.string.dashboard_teacher_summary);
        }
    }

    private boolean hasRole(Set<String> roles, String expected) {
        if (roles == null) return false;
        for (String role : roles) {
            if (role != null && (role.equalsIgnoreCase(expected)
                    || role.equalsIgnoreCase("ROLE_" + expected))) return true;
        }
        return false;
    }

    private int countAvailableModules() {
        int count = 0;
        for (DashboardModule module : createModules()) {
            if (Permissions.canOpenDestination(module.menuItemId)) count++;
        }
        return count;
    }

    private void renderModules() {
        modulesGrid.removeAllViews();
        List<DashboardModule> modules = new ArrayList<>();
        for (DashboardModule module : createModules()) {
            if (Permissions.canOpenDestination(module.menuItemId)) modules.add(module);
        }
        if (modules.isEmpty()) {
            ((TextView) txtModulesTitle).setText("No tienes módulos habilitados para esta cuenta.");
            return;
        }
        ((TextView) txtModulesTitle).setText(R.string.dashboard_access_title);

        for (int index = 0; index < modules.size(); index++) {
            DashboardModule module = modules.get(index);
            MaterialCardView card = (MaterialCardView) getLayoutInflater()
                    .inflate(R.layout.item_dashboard_module, modulesGrid, false);

            ImageView icon = card.findViewById(R.id.imgModuleIcon);
            TextView name = card.findViewById(R.id.txtModuleName);
            TextView description = card.findViewById(R.id.txtModuleDescription);
            View iconContainer = card.findViewById(R.id.iconContainer);

            icon.setImageResource(module.iconRes);
            icon.setVisibility(View.VISIBLE);
            icon.setAlpha(1f);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (Permissions.hasRole("ESTUDIANTE") && !Permissions.hasRole("ADMIN")) {
                name.setText(module.menuItemId == R.id.nav_inscripciones ? "Mi carrera"
                        : module.menuItemId == R.id.nav_cursos ? "Mis cursos"
                        : module.menuItemId == R.id.nav_notas ? "Mis notas"
                        : module.menuItemId == R.id.nav_colegiaturas ? "Colegiatura"
                        : module.menuItemId == R.id.nav_profile ? "Mi perfil" : getString(module.titleRes));
                description.setText(module.menuItemId == R.id.nav_profile ? "Datos de mi cuenta"
                        : module.menuItemId == R.id.nav_inscripciones ? "Plan y avance de créditos"
                        : getString(module.descriptionRes));
            } else { name.setText(module.titleRes); description.setText(module.descriptionRes); }
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
        int iconBackgroundColor = blendWithBlack(accentColor, 0.72f);

        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.dashboard_border));
        icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setColor(iconBackgroundColor);
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
            drawerDestinationSelected = true;
            drawerLayout.closeDrawer(GravityCompat.START);
            showLogoutConfirmation();
            return;
        }

        navView.setCheckedItem(itemId);
        syncBottomSelection(itemId);
        if (drawerOnlyMode) drawerDestinationSelected = true;
        drawerLayout.closeDrawer(GravityCompat.START);
        setDashboardTitle(itemId == R.id.nav_inicio
                ? R.string.dashboard_title
                : moduleTitleFor(itemId));

        if (itemId == R.id.nav_inicio) {
            if (drawerOnlyMode) {
                drawerDestinationSelected = true;
                Intent intent = new Intent(this, HomeActivity.class);
                // This Activity instance is only the transparent drawer host.
                // CLEAR_TOP would resolve back to this same instance, so make
                // the real dashboard the new task root instead.
                intent.putExtra("profileVerified", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
            return;
        }

        if (!openModule(itemId)) {
            Toast.makeText(this, R.string.dashboard_module_coming_soon, Toast.LENGTH_SHORT).show();
        }
    }

    private void openModule(DashboardModule module) {
        if (module.menuItemId == R.id.nav_profile) { showProfile(); return; }
        navView.setCheckedItem(module.menuItemId);
        syncBottomSelection(module.menuItemId);
        setDashboardTitle(module.titleRes);
        if (!openModule(module.menuItemId)) {
            Toast.makeText(this, R.string.dashboard_module_coming_soon, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation() {
        rebuildBottomNavigation();
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
                showProfile();
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
        DashboardModule target = findModule(itemId);
        if (target == null || !Permissions.canOpenDestination(itemId)) {
            Toast.makeText(this, "No tienes permiso para consultar este módulo.", Toast.LENGTH_SHORT).show();
            return false;
        }
        Intent intent = moduleIntentFor(itemId);
        if (intent == null) {
            return false;
        }
        if (drawerOnlyMode) drawerDestinationSelected = true;
        startActivity(intent);
        if (drawerOnlyMode) {
            finish();
            overridePendingTransition(0, 0);
        }
        return true;
    }

    private void showProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private DashboardModule findModule(int itemId) {
        for (DashboardModule module : createModules()) if (module.menuItemId == itemId) return module;
        return null;
    }

    private void applyMenuPermissions() {
        for (DashboardModule module : createModules()) {
            MenuItem drawerItem = navView.getMenu().findItem(module.menuItemId);
            if (drawerItem != null) drawerItem.setVisible(Permissions.canOpenDestination(module.menuItemId));
        }
        rebuildBottomNavigation();
    }

    /** Re-inflation makes permission gains and removals deterministic and leaves no empty slots. */
    private void rebuildBottomNavigation() {
        if (bottomNavigation == null) return;
        int selectedItem = bottomNavigation.getSelectedItemId();
        syncingBottomNavigation = true;
        try {
            bottomNavigation.getMenu().clear();
            bottomNavigation.inflateMenu(R.menu.menu_bottom_navigation);
            List<Integer> candidates = new ArrayList<>();
            candidates.add(R.id.nav_cursos);
            candidates.add(R.id.nav_estudiantes);
            candidates.add(R.id.nav_notas);
            for (int itemId : candidates) {
                if (!Permissions.canOpenDestination(itemId)) {
                    bottomNavigation.getMenu().removeItem(itemId);
                }
            }
            if (bottomNavigation.getMenu().findItem(selectedItem) != null) {
                bottomNavigation.setSelectedItemId(selectedItem);
            } else {
                bottomNavigation.setSelectedItemId(R.id.nav_inicio);
            }
        } finally {
            syncingBottomNavigation = false;
        }
    }

    private Intent moduleIntentFor(int itemId) {
        if (!Permissions.hasRole("ADMIN") && Permissions.hasRole("DOCENTE")
                && (itemId == R.id.nav_cursos || itemId == R.id.nav_notas
                || itemId == R.id.nav_estudiantes || itemId == R.id.nav_inscripciones)) {
            return new Intent(this, DocenteAcademicActivity.class)
                    .putExtra(EXTRA_CURRENT_DESTINATION, itemId);
        }
        if (!Permissions.hasRole("ADMIN") && Permissions.hasRole("ESTUDIANTE")
                && (itemId == R.id.nav_carreras || itemId == R.id.nav_cursos || itemId == R.id.nav_inscripciones
                || itemId == R.id.nav_notas || itemId == R.id.nav_colegiaturas)) {
            return new Intent(this, EstudianteAcademicActivity.class)
                    .putExtra(EXTRA_CURRENT_DESTINATION, itemId);
        }
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
        if (itemId == R.id.nav_roles) {
            return new Intent(this, RolListActivity.class);
        }
        if (itemId == R.id.nav_permisos) {
            return new Intent(this, PermisoListActivity.class);
        }
        if (itemId == R.id.nav_auditoria) {
            return new Intent(this, AuditoriaActivity.class);
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
                .setNegativeButton(R.string.dashboard_logout_cancel, (dialog, which) -> {
                    if (drawerOnlyMode) {
                        finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .setOnCancelListener(dialog -> {
                    if (drawerOnlyMode) {
                        finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .setPositiveButton(R.string.dashboard_logout_confirm, (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        PushRegistrationManager.unregister(this, () -> runOnUiThread(() -> {
            sessionManager.logout();
            navigateToLogin();
        }));
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
                R.color.dashboard_career, Permissions.CARRERAS_LEER));
        modules.add(new DashboardModule(
                R.id.nav_cursos,
                R.string.nav_cursos,
                R.string.module_cursos_description,
                R.drawable.ic_course,
                R.color.dashboard_course, Permissions.CURSOS_LEER));
        modules.add(new DashboardModule(
                R.id.nav_estudiantes,
                R.string.nav_estudiantes,
                R.string.module_estudiantes_description,
                R.drawable.ic_student,
                R.color.dashboard_student, Permissions.ESTUDIANTES_LEER));
        modules.add(new DashboardModule(
                R.id.nav_docentes,
                R.string.nav_docentes,
                R.string.module_docentes_description,
                R.drawable.ic_teacher,
                R.color.dashboard_teacher, Permissions.DOCENTES_LEER));
        modules.add(new DashboardModule(
                R.id.nav_inscripciones,
                R.string.nav_inscripciones,
                R.string.module_inscripciones_description,
                R.drawable.ic_enrollment,
                R.color.dashboard_enrollment, Permissions.INSCRIPCIONES_LEER));
        modules.add(new DashboardModule(
                R.id.nav_notas,
                R.string.nav_notas,
                R.string.module_notas_description,
                R.drawable.ic_grade,
                R.color.dashboard_grade, Permissions.NOTAS_LEER));
        modules.add(new DashboardModule(
                R.id.nav_colegiaturas,
                R.string.nav_colegiaturas,
                R.string.module_colegiaturas_description,
                R.drawable.ic_payment,
                R.color.dashboard_payment, Permissions.COLEGIATURAS_LEER));
        modules.add(new DashboardModule(
                R.id.nav_usuarios,
                R.string.nav_usuarios,
                R.string.module_usuarios_description,
                R.drawable.ic_users,
                R.color.dashboard_users, Permissions.USUARIOS_LEER));
        modules.add(new DashboardModule(
                R.id.nav_roles,
                R.string.nav_roles,
                R.string.module_roles_description,
                R.drawable.ic_badge,
                R.color.dashboard_rol, Permissions.ROLES_LEER));
        modules.add(new DashboardModule(
                R.id.nav_permisos,
                R.string.nav_permisos,
                R.string.module_permisos_description,
                R.drawable.ic_lock,
                R.color.dashboard_permiso, Permissions.PERMISOS_LEER));
        modules.add(new DashboardModule(
                R.id.nav_auditoria,
                R.string.nav_auditoria,
                R.string.module_auditoria_description,
                R.drawable.ic_description,
                R.color.dashboard_permiso, Permissions.AUDITORIA_LEER));
        if (Permissions.hasRole("ESTUDIANTE") && !Permissions.hasRole("ADMIN")) {
            modules.add(new DashboardModule(R.id.nav_profile, R.string.bottom_profile,
                    R.string.dashboard_profile_description, R.drawable.ic_user,
                    R.color.dashboard_student, null));
        }
        return modules;
    }

    private int blendWithBlack(int color, float accentRatio) {
        int red = (int) (Color.red(color) * accentRatio);
        int green = (int) (Color.green(color) * accentRatio);
        int blue = (int) (Color.blue(color) * accentRatio);
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
        private final String permission;

        private DashboardModule(
                int menuItemId,
                int titleRes,
                int descriptionRes,
                int iconRes,
                int accentColorRes,
                String permission) {
            this.menuItemId = menuItemId;
            this.titleRes = titleRes;
            this.descriptionRes = descriptionRes;
            this.iconRes = iconRes;
            this.accentColorRes = accentColorRes;
            this.permission = permission;
        }
    }
}
