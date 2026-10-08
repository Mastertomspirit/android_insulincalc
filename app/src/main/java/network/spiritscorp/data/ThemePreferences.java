package network.spiritscorp.data;

/*
 * Copyright (C) 2026 Tom Spirit
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import network.spiritscorp.ui.theme.AppTheme;

/**
 * Synchronous theme preferences cache in Java to prevent theme flashing/flickering on app startup
 * while Room Database asynchronous Flow is initializing.
 */
public class ThemePreferences {

    private static final String PREFS_NAME = "insulin_calc_theme_prefs";
    private static final String KEY_SELECTED_THEME = "selected_theme";
    private static final String KEY_THEME_MODE = "theme_mode";
    private final SharedPreferences mPrefs;

    /**
     * Constructs a new ThemePreferences instance bound to the application context.
     *
     * @param context Android context.
     */
    public ThemePreferences(@NonNull Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    /**
     * Constructs a new ThemePreferences instance with custom SharedPreferences for DI and testing.
     *
     * @param prefs SharedPreferences instance.
     */
    public ThemePreferences(@NonNull SharedPreferences prefs) {
        this.mPrefs = prefs;
    }

    /**
     * Reads the cached color theme identifier synchronously.
     *
     * @return Stored theme enum name or AppTheme.MEDICAL_TEAL default.
     */
    public AppTheme getSelectedTheme() {
        String themeName = mPrefs.getString(KEY_SELECTED_THEME, AppTheme.MEDICAL_TEAL.name());
        return AppTheme.valueOf(themeName);
    }

    /**
     * Reads the cached theme mode (LIGHT, DARK, or SYSTEM) synchronously.
     *
     * @return Stored mode enum name or AppTheme.Mode.SYSTEM default.
     */
    public AppTheme.Mode getThemeMode() {
        String modeName = mPrefs.getString(KEY_THEME_MODE, AppTheme.Mode.SYSTEM.name());
        return AppTheme.Mode.valueOf(modeName);
    }

    /**
     * Persists the active theme preferences synchronously to SharedPreferences.
     *
     * @param selectedTheme Selected color theme identifier.
     * @param themeMode     Selected mode (LIGHT, DARK, or SYSTEM).
     */
    public void savePreferences(@NonNull AppTheme selectedTheme, @NonNull AppTheme.Mode themeMode) {
        mPrefs.edit()
                .putString(KEY_SELECTED_THEME, selectedTheme.name())
                .putString(KEY_THEME_MODE, themeMode.name())
                .apply();
    }
}
