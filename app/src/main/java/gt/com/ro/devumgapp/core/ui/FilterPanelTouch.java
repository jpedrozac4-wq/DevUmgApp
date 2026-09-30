package gt.com.ro.devumgapp.core.ui;

import android.view.View;
import android.view.ViewParent;

import com.google.android.material.card.MaterialCardView;

/** Makes the filter header and the unused surface around it one reliable touch target. */
public final class FilterPanelTouch {
    private FilterPanelTouch() { }

    public static void bind(View header, View controls, Runnable toggle) {
        if (header == null || controls == null || toggle == null) return;
        header.setMinimumHeight(Math.round(56 * header.getResources().getDisplayMetrics().density));
        header.setClickable(true);
        header.setFocusable(true);
        header.setContentDescription(controls.getVisibility() == View.VISIBLE ? "Ocultar filtros" : "Mostrar filtros");
        Runnable safeToggle = () -> {
            boolean wasVisible = controls.getVisibility() == View.VISIBLE;
            controls.animate().cancel();
            header.setContentDescription(wasVisible ? "Mostrar filtros" : "Ocultar filtros");
            toggle.run();
        };
        header.setOnClickListener(v -> safeToggle.run());

        ViewParent parent = header.getParent();
        while (parent != null && !(parent instanceof MaterialCardView)) parent = parent.getParent();
        if (parent instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) parent;
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> safeToggle.run());
        }
    }
}
