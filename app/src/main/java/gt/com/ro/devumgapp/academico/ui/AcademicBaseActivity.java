package gt.com.ro.devumgapp.academico.ui;

import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import gt.com.ro.devumgapp.R;

abstract class AcademicBaseActivity extends AppCompatActivity {
    protected LinearLayout content;
    protected View progress;

    protected TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(this, R.color.dashboard_text_primary));
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(4), 0, dp(4));
        return view;
    }

    protected TextView section(String title) {
        TextView view = text(title, 18, true);
        view.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        view.setPadding(dp(2), dp(22), 0, dp(10));
        return view;
    }

    protected void configureHero(String title, String subtitle, int background, int icon, int tint) {
        findViewById(R.id.academicHero).setBackgroundResource(background);
        ((TextView) findViewById(R.id.txtAcademicHeroTitle)).setText(title);
        ((TextView) findViewById(R.id.txtAcademicHeroSubtitle)).setText(subtitle);
        ImageView image = findViewById(R.id.imgAcademicHero);
        image.setImageResource(icon);
        image.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, tint)));
    }

    protected MaterialCardView card(String title, String detail) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.dashboard_surface));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.dashboard_border));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(8));
        card.setCardElevation(dp(4));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(15), dp(16), dp(15));
        box.addView(text(title, 16, true));
        if (detail != null && !detail.isEmpty()) {
            TextView secondary = text(detail, 14, false);
            secondary.setTextColor(ContextCompat.getColor(this, R.color.dashboard_text_secondary));
            box.addView(secondary);
        }
        card.addView(box);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        return card;
    }

    protected MaterialCardView messageCard(String message, boolean error) {
        MaterialCardView result = card(message, "");
        TextView label = (TextView) ((LinearLayout) result.getChildAt(0)).getChildAt(0);
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        label.setTextColor(ContextCompat.getColor(this,
                error ? R.color.dashboard_error : R.color.dashboard_text_secondary));
        return result;
    }

    protected int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    protected void loading(boolean value) { progress.setVisibility(value ? View.VISIBLE : View.GONE); }
}
