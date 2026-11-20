package app.windowshopper.extension.meesho;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.ComponentName;
import android.content.ActivityNotFoundException;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SettingsUi {
    static final int ACCENT = Color.rgb(159, 37, 112);
    private static final int INK = 0xFF1A1C1E;
    private static final int MUTED = 0xFF43474E;
    private static final int BACKGROUND = 0xFFFDFCFF;
    private static final int PRIMARY = 0xFF005FAC;
    private static final String[] CATEGORIES = {"Ads", "Ratings", "Layout", "Privacy"};

    private SettingsUi() {}

    static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    static Drawable background(int color, int radius, Activity activity) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(activity, radius));
        return shape;
    }

    static Drawable buttonBackground(Activity activity, boolean filled) {
        return new RippleDrawable(ColorStateList.valueOf(
            filled ? 0x33FFFFFF : 0x189F2570),
            background(filled ? ACCENT : Color.WHITE, 28, activity), null);
    }

    public static void show(Activity activity) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Material_Light_NoActionBar);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });

        Toolbar toolbar = new Toolbar(activity);
        toolbar.setTitle("Morphe");
        toolbar.setTitleTextColor(INK);
        toolbar.setNavigationIcon(new BackIcon(activity));
        toolbar.setNavigationContentDescription("Back");
        toolbar.setNavigationOnClickListener(view -> dialog.dismiss());
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(activity, 56)));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout preferences = new LinearLayout(activity);
        preferences.setOrientation(LinearLayout.VERTICAL);
        preferences.setPadding(0, 0, 0, dp(activity, 16));
        scroll.addView(preferences);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        for (int category = 0; category < CATEGORIES.length; category++) {
            TextView heading = text(activity, CATEGORIES[category], 14, PRIMARY, true);
            heading.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 8));
            heading.setAccessibilityHeading(true);
            preferences.addView(heading);
            for (Settings.Feature feature : Settings.Feature.values()) {
                if (feature.category == category) preferences.addView(featureRow(activity, feature));
            }
            if (category == 2) preferences.addView(tabsRow(activity));
        }

        LinearLayout apply = new LinearLayout(activity);
        apply.setOrientation(LinearLayout.VERTICAL);
        apply.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 20));
        apply.setMinimumHeight(dp(activity, 64));
        apply.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1F005FAC),
            null, new ColorDrawable(Color.WHITE)));
        apply.addView(text(activity, "Apply & reopen Meesho", 16, PRIMARY, false));
        TextView applySummary = text(activity, "Rebuild navigation and profile. Your sign-in is kept.",
            14, MUTED, false);
        applySummary.setPadding(0, dp(activity, 4), 0, 0);
        apply.addView(applySummary);
        preferences.addView(apply, new LinearLayout.LayoutParams(-1, -2));
        TextView note = text(activity,
            "Settings are saved automatically. Reload results after changing filters; "
            + "cached content may need a fresh response.", 13, MUTED, false);
        note.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 16));
        preferences.addView(note);
        apply.setOnClickListener(view -> {
            try {
                activity.startActivity(Intent.makeRestartActivityTask(new ComponentName(
                    activity, "com.meesho.supply.main.MainActivity")));
                dialog.dismiss();
            } catch (ActivityNotFoundException error) {
                Log.e("WindowShopper", "Meesho main entry unavailable", error);
                Toast.makeText(activity, "Could not reopen Meesho; reopen it manually", Toast.LENGTH_LONG).show();
            }
        });
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window == null) throw new IllegalStateException("Morphe settings window unavailable");
        window.setStatusBarColor(BACKGROUND);
        window.setNavigationBarColor(BACKGROUND);
        window.getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        dialog.show();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.requestApplyInsets();
    }

    private static View featureRow(Activity activity, Settings.Feature feature) {
        boolean available = feature.available();
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(activity, 72));
        row.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1F005FAC),
            null, new ColorDrawable(Color.WHITE)));
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(0, 0, dp(activity, 12), 0);
        labels.addView(text(activity, feature.title, 16, INK, false));
        TextView detail = text(activity, available ? feature.summary
            : "Not included in this APK. Select its patch before building.", 14, MUTED, false);
        detail.setPadding(0, dp(activity, 4), 0, 0);
        labels.addView(detail);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        Switch toggle = new Switch(activity);
        toggle.setMinimumHeight(dp(activity, 48));
        toggle.setContentDescription(feature.title);
        toggle.setTag("morphe-" + feature.name());
        toggle.setChecked(Settings.enabled(feature));
        toggle.setEnabled(available);
        int[][] states = {{android.R.attr.state_checked}, {}};
        toggle.setThumbTintList(new ColorStateList(states, new int[] {PRIMARY, 0xFF73777F}));
        toggle.setTrackTintList(new ColorStateList(states, new int[] {0xFFD4E3FF, 0xFFDFE2EB}));
        toggle.setOnCheckedChangeListener((view, checked) -> {
            if (!Settings.save(feature, checked)) {
                Toast.makeText(activity, "Could not save setting; it may reset after restart",
                    Toast.LENGTH_LONG).show();
            }
        });
        row.addView(toggle);
        row.setOnClickListener(view -> { if (available) toggle.setChecked(!toggle.isChecked()); });
        if (!available) row.setAlpha(0.55f);
        return row;
    }

    private static View tabsRow(Activity activity) {
        boolean available = Unclutter.hideNavTabs();
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setMinimumHeight(dp(activity, 72));
        row.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1F005FAC),
            null, new ColorDrawable(Color.WHITE)));
        row.addView(text(activity, "Bottom tabs", 16, INK, false));
        TextView detail = text(activity, "", 14, MUTED, false);
        detail.setPadding(0, dp(activity, 4), 0, 0);
        row.addView(detail);
        Runnable refresh = () -> {
            List<String> names = new ArrayList<>();
            java.util.Map<String, String> known = Settings.knownTabs();
            for (String tab : Settings.tabs()) if (known.containsKey(tab)) names.add(known.get(tab));
            detail.setText(!available ? "Not included in this APK. Select its patch before building."
                : names.isEmpty() ? "Meesho default. Tap to choose and reorder up to 5 tabs."
                : String.join(" · ", names));
        };
        refresh.run();
        row.setOnClickListener(view -> { if (available) tabsDialog(activity, refresh); });
        if (!available) row.setAlpha(0.55f);
        return row;
    }

    private static void tabsDialog(Activity activity, Runnable done) {
        java.util.Map<String, String> known = Settings.knownTabs();
        if (known.isEmpty()) {
            Toast.makeText(activity, "Open the home screen once, then try again", Toast.LENGTH_LONG).show();
            return;
        }
        List<String> order = new ArrayList<>();
        Set<String> picked = new HashSet<>();
        for (String tab : Settings.tabs()) if (known.containsKey(tab) && picked.add(tab)) order.add(tab);
        if (picked.isEmpty()) {
            for (String tab : known.keySet()) if (!tab.equals("action_account") && picked.size() < NavTabs.MAX) picked.add(tab);
        }
        for (String tab : known.keySet()) if (!order.contains(tab)) order.add(tab);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), 0);
        for (String tab : order) {
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setTag(tab);
            CheckBox box = new CheckBox(activity);
            box.setText(known.get(tab));
            box.setTextSize(16);
            box.setChecked(picked.contains(tab));
            box.setOnCheckedChangeListener((view, checked) -> {
                if (checked && picked.size() >= NavTabs.MAX) {
                    view.setChecked(false);
                    Toast.makeText(activity, "Up to 5 tabs", Toast.LENGTH_SHORT).show();
                } else if (checked) picked.add(tab);
                else picked.remove(tab);
            });
            row.addView(box, new LinearLayout.LayoutParams(0, dp(activity, 48), 1));
            TextView handle = text(activity, "≡", 24, MUTED, false);
            handle.setGravity(Gravity.CENTER);
            handle.setContentDescription("Reorder " + known.get(tab));
            handle.setOnTouchListener((view, event) -> {
                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        list.requestDisallowInterceptTouchEvent(true);
                        row.setAlpha(0.6f);
                        return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        float y = row.getTop() + row.getHeight() / 2f + event.getY() - view.getHeight() / 2f;
                        for (int i = 0; i < list.getChildCount(); i++) {
                            View other = list.getChildAt(i);
                            if (other != row && y > other.getTop() && y < other.getBottom()) {
                                // Move the neighbour, not the touched row: removing it would cancel this drag.
                                int from = list.indexOfChild(row);
                                list.removeView(other);
                                list.addView(other, from);
                                break;
                            }
                        }
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        row.setAlpha(1f);
                        return true;
                    default:
                        return false;
                }
            });
            // Screen-reader alternative to dragging.
            handle.setAccessibilityDelegate(new View.AccessibilityDelegate() {
                public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(1, "Move up"));
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(2, "Move down"));
                }
                public boolean performAccessibilityAction(View host, int action, android.os.Bundle args) {
                    if (action != 1 && action != 2) return super.performAccessibilityAction(host, action, args);
                    int to = list.indexOfChild(row) + (action == 1 ? -1 : 1);
                    if (to < 0 || to >= list.getChildCount()) return false;
                    list.removeView(row);
                    list.addView(row, to);
                    return true;
                }
            });
            row.addView(handle, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
            list.addView(row);
        }
        new AlertDialog.Builder(activity)
            .setTitle("Bottom tabs")
            .setMessage("Choose up to 5 tabs. Drag ≡ to reorder, top is leftmost.")
            .setView(list)
            .setPositiveButton("Save", (dialog, which) -> {
                List<String> tabs = new ArrayList<>();
                for (int i = 0; i < list.getChildCount(); i++) {
                    String tab = (String) list.getChildAt(i).getTag();
                    if (picked.contains(tab)) tabs.add(tab);
                }
                if (tabs.isEmpty() || !Settings.saveTabs(tabs)) {
                    Toast.makeText(activity, "Select at least one tab", Toast.LENGTH_LONG).show();
                }
                done.run();
            })
            .setNeutralButton("Default", (dialog, which) -> { Settings.saveTabs(new ArrayList<>()); done.run(); })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private static TextView text(Activity activity, String value, int size, int color, boolean bold) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return text;
    }

    private static final class BackIcon extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int size;

        BackIcon(Activity activity) {
            size = dp(activity, 24);
            paint.setColor(INK);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        public int getIntrinsicWidth() { return size; }
        public int getIntrinsicHeight() { return size; }

        public void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(getBounds().left, getBounds().top);
            canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
            canvas.drawLine(4, 12, 20, 12, paint);
            canvas.drawLine(4, 12, 11, 5, paint);
            canvas.drawLine(4, 12, 11, 19, paint);
            canvas.restore();
        }

        public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    static final class SortIcon extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        SortIcon() {
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);
        }

        public void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(getBounds().left, getBounds().top);
            canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
            canvas.drawLine(4, 6, 15, 6, paint);
            canvas.drawLine(4, 12, 12, 12, paint);
            canvas.drawLine(4, 18, 9, 18, paint);
            canvas.drawLine(19, 5, 19, 19, paint);
            canvas.drawLine(16, 16, 19, 19, paint);
            canvas.drawLine(19, 19, 22, 16, paint);
            canvas.restore();
        }

        public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
