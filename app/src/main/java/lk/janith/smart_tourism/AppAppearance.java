package lk.janith.smart_tourism;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

/** Device-wide UI preference, kept separate from each tourist's Firestore account. */
public final class AppAppearance {
    private static final String SETTINGS = "app_appearance";
    private static final String THEME_MODE = "theme_mode";

    private AppAppearance() {
    }

    public static int getThemeMode(Context context) {
        int value = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
                .getInt(THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (value == AppCompatDelegate.MODE_NIGHT_NO
                || value == AppCompatDelegate.MODE_NIGHT_YES) return value;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(getThemeMode(context));
    }

    public static void setThemeMode(Context context, int mode) {
        if (mode != AppCompatDelegate.MODE_NIGHT_NO
                && mode != AppCompatDelegate.MODE_NIGHT_YES
                && mode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) return;
        context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
                .edit().putInt(THEME_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
